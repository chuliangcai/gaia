package com.gaia.domain.ability.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia.domain.ability.task.config.Task;
import com.gaia.domain.ability.task.config.TaskConfig;
import com.gaia.domain.ability.task.dto.TaskDTO;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务清单 Ability（processor 链式版）。
 *
 * <p>该能力不覆写父类的 {@link #execute(RequestContext)}；执行骨架由
 * {@link AbstractBaseAbility} 默认实现按 {@link #getProcessor(String)} 返回的顺序
 * 反射调用同名 processor 方法（每个处理器名对应一个签名匹配的方法）：</p>
 * <ol>
 *     <li>{@link #preCheck(RequestContext) preCheck} —
 *     解析入参（activityId），按 id 加载 {@link Activity}，并从
 *     {@link Activity#getConfigurations()} 反序列化得到任务列表。</li>
 *     <li>{@link #enrichProgress(RequestContext) enrichProgress} —
 *     对每个任务按当前用户查询累计执行次数，组装 {@link TaskDTO} 输出。</li>
 * </ol>
 *
 * <p>注意：每个 processor 通过 {@link #resolveContext(RequestContext)} 重新构造
 * {@link ExecutionContext}，processor 之间没有"共享同一个 ec 实例"的约定。
 * 任何仅在本地修改 ec 字段的写法都会在下一个 processor 里被丢弃——
 * 这是本框架执行骨架的硬约束。{@link CollectDrawAbility} 采用同样的模式
 * （在 {@code resolveContext} 里把 {@code CollectDrawConfig} 装进 ec）。
 * 因此任务列表必须在 {@link #resolveContext(RequestContext)} 中加载并放进
 * ec，使所有 processor 看到的都是同一份结果。</p>
 *
 * <p>该能力与 {@link TaskExecuteAbility} 平级、互不重叠，二者共享同一份
 * {@code TaskConfig}。该能力不需要扩展点（无玩法侧差异化逻辑）。</p>
 */
@Slf4j
@Component
@Ability(code = "task_list", bizNs = "task", name = "任务清单", configClass = TaskConfig.class)
@RequiredArgsConstructor
public class TaskListAbility extends AbstractBaseAbility {

    /** 处理器名：前置校验（同时把任务列表装入 ec） */
    private static final String PROC_PRE_CHECK = "preCheck";
    /** 处理器名：填充执行进度 */
    private static final String PROC_ENRICH_PROGRESS = "enrichProgress";

    private static final String CONFIG_KEY = TaskConfig.class.getName();

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ObjectMapper objectMapper;

    // ---------------------- processor 声明 ----------------------

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of(PROC_PRE_CHECK, PROC_ENRICH_PROGRESS);
    }

    // ---------------------- processor 实现 ----------------------

    /**
     * 前置校验：解析入参（activityId），按 id 加载 {@link Activity} 并解析任务列表。
     * 校验失败时短路返回对应 msgCode。
     */
    IFResult<List<TaskDTO>> preCheck(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }
        return IFResult.success(Collections.emptyList());
    }

    /**
     * 填充执行进度：按用户累计执行次数填充每个任务的进度字段，
     * 组装为 {@link TaskDTO} 列表。
     */
    IFResult<List<TaskDTO>> enrichProgress(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }
        if (ec.tasks == null || ec.tasks.isEmpty()) {
            return IFResult.success(Collections.emptyList());
        }

        List<TaskDTO> out = new ArrayList<>(ec.tasks.size());
        for (Task t : ec.tasks) {
            long executed = 0;
            if (ec.userId != null && ec.userId > 0) {
                executed = activityRecordRepository.countByUserTask(
                        ec.userId, ec.activity.getId(), t.getId());
            }
            out.add(TaskDTO.builder()
                    .id(t.getId())
                    .taskName(t.getTaskName())
                    .taskDesc(t.getTaskDesc())
                    .rewardCount(t.getRewardCount())
                    .maxExecutions(t.getMaxExecutions())
                    .executedCount((int) executed)
                    .available(t.isAvailableFor((int) executed))
                    .sortOrder(t.getSortOrder())
                    .build());
        }
        return IFResult.success(out);
    }

    // ---------------------- 入参解析 ----------------------

    /**
     * 从 {@link RequestContext} 解析入参：按 activityId 加载 {@link Activity}，
     * 并立即从 {@link Activity#getConfigurations()} 反序列化任务列表。
     *
     * <p>之所以把"加载任务列表"放进 {@code resolveContext} 而不是单独的
     * {@code loadConfig} processor，是因为每个 processor 在自己的入口处都会
     * 重新调用 {@code resolveContext} 构造 ec，前一站修改的 ec 字段会在下一站
     * 被丢弃——把任务列表直接装进 ec 是唯一能在两个 processor 之间传递状态的
     * 方式。详见类注释。</p>
     *
     * <p>校验失败时 {@link ExecutionContext#errorMsgCode} 非空，其它字段未填充。</p>
     */
    ExecutionContext resolveContext(RequestContext context) {
        Long userId = context.getUserId();
        Map<String, String> ext = context.getExtParams();
        String rawId = ext == null ? null : ext.get("activityId");
        if (rawId == null || rawId.isBlank()) {
            return ExecutionContext.fail("ACTIVITY_ID_REQUIRED");
        }
        Long activityId;
        try {
            activityId = Long.parseLong(rawId.trim());
        } catch (NumberFormatException e) {
            return ExecutionContext.fail("ACTIVITY_ID_INVALID");
        }
        Optional<Activity> opt = activityRepository.findById(activityId);
        if (opt.isEmpty()) {
            return ExecutionContext.fail("ACTIVITY_NOT_FOUND");
        }
        List<Task> tasks = loadConfigTasks(opt.get());
        return ExecutionContext.ok(userId, opt.get(), tasks);
    }

    // ---------------------- helpers ----------------------

    /**
     * 从 {@link Activity#getConfigurations()} 反序列化 {@link TaskConfig}。
     * 配置缺失或解析失败时返回空列表（视为"该活动未启用任何任务"，不报错）。
     */
    List<Task> loadConfigTasks(Activity activity) {
        String raw = activity.getConfigurations();
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    raw, new TypeReference<Map<String, Object>>() {});
            Object payload = root.get(CONFIG_KEY);
            if (payload == null) {
                return Collections.emptyList();
            }
            TaskConfig cfg = objectMapper.convertValue(payload, TaskConfig.class);
            return cfg.getTasks() == null ? Collections.emptyList() : cfg.getTasks();
        } catch (Exception e) {
            log.warn("反序列化 TaskConfig 失败 activityId={}", activity.getId(), e);
            return Collections.emptyList();
        }
    }

    // ---------------------- 内部数据结构 ----------------------

    /**
     * processor 间共享的入参上下文。{@link #errorMsgCode} 为 {@code null} 表示成功；
     * 非空时即对应的失败 msgCode，processor 应据此短路返回。
     * 成功时携带 {@link #userId} / {@link #activity} / {@link #tasks}（任务列表
     * 由 {@link #resolveContext(RequestContext)} 一并加载，避免被 processor
     * 边界丢弃，详见类注释）。
     */
    static final class ExecutionContext {
        String errorMsgCode;
        Long userId;
        Activity activity;
        List<Task> tasks;

        static ExecutionContext ok(Long userId, Activity activity, List<Task> tasks) {
            ExecutionContext ec = new ExecutionContext();
            ec.userId = userId;
            ec.activity = activity;
            ec.tasks = tasks;
            return ec;
        }

        static ExecutionContext fail(String msgCode) {
            ExecutionContext ec = new ExecutionContext();
            ec.errorMsgCode = msgCode;
            return ec;
        }
    }
}
