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
 *     解析入参（activityCode），按 code 加载 {@link Activity}。</li>
 *     <li>{@link #loadConfig(RequestContext) loadConfig} —
 *     从 {@link Activity#getConfigurations()} 反序列化 {@link TaskConfig}，
 *     得到任务列表。</li>
 *     <li>{@link #enrichProgress(RequestContext) enrichProgress} —
 *     对每个任务按当前用户查询累计执行次数，组装 {@link TaskDTO} 输出。</li>
 * </ol>
 *
 * <p>该能力与 {@link TaskExecuteAbility} 平级、互不重叠，二者共享同一份
 * {@code TaskConfig}。该能力不需要扩展点（无玩法侧差异化逻辑）。</p>
 */
@Slf4j
@Component
@Ability(code = "task_list", bizNs = "task", name = "任务清单", configClass = TaskConfig.class)
@RequiredArgsConstructor
public class TaskListAbility extends AbstractBaseAbility {

    /** 处理器名：前置校验 */
    private static final String PROC_PRE_CHECK = "preCheck";
    /** 处理器名：加载配置 */
    private static final String PROC_LOAD_CONFIG = "loadConfig";
    /** 处理器名：填充执行进度 */
    private static final String PROC_ENRICH_PROGRESS = "enrichProgress";

    private static final String CONFIG_KEY = TaskConfig.class.getName();

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ObjectMapper objectMapper;

    // ---------------------- processor 声明 ----------------------

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of(PROC_PRE_CHECK, PROC_LOAD_CONFIG, PROC_ENRICH_PROGRESS);
    }

    // ---------------------- processor 实现 ----------------------

    /**
     * 前置校验：解析入参（activityCode）、按 code 加载 {@link Activity}。
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
     * 加载配置：从 {@link Activity#getConfigurations()} 反序列化
     * {@link TaskConfig}，得到任务列表（缺失或解析失败时视为空）。
     */
    IFResult<List<TaskDTO>> loadConfig(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }
        ec.tasks = loadConfigTasks(ec.activity);
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
     * 从 {@link RequestContext} 解析入参，按 activityCode 加载 {@link Activity}。
     * 校验失败时 {@link ExecutionContext#errorMsgCode} 非空；其它字段可能未填充。
     */
    ExecutionContext resolveContext(RequestContext context) {
        Long userId = context.getUserId();
        Map<String, String> ext = context.getExtParams();
        String activityCode = ext == null ? null : ext.get("activityCode");
        if (activityCode == null || activityCode.isBlank()) {
            return ExecutionContext.fail("ACTIVITY_CODE_REQUIRED");
        }
        Optional<Activity> opt = activityRepository.findByCode(activityCode);
        if (opt.isEmpty()) {
            return ExecutionContext.fail("ACTIVITY_NOT_FOUND");
        }
        return ExecutionContext.ok(userId, opt.get());
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
     * 成功时携带 {@link #userId} / {@link #activity}；{@link #tasks} 在
     * {@link #loadConfig(RequestContext)} 解析完成后填充，
     * {@link #enrichProgress(RequestContext)} 据此组装输出。
     */
    static final class ExecutionContext {
        String errorMsgCode;
        Long userId;
        Activity activity;
        List<Task> tasks;

        static ExecutionContext ok(Long userId, Activity activity) {
            ExecutionContext ec = new ExecutionContext();
            ec.userId = userId;
            ec.activity = activity;
            return ec;
        }

        static ExecutionContext fail(String msgCode) {
            ExecutionContext ec = new ExecutionContext();
            ec.errorMsgCode = msgCode;
            return ec;
        }
    }
}
