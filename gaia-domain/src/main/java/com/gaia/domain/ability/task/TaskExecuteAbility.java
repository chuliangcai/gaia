package com.gaia.domain.ability.task;

import com.gaia.domain.ability.task.config.Task;
import com.gaia.domain.ability.task.config.TaskConfig;
import com.gaia.domain.ability.task.dto.TaskExecuteResultDTO;
import com.gaia.domain.ability.treasure.ext.TreasureCodeIssuanceExt;
import com.gaia.domain.base.activity.ActionType;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.base.activity.ActivityRecord;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务执行 Ability。
 *
 * <p>幂等地为用户执行某活动下的某条任务，命中幂等键 {@code requestId} 时返回上次结果；
 * 成功后通过发放扩展点为该用户颁发夺宝码。该能力与 {@link TaskListAbility}
 * 共享同一份 {@link TaskConfig}。</p>
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

    private static final String EXT_ISSUANCE = "issuance";
    private static final String CONFIG_KEY = TaskConfig.class.getName();

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final MemberBenefitRepository memberBenefitRepository;
    private final ObjectMapper objectMapper;

    @Override
    public IFResult<?> execute(RequestContext context) {
        Long userId = context.getUserId();
        if (userId == null || userId <= 0) {
            return IFResult.fail("USER_ID_REQUIRED");
        }
        Map<String, String> ext = context.getExtParams();
        if (ext == null) {
            return IFResult.fail("PARAMS_REQUIRED");
        }
        String activityIdStr = ext.get("activityId");
        String taskIdStr = ext.get("taskId");
        String requestId = ext.get("requestId");
        if (activityIdStr == null || taskIdStr == null) {
            return IFResult.fail("ACTIVITY_ID_TASK_ID_REQUIRED");
        }
        if (requestId == null || requestId.isBlank()) {
            return IFResult.fail("REQUEST_ID_REQUIRED");
        }
        Long activityId = parseLong(activityIdStr);
        Long taskId = parseLong(taskIdStr);
        if (activityId == null || taskId == null) {
            return IFResult.fail("INVALID_PARAMS");
        }

        // 1. 校验活动状态
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return IFResult.fail("ACTIVITY_NOT_FOUND");
        }
        if (!activity.isOngoing()) {
            return IFResult.fail("ACTIVITY_NOT_ONGOING");
        }

        // 2. 从 configurations 中找出该 task
        Task task = loadConfigTask(activity, taskId);
        if (task == null) {
            return IFResult.fail("TASK_NOT_FOUND");
        }

        // 3. 构造幂等键
        String actionCode = "task_exec:" + userId + ":" + taskId + ":" + requestId;

        // 4. 幂等检查
        Optional<ActivityRecord> existing = activityRecordRepository.findByActionCode(actionCode);
        if (existing.isPresent()) {
            ActivityRecord rec = existing.get();
            Map<String, Object> result = parseJson(rec.getResult());
            List<String> codes = toStringList(result.get("codes"));
            int totalCodes = memberBenefitRepository
                    .listLotteryCodesByUserAndActivity(userId, activityId).size();
            return IFResult.success(TaskExecuteResultDTO.builder()
                    .newCodes(codes == null ? List.of() : codes)
                    .totalCodes(totalCodes)
                    .executedCount((int) activityRecordRepository
                            .countByUserTask(userId, activityId, taskId))
                    .idempotent(true)
                    .build());
        }

        // 5. 校验任务执行次数
        long executedCount = activityRecordRepository.countByUserTask(userId, activityId, taskId);
        if (!task.isAvailableFor((int) executedCount)) {
            return IFResult.fail("TASK_EXHAUSTED");
        }

        // 6. 事务：插入明细 + 发放夺宝码
        return doExecuteTx(userId, activityId, task, actionCode, context);
    }

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of();
    }

    @Transactional(rollbackFor = Exception.class)
    public IFResult<TaskExecuteResultDTO> doExecuteTx(Long userId, Long activityId,
                                                       Task task, String actionCode,
                                                       RequestContext context) {
        // 先写明细占位（拿到 recordId 当 sourceId）
        ActivityRecord placeholder = ActivityRecord.builder()
                .userId(userId)
                .activityId(activityId)
                .actionType(ActionType.TASK_EXECUTED)
                .actionCode(actionCode)
                .bizId(task.getId())
                .payload(null)
                .result(null)
                .createdAt(LocalDateTime.now())
                .build();
        boolean inserted = activityRecordRepository.insertIfAbsent(placeholder);
        if (!inserted) {
            // 并发场景：另一线程抢到了，回退返回上次结果
            Optional<ActivityRecord> existing = activityRecordRepository.findByActionCode(actionCode);
            if (existing.isPresent()) {
                Map<String, Object> result = parseJson(existing.get().getResult());
                List<String> codes = toStringList(result.get("codes"));
                int totalCodes = memberBenefitRepository
                        .listLotteryCodesByUserAndActivity(userId, activityId).size();
                return IFResult.success(TaskExecuteResultDTO.builder()
                        .newCodes(codes == null ? List.of() : codes)
                        .totalCodes(totalCodes)
                        .executedCount((int) activityRecordRepository
                                .countByUserTask(userId, activityId, task.getId()))
                        .idempotent(true)
                        .build());
            }
            return IFResult.fail("DUPLICATE_REQUEST");
        }
        Long recordId = placeholder.getId();

        // 发放夺宝码：bizCode 由 RequestContext 注入，领域代码不再硬编码玩法。
        int rewardCount = task.getRewardCodes() == null ? 1 : task.getRewardCodes();
        List<String> newCodes = reduceExecute(
                context, EXT_ISSUANCE,
                ext -> ((TreasureCodeIssuanceExt) ext).issue(userId, activityId, rewardCount, recordId));

        // result 回写省略（无 updateResult API），反查改走 memberBenefitRepository。
        log.info("任务执行成功 userId={} activityId={} taskId={} newCodes={} recordId={}",
                userId, activityId, task.getId(), newCodes.size(), recordId);

        int totalCodes = memberBenefitRepository
                .listLotteryCodesByUserAndActivity(userId, activityId).size();
        return IFResult.success(TaskExecuteResultDTO.builder()
                .newCodes(newCodes)
                .totalCodes(totalCodes)
                .executedCount((int) activityRecordRepository
                        .countByUserTask(userId, activityId, task.getId()))
                .idempotent(false)
                .build());
    }

    // --------------- helpers ---------------

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
            List<String> out = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) out.add(String.valueOf(item));
            }
            return out;
        }
        return List.of();
    }
}
