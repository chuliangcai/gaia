package com.gaia.domain.ability.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia.domain.ability.task.config.Task;
import com.gaia.domain.ability.task.config.TaskConfig;
import com.gaia.domain.ability.task.dto.RewardIssueResult;
import com.gaia.domain.ability.task.dto.TaskExecuteResultDTO;
import com.gaia.domain.ability.treasure.ext.TaskRewardIssueExt;
import com.gaia.domain.base.activity.ActionType;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.base.activity.ActivityRecord;
import com.gaia.domain.member.MemberBenefit;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务执行 Ability（processor 链式版）。
 *
 * <p>该能力不覆写父类的 {@link #execute(RequestContext)}；执行骨架由
 * {@link AbstractBaseAbility} 默认实现按 {@link #getProcessor(String)} 返回的顺序
 * 反射调用同名 processor 方法（每个处理器名对应一个签名匹配的方法）：</p>
 * <ol>
 *     <li>{@link #preCheck(RequestContext) preCheck} —
 *     校验任务今天是否已被执行（按 {@link ActivityRecord} 当日窗口计数），
 *     以及累计执行次数是否超出配额；失败即短路。</li>
 *     <li>{@link #executeTask(RequestContext) executeTask} —
 *     幂等插入一条 {@code TASK_EXECUTED} 明细记录，明细的 {@code actionCode}
 *     作为幂等键，命中已存在记录时返回上次结果（idempotent=true）。</li>
 *     <li>{@link #issueReward(RequestContext) issueReward} —
 *     通过扩展点（{@code TaskRewardIssueExt}）为该用户发放夺宝码等权益；
 *     会员夺宝玩法下由 {@code TreasureBizProduct.issuance(...)} 1:1 委托给
 *     {@code TreasureCodeIssuer.issue(...)}。</li>
 * </ol>
 *
 * <p>扩展点调用通过 {@link AbstractBaseAbility#reduceExecute(RequestContext, String,
 * AbstractBaseAbility.ExtensionCallback)} 完成，bizCode 由运行时上下文注入，
 * 领域层不感知玩法归属。</p>
 */
@Slf4j
@Component
@Ability(code = "task_execute", bizNs = "task", name = "任务执行", configClass = TaskConfig.class)
@RequiredArgsConstructor
public class TaskExecuteAbility extends AbstractBaseAbility {

    /** 扩展点 code：奖品发放（与 {@code TaskRewardIssueExt.code()} 对应） */
    private static final String EXT_ISSUANCE = "issuance";

    /** 处理器名：前置校验 */
    private static final String PROC_PRE_CHECK = "preCheck";
    /** 处理器名：插入活动明细 */
    private static final String PROC_EXECUTE_TASK = "executeTask";
    /** 处理器名：发放奖品 */
    private static final String PROC_ISSUE_REWARD = "issueReward";

    /** 幂等键前缀。完整 key 形如 {@code task_exec:{userId}:{taskId}:{yyyy-MM-dd}:{requestId}} */
    private static final String ACTION_CODE_PREFIX = "task_exec";

    private static final String CONFIG_KEY = TaskConfig.class.getName();

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final MemberBenefitRepository memberBenefitRepository;
    private final ObjectMapper objectMapper;

    // ---------------------- processor 声明 ----------------------

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of(PROC_PRE_CHECK, PROC_EXECUTE_TASK, PROC_ISSUE_REWARD);
    }

    // ---------------------- processor 实现 ----------------------

    /**
     * 前置校验：解析入参、校验活动与任务状态、检查"今天是否已执行过"、
     * 检查累计执行次数是否超出配额。
     */
    IFResult<ExecutionContext> preCheck(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }

        // 今日已执行过则短路
        LocalDate today = LocalDate.now();
        long todayCount = activityRecordRepository.countByUserTaskOnDay(
                ec.userId, ec.activityId, ec.task.getId(),
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        if (todayCount > 0) {
            log.info("任务今日已执行 userId={} activityId={} taskId={}",
                    ec.userId, ec.activityId, ec.task.getId());
            return IFResult.fail("TASK_ALREADY_EXECUTED_TODAY");
        }

        // 累计配额校验
        long executedCount = activityRecordRepository.countByUserTask(
                ec.userId, ec.activityId, ec.task.getId());
        if (!ec.task.isAvailableFor((int) executedCount)) {
            return IFResult.fail("TASK_EXHAUSTED");
        }

        return IFResult.success(ec);
    }

    /**
     * 执行任务：构造幂等键 → 幂等命中检查 → 写入一条 {@code TASK_EXECUTED} 明细。
     * 返回结果 model 为 {@link TaskExecuteResultDTO}，幂等命中时
     * {@code idempotent=true} 且不重复发放奖品。
     */
    IFResult<TaskExecuteResultDTO> executeTask(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            // 走兜底：上游失败时应被 preCheck 短路；此处防御性返回同一失败。
            return IFResult.fail(ec.errorMsgCode);
        }
        String actionCode = buildActionCode(ec);

        // 幂等命中：返回上次结果
        Optional<ActivityRecord> existing = activityRecordRepository.findByActionCode(actionCode);
        if (existing.isPresent()) {
            return buildIdempotentResult(ec, existing.get());
        }

        return doExecuteTaskTx(ec, actionCode);
    }

    /**
     * 发放奖品：调用扩展点拿到 {@link RewardIssueResult} 列表，
     * 统一落 {@code memberbenefit} 账本（{@link MemberBenefitRepository#insertBenefits}）。
     *
 * <p>v1 不做重试：若 UK 冲突（如 LOTTERY_CODE 在同活动内重复），{@link DuplicateKeyException}
     * 直接抛出，由上游 processor 错误处理兜底。</p>
     */
    IFResult<TaskExecuteResultDTO> issueReward(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }
        Long recordId = resolveRecordId(ec);
        if (recordId == null) {
            return IFResult.fail("RECORD_ID_MISSING");
        }

        int rewardCount = ec.task.getRewardCount() == null ? 1 : ec.task.getRewardCount();
        List<RewardIssueResult> issued = reduceExecute(
                context, EXT_ISSUANCE,
                ext -> ((TaskRewardIssueExt) ext).issue(ec.userId, ec.activityId, rewardCount, recordId));

        List<MemberBenefit> rows = new java.util.ArrayList<>(issued.size());
        for (RewardIssueResult r : issued) {
            if (r == null || r.getBenefitContent() == null) {
                continue;
            }
            rows.add(MemberBenefit.builder()
                    .userId(ec.userId)
                    .benefitType(r.getBenefitType() != null ? r.getBenefitType().name() : null)
                    .benefitValue(r.getBenefitContent())
                    .activityId(ec.activityId)
                    .effectiveActivityId(r.getAvailableActivityId())
                    .sourceId(recordId)
                    .status(1)
                    .grantedAt(java.time.LocalDateTime.now())
                    .build());
        }
        memberBenefitRepository.insertBenefits(rows);

        // 抽取 benefitContent 透传给上游 DTO（保留 newCodes 字段以避免动对外契约）
        List<String> newCodes = new java.util.ArrayList<>(issued.size());
        for (RewardIssueResult r : issued) {
            if (r != null && r.getBenefitContent() != null) {
                newCodes.add(r.getBenefitContent());
            }
        }

        int totalCodes = memberBenefitRepository
                .listLotteryCodesByUserAndActivity(ec.userId, ec.activityId).size();
        return IFResult.success(TaskExecuteResultDTO.builder()
                .newCodes(newCodes)
                .totalCodes(totalCodes)
                .executedCount((int) activityRecordRepository
                        .countByUserTask(ec.userId, ec.activityId, ec.task.getId()))
                .idempotent(false)
                .build());
    }

    // ---------------------- 事务 ----------------------

    @Transactional(rollbackFor = Exception.class)
    IFResult<TaskExecuteResultDTO> doExecuteTaskTx(ExecutionContext ec, String actionCode) {
        // 先写明细占位（拿到 recordId 当 sourceId 给奖品发放扩展点用）
        ActivityRecord placeholder = ActivityRecord.builder()
                .userId(ec.userId)
                .activityId(ec.activityId)
                .actionType(ActionType.TASK_EXECUTED)
                .actionCode(actionCode)
                .bizId(ec.task.getId())
                .payload(null)
                .result(null)
                .createdAt(LocalDateTime.now())
                .build();
        boolean inserted = activityRecordRepository.insertIfAbsent(placeholder);
        if (!inserted) {
            // 并发场景：另一线程抢到了，回退返回上次结果
            Optional<ActivityRecord> existing = activityRecordRepository.findByActionCode(actionCode);
            if (existing.isPresent()) {
                return buildIdempotentResult(ec, existing.get());
            }
            return IFResult.fail("DUPLICATE_REQUEST");
        }
        ec.recordId = placeholder.getId();
        log.info("任务明细已写入 userId={} activityId={} taskId={} recordId={} actionCode={}",
                ec.userId, ec.activityId, ec.task.getId(), ec.recordId, actionCode);

        int totalCodes = memberBenefitRepository
                .listLotteryCodesByUserAndActivity(ec.userId, ec.activityId).size();
        return IFResult.success(TaskExecuteResultDTO.builder()
                .newCodes(List.of())
                .totalCodes(totalCodes)
                .executedCount((int) activityRecordRepository
                        .countByUserTask(ec.userId, ec.activityId, ec.task.getId()))
                .idempotent(false)
                .build());
    }

    // ---------------------- 入参解析 ----------------------

    /**
     * 从 {@link RequestContext} 提取入参、校验活动与任务配置。
     * 校验失败时 {@link ExecutionContext#errorMsgCode} 非空；其它字段可能未填充。
     */
    ExecutionContext resolveContext(RequestContext context) {
        Long userId = context.getUserId();
        if (userId == null || userId <= 0) {
            return ExecutionContext.fail("USER_ID_REQUIRED");
        }
        Map<String, String> ext = context.getExtParams();
        if (ext == null) {
            return ExecutionContext.fail("PARAMS_REQUIRED");
        }
        String activityIdStr = ext.get("activityId");
        String taskIdStr = ext.get("taskId");
        String requestId = ext.get("requestId");
        if (activityIdStr == null || taskIdStr == null) {
            return ExecutionContext.fail("ACTIVITY_ID_TASK_ID_REQUIRED");
        }
        if (requestId == null || requestId.isBlank()) {
            return ExecutionContext.fail("REQUEST_ID_REQUIRED");
        }
        Long activityId = parseLong(activityIdStr);
        Long taskId = parseLong(taskIdStr);
        if (activityId == null || taskId == null) {
            return ExecutionContext.fail("INVALID_PARAMS");
        }

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ExecutionContext.fail("ACTIVITY_NOT_FOUND");
        }
        if (!activity.isOngoing()) {
            return ExecutionContext.fail("ACTIVITY_NOT_ONGOING");
        }
        Task task = loadConfigTask(activity, taskId);
        if (task == null) {
            return ExecutionContext.fail("TASK_NOT_FOUND");
        }

        return ExecutionContext.ok(userId, activityId, task, requestId);
    }

    /**
     * 幂等键：{@code task_exec:{userId}:{taskId}:{yyyy-MM-dd}:{requestId}}。
     * 包含日期使得跨日重放能拿到新 executionCode；
     * 包含 requestId 保证同日重放仍是同一幂等键。
     */
    String buildActionCode(ExecutionContext ec) {
        return ACTION_CODE_PREFIX
                + ":" + ec.userId
                + ":" + ec.task.getId()
                + ":" + LocalDate.now()
                + ":" + ec.requestId;
    }

    IFResult<TaskExecuteResultDTO> buildIdempotentResult(ExecutionContext ec, ActivityRecord rec) {
        ec.recordId = rec.getId();
        Map<String, Object> result = parseJson(rec.getResult());
        List<String> codes = toStringList(result.get("codes"));
        int totalCodes = memberBenefitRepository
                .listLotteryCodesByUserAndActivity(ec.userId, ec.activityId).size();
        return IFResult.success(TaskExecuteResultDTO.builder()
                .newCodes(codes == null ? List.of() : codes)
                .totalCodes(totalCodes)
                .executedCount((int) activityRecordRepository
                        .countByUserTask(ec.userId, ec.activityId, ec.task.getId()))
                .idempotent(true)
                .build());
    }

    /**
     * 解析奖品发放所需的来源明细 ID：优先使用 {@link ExecutionContext#recordId}
     * （{@link #executeTask} 刚写完明细的场景），否则按 actionCode 反查已存在明细
     * （幂等命中场景）。返回 {@code null} 表示无法解析。
     */
    Long resolveRecordId(ExecutionContext ec) {
        if (ec.recordId != null) {
            return ec.recordId;
        }
        return activityRecordRepository.findByActionCode(buildActionCode(ec))
                .map(ActivityRecord::getId)
                .orElse(null);
    }

    // ---------------------- helpers ----------------------

    /**
     * 从 {@link Activity#getConfigurations()} 反序列化 {@link TaskConfig}，
     * 并按 id 找出指定任务。找不到返回 {@code null}。
     */
    Task loadConfigTask(Activity activity, Long taskId) {
        String raw = activity.getConfigurations();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    raw, new TypeReference<Map<String, Object>>() {});
            Object payload = root.get(CONFIG_KEY);
            if (payload == null) {
                return null;
            }
            TaskConfig cfg = objectMapper.convertValue(payload, TaskConfig.class);
            List<Task> tasks = cfg.getTasks();
            if (tasks == null || tasks.isEmpty()) {
                return null;
            }
            for (Task t : tasks) {
                if (taskId.equals(t.getId())) {
                    return t;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("反序列化 TaskConfig 失败 activityId={}", activity.getId(), e);
            return null;
        }
    }

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseJson(String s) {
        if (s == null || s.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(s,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON 反序列化失败: {}", s, e);
            return Collections.emptyMap();
        }
    }

    private List<String> toStringList(Object o) {
        if (o == null) return List.of();
        if (o instanceof List<?> list) {
            List<String> out = new java.util.ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) out.add(String.valueOf(item));
            }
            return out;
        }
        return List.of();
    }

    // ---------------------- 内部数据结构 ----------------------

    /**
     * processor 间共享的入参上下文。{@link #errorMsgCode} 为 {@code null} 表示成功；
     * 非空时即对应的失败 msgCode，processor 应据此短路返回。
     * 成功时携带 {@link #userId} / {@link #activityId} / {@link #task} / {@link #requestId}。
     * {@link #recordId} 在 {@link #executeTask(RequestContext)} 写完明细后填充，
     * {@link #issueReward(RequestContext)} 据此调用扩展点。
     */
    static final class ExecutionContext {
        String errorMsgCode;
        Long userId;
        Long activityId;
        Task task;
        String requestId;
        Long recordId;

        static ExecutionContext ok(Long userId, Long activityId, Task task, String requestId) {
            ExecutionContext ec = new ExecutionContext();
            ec.userId = userId;
            ec.activityId = activityId;
            ec.task = task;
            ec.requestId = requestId;
            return ec;
        }

        static ExecutionContext fail(String msgCode) {
            ExecutionContext ec = new ExecutionContext();
            ec.errorMsgCode = msgCode;
            return ec;
        }
    }
}
