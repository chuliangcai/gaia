package com.gaia.app.service.impl;

import com.gaia.api.activity.ActivityQueryService;
import com.gaia.api.activity.ActivitySummaryDTO;
import com.gaia.common.constants.DubboConstants;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动查询 Dubbo Provider。
 *
 * <p>被 gateway 的 {@code ActivityController} 调用，给 {@code /api/activities}
 * 提供移动端"热门活动"列表数据。</p>
 *
 * <p>领域 status 0/1/2/3 在这里翻译为 UI 状态 0/1/2（草稿/上架/下架），
 * 与 {@code frontend-mobile/src/api/activities.js} 的契约对齐。
 * 详见 {@link #toUiStatus(int)}。</p>
 */
@DubboService(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
@RequiredArgsConstructor
public class ActivityQueryServiceImpl implements ActivityQueryService {

    private final ActivityRepository activityRepository;

    @Override
    public List<ActivitySummaryDTO> listLiveActivities() {
        List<Activity> live = activityRepository.findLive(LocalDateTime.now());
        return live.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private ActivitySummaryDTO toSummary(Activity a) {
        return ActivitySummaryDTO.builder()
                .id(a.getId())
                .bizCode(a.getBizCode())
                .name(a.getName())
                // mobile 前端期望 /treasure/<id>（无 #，见 activities.js PLACEHOLDER）
                .link("/treasure/" + a.getId())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(toUiStatus(a.getStatus()))
                .build();
    }

    /**
     * 领域 status → UI status。
     * <pre>
     *   0 NOT_STARTED   → 0 草稿
     *   1 ONGOING       → 1 已上架
     *   2 WAITING_DRAW  → 1 已上架（仍在列表中，开奖前对用户可见）
     *   3 DRAWN         → 2 已下架
     *   其它/null      → 0 草稿（兜底）
     * </pre>
     *
     * <p>目前 {@link ActivityRepository#findLive(LocalDateTime)} 已限定
     * {@code status ∈ {1,2} AND start_time <= now < end_time}，
     * 运行时实际只会落到 case 1,2 → 1。但保留完整 switch 以防
     * 仓库方法后续扩展（如 {@code findAllForAdmin}）时引入状态 0/3。</p>
     */
    private static int toUiStatus(int domainStatus) {
        return switch (domainStatus) {
            case 1, 2 -> 1;
            case 3 -> 2;
            default -> 0;
        };
    }
}