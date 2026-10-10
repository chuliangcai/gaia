package com.gaia.server.treasure.ability;

import com.gaia.domain.activity.Activity;
import com.gaia.domain.activity.ActivityTask;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.ActivityTaskRepository;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import com.gaia.server.treasure.dto.ActivityDTO;
import com.gaia.server.treasure.dto.ActivityPageDTO;
import com.gaia.server.treasure.dto.CountdownDTO;
import com.gaia.server.treasure.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 活动页 Ability。
 *
 * <p>subType：</p>
 * <ul>
 *   <li>{@code info} — 首屏聚合（奖品 + 我的夺宝码 + 任务列表）</li>
 *   <li>{@code countdown} — 倒计时（用于未开始页面）</li>
 *   <li>{@code status} — 活动状态（前端轮询）</li>
 * </ul>
 */
@Slf4j
@Component
@Ability(code = "activity_page", bizNs = "treasure", name = "活动页")
@RequiredArgsConstructor
public class ActivityPageAbility extends AbstractBaseAbility {

    private static final String SUB_INFO = "info";
    private static final String SUB_COUNTDOWN = "countdown";
    private static final String SUB_STATUS = "status";

    private final ActivityRepository activityRepository;
    private final ActivityTaskRepository activityTaskRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final MemberBenefitRepository memberBenefitRepository;

    @Override
    public IFResult<?> execute(RequestContext context) {
        String sub = resolveSubType(context);
        return switch (sub) {
            case SUB_INFO -> doInfo(context);
            case SUB_COUNTDOWN -> doCountdown(context);
            case SUB_STATUS -> doStatus(context);
            default -> IFResult.fail("UNKNOWN_SUBTYPE");
        };
    }

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of(SUB_INFO, SUB_COUNTDOWN, SUB_STATUS);
    }

    private String resolveSubType(RequestContext context) {
        Map<String, String> ext = context.getExtParams();
        String sub = (ext == null) ? null : ext.get("subType");
        return (sub == null || sub.isBlank()) ? SUB_INFO : sub;
    }

    private IFResult<ActivityPageDTO> doInfo(RequestContext context) {
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
        Activity activity = opt.get();
        Long activityId = activity.getId();

        List<String> myCodes = (userId == null || userId <= 0)
                ? List.of()
                : memberBenefitRepository.listLotteryCodesByUserAndActivity(userId, activityId);

        List<ActivityTask> tasks = activityTaskRepository.findByActivityId(activityId);
        List<TaskDTO> taskDTOs = new ArrayList<>(tasks.size());
        for (ActivityTask t : tasks) {
            long executed = 0;
            if (userId != null && userId > 0) {
                executed = activityRecordRepository.countByUserTask(userId, activityId, t.getId());
            }
            taskDTOs.add(TaskDTO.builder()
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

        ActivityPageDTO dto = ActivityPageDTO.builder()
                .activity(toDTO(activity))
                .myCodes(myCodes)
                .tasks(taskDTOs)
                .totalCodes(myCodes.size())
                .build();
        return IFResult.success(dto);
    }

    private IFResult<CountdownDTO> doCountdown(RequestContext context) {
        String activityCode = context.getExtParams() == null ? null
                : context.getExtParams().get("activityCode");
        if (activityCode == null || activityCode.isBlank()) {
            return IFResult.fail("ACTIVITY_CODE_REQUIRED");
        }
        Optional<Activity> opt = activityRepository.findByCode(activityCode);
        if (opt.isEmpty()) {
            return IFResult.fail("ACTIVITY_NOT_FOUND");
        }
        Activity a = opt.get();
        return IFResult.success(CountdownDTO.builder()
                .activityId(a.getId())
                .activityCode(a.getActivityCode())
                .status(a.getStatus())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .serverNow(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
                .drawAt(a.getDrawAt())
                .build());
    }

    private IFResult<CountdownDTO> doStatus(RequestContext context) {
        return doCountdown(context);
    }

    private static ActivityDTO toDTO(Activity a) {
        return ActivityDTO.builder()
                .id(a.getId())
                .activityCode(a.getActivityCode())
                .name(a.getName())
                .prizeDesc(a.getPrizeDesc())
                .prizeImgUrl(a.getPrizeImgUrl())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus())
                .winnerCount(a.getWinnerCount())
                .drawAt(a.getDrawAt())
                .build();
    }
}