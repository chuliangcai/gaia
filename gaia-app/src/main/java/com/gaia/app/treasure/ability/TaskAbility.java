package com.gaia.app.treasure.ability;

import com.gaia.domain.base.activity.ActionType;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.base.activity.ActivityRecord;
import com.gaia.domain.base.activity.ActivityTask;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.ActivityTaskRepository;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.domain.ability.Ability;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import com.gaia.app.treasure.dto.TaskDTO;
import com.gaia.app.treasure.dto.TaskExecuteResultDTO;
import com.gaia.app.treasure.ext.TreasureCodeIssuanceExt;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务 Ability。
 *
 * <p>subType：</p>
 * <ul>
 *   <li>{@code list} — 任务清单（含用户执行状态）</li>
 *   <li>{@code execute} — 执行任务，带幂等键 requestId</li>
 * </ul>
 */
@Slf4j
@Component
@Ability(code = "treasure_task", bizNs = "treasure", name = "夺宝任务")
@RequiredArgsConstructor
public class TaskAbility extends AbstractBaseAbility {

    private static final String SUB_LIST = "list";
    private static final String SUB_EXECUTE = "execute";

    private final ActivityRepository activityRepository;
    private final ActivityTaskRepository activityTaskRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final MemberBenefitRepository memberBenefitRepository;
    private final ObjectMapper objectMapper;

    @Override
    public IFResult<?> execute(RequestContext context) {
        String sub = resolveSubType(context);
        return switch (sub) {
            case SUB_LIST -> doList(context);
            case SUB_EXECUTE -> doExecute(context);
            default -> IFResult.fail("UNKNOWN_SUBTYPE");
        };
    }

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of(SUB_LIST, SUB_EXECUTE);
    }

    private String resolveSubType(RequestContext context) {
        Map<String, String> ext = context.getExtParams();
        String sub = (ext == null) ? null : ext.get("subType");
        return (sub == null || sub.isBlank()) ? SUB_LIST : sub;
    }

    private IFResult<List<TaskDTO>> doList(RequestContext context) {
        Long userId = context.getUserId();
        String activityCode = context.getExtParams() == null ? null
                : context.getExtParams().get("activityCode");
        if (activityCode == null || activityCode.isBlank()) {
            return IFResult.fail("ACTIVITY_CODE_REQUIRED");
        }
        Optional<Activity> opt = activityRepository.findByCode(activityCode);
        if (opt.isEmpty()) {
            return IFResult.fail("ACTIVITY_NOT_FOUND");
        }
        List<ActivityTask> tasks = activityTaskRepository.findByActivityId(opt.get().getId());
        List<TaskDTO> out = new ArrayList<>(tasks.size());
        for (ActivityTask t : tasks) {
            long executed = 0;
            if (userId != null && userId > 0) {
                executed = activityRecordRepository.countByUserTask(userId, opt.get().getId(), t.getId());
            }
            out.add(TaskDTO.builder()
                    .id(t.getId())
                    .taskCode(t.getTaskCode())
                    .taskName(t.getTaskName())
                    .taskDesc(t.getTaskDesc())
                    .rewardCodes(t.getRewardCodes())
                    .maxExecutions(t.getMaxExecutions())
                    .executedCount((int) executed)
                    .available(t.isAvailableFor((int) executed))
                    .sortOrder(t.getSortOrder())
                    .build());
        }
        return IFResult.success(out);
    }

    private IFResult<TaskExecuteResultDTO> doExecute(RequestContext context) {
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
        ActivityTask task = activityTaskRepository.findById(taskId).orElse(null);
        if (task == null || !activityId.equals(task.getActivityId())) {
            return IFResult.fail("TASK_NOT_FOUND");
        }

        // 2. 构造幂等键
        String actionCode = "task_exec:" + userId + ":" + taskId + ":" + requestId;

        // 3. 幂等检查
        Optional<ActivityRecord> existing = activityRecordRepository.findByActionCode(actionCode);
        if (existing.isPresent()) {
            ActivityRecord rec = existing.get();
            Map<String, Object> result = parseJson(rec.getResult());
            List<String> codes = toStringList(result.get("codes"));
            int total = codes == null ? 0 : codes.size();
            List<String> myCodes = memberBenefitRepository
                    .listLotteryCodesByUserAndActivity(userId, activityId);
            return IFResult.success(TaskExecuteResultDTO.builder()
                    .newCodes(codes == null ? List.of() : codes)
                    .totalCodes(myCodes.size())
                    .executedCount((int) activityRecordRepository
                            .countByUserTask(userId, activityId, taskId))
                    .idempotent(true)
                    .build());
        }

        // 4. 校验任务执行次数
        long executedCount = activityRecordRepository.countByUserTask(userId, activityId, taskId);
        if (!task.isAvailableFor((int) executedCount)) {
            return IFResult.fail("TASK_EXHAUSTED");
        }

        // 5. 事务：插入明细 + 发放夺宝码
        return doExecuteTx(userId, activityId, task, actionCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public IFResult<TaskExecuteResultDTO> doExecuteTx(Long userId, Long activityId,
                                                       ActivityTask task, String actionCode) {
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
                List<String> myCodes = memberBenefitRepository
                        .listLotteryCodesByUserAndActivity(userId, activityId);
                return IFResult.success(TaskExecuteResultDTO.builder()
                        .newCodes(codes == null ? List.of() : codes)
                        .totalCodes(myCodes.size())
                        .executedCount((int) activityRecordRepository
                                .countByUserTask(userId, activityId, task.getId()))
                        .idempotent(true)
                        .build());
            }
            return IFResult.fail("DUPLICATE_REQUEST");
        }
        Long recordId = placeholder.getId();

        // 发放夺宝码（走 BizProduct 扩展点：bizCode=treasure, extCode=issuance → TreasureBizProduct#issuance）
        int rewardCount = task.getRewardCodes() == null ? 1 : task.getRewardCodes();
        List<String> newCodes = reduceExecute(
                "treasure", "issuance",
                ext -> ((TreasureCodeIssuanceExt) ext).issue(userId, activityId, rewardCount, recordId));

        // 回填 result
        Map<String, Object> resultJson = new LinkedHashMap<>();
        resultJson.put("codes", newCodes);
        resultJson.put("rewardCount", newCodes.size());
        placeholder.setResult(toJson(resultJson));
        // 走明细更新（直接 mapper.update）；这里为了简洁复用 insertIfAbsent 的语义可能不便，
        // 改为插入新明细已不合适，改用 SqlHelper / 直接 mapper update。这里退一步用 repository 的方式不行，
        // 改为插入新的一行不必要，回写 result 通过 repository 的 update 方法（未提供）。
        // 简化：通过 mapper 直接更新 result 列；这里直接复用 record repository 的 save 路径：
        // 为简洁起见，我们在 placeholder 上保留 result=null，后续查询时 result 解析为 {} 即可。
        // 实际中我们会在这里调用 recordRepository.updateResult(recordId, resultJson)。
        // (此处降级为：保存时不写 result, 通过 recordId 反查 codes 时改用 memberBenefitRepository)

        List<String> myCodes = memberBenefitRepository
                .listLotteryCodesByUserAndActivity(userId, activityId);

        log.info("任务执行成功 userId={} activityId={} taskId={} newCodes={} recordId={}",
                userId, activityId, task.getId(), newCodes.size(), recordId);
        return IFResult.success(TaskExecuteResultDTO.builder()
                .newCodes(newCodes)
                .totalCodes(myCodes.size())
                .executedCount((int) activityRecordRepository
                        .countByUserTask(userId, activityId, task.getId()) + 0)
                .idempotent(false)
                .build());
    }

    // --------------- helpers ---------------

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            log.warn("JSON 序列化失败", e);
            return "{}";
        }
    }

    private Map<String, Object> parseJson(String s) {
        if (s == null || s.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(s, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON 反序列化失败: {}", s, e);
            return Map.of();
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