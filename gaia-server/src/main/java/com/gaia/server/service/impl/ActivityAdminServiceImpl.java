package com.gaia.server.service.impl;

import com.gaia.api.activity.ActivityAdminService;
import com.gaia.api.activity.ActivityCreateRequest;
import com.gaia.api.activity.ActivityUpdateRequest;
import com.gaia.api.activity.MemberSeedDTO;
import com.gaia.api.activity.TaskConfigDTO;
import com.gaia.common.constants.DubboConstants;
import com.gaia.domain.activity.Activity;
import com.gaia.domain.activity.ActivityTask;
import com.gaia.domain.member.Member;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.ActivityTaskRepository;
import com.gaia.domain.repository.MemberRepository;
import com.gaia.server.treasure.service.DrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 活动管理 Dubbo Provider（后台管理调用入口）。
 */
@Slf4j
@DubboService(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
@RequiredArgsConstructor
public class ActivityAdminServiceImpl implements ActivityAdminService {

    private final ActivityRepository activityRepository;
    private final ActivityTaskRepository activityTaskRepository;
    private final MemberRepository memberRepository;
    private final DrawService drawService;

    @Override
    public Long createActivity(ActivityCreateRequest req) {
        if (req == null || req.getActivityCode() == null) {
            throw new IllegalArgumentException("activityCode 必填");
        }
        Activity a = Activity.builder()
                .activityCode(req.getActivityCode())
                .name(req.getName())
                .prizeDesc(req.getPrizeDesc() == null ? "" : req.getPrizeDesc())
                .prizeImgUrl(req.getPrizeImgUrl() == null ? "" : req.getPrizeImgUrl())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(0)
                .winnerCount(req.getWinnerCount() == null ? 1 : req.getWinnerCount())
                .build();
        Activity saved = activityRepository.save(a);
        log.info("创建活动 id={} code={}", saved.getId(), saved.getActivityCode());
        return saved.getId();
    }

    @Override
    public void updateActivity(Long id, ActivityUpdateRequest req) {
        if (id == null || req == null) {
            throw new IllegalArgumentException("id 与 req 必填");
        }
        Activity a = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("活动不存在 id=" + id));
        if (req.getName() != null) {
            a.setName(req.getName());
        }
        if (req.getPrizeDesc() != null) {
            a.setPrizeDesc(req.getPrizeDesc());
        }
        if (req.getPrizeImgUrl() != null) {
            a.setPrizeImgUrl(req.getPrizeImgUrl());
        }
        if (req.getStartTime() != null) {
            a.setStartTime(req.getStartTime());
        }
        if (req.getEndTime() != null) {
            a.setEndTime(req.getEndTime());
        }
        if (req.getWinnerCount() != null) {
            a.setWinnerCount(req.getWinnerCount());
        }
        activityRepository.save(a);
        log.info("更新活动 id={}", id);
    }

    @Override
    public void configureTasks(Long activityId, List<TaskConfigDTO> tasks) {
        if (activityId == null) {
            throw new IllegalArgumentException("activityId 必填");
        }
        if (activityRepository.findById(activityId).isEmpty()) {
            throw new IllegalStateException("活动不存在 id=" + activityId);
        }
        List<ActivityTask> entities = new ArrayList<>();
        if (tasks != null) {
            for (TaskConfigDTO t : tasks) {
                entities.add(ActivityTask.builder()
                        .taskCode(t.getTaskCode())
                        .taskName(t.getTaskName())
                        .taskDesc(t.getTaskDesc() == null ? "" : t.getTaskDesc())
                        .rewardCodes(t.getRewardCodes() == null ? 1 : t.getRewardCodes())
                        .maxExecutions(t.getMaxExecutions() == null ? 1 : t.getMaxExecutions())
                        .sortOrder(t.getSortOrder() == null ? 0 : t.getSortOrder())
                        .status(t.getStatus() == null ? 1 : t.getStatus())
                        .build());
            }
        }
        int n = activityTaskRepository.replaceByActivityId(activityId, entities);
        log.info("配置任务完成 activityId={} size={}", activityId, n);
    }

    @Override
    public void triggerDraw(Long activityId) {
        if (activityId == null) {
            throw new IllegalArgumentException("activityId 必填");
        }
        log.info("管理端手动触发开奖 activityId={}", activityId);
        drawService.draw(activityId);
    }

    @Override
    public void seedMembers(List<MemberSeedDTO> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        for (MemberSeedDTO m : members) {
            Member entity = Member.builder()
                    .userId(m.getUserId())
                    .level(m.getLevel() == null ? "NORMAL" : m.getLevel())
                    .status(m.getStatus() == null ? 1 : m.getStatus())
                    .joinAt(m.getJoinAt() == null ? LocalDateTime.now(ZoneId.of("Asia/Shanghai")) : m.getJoinAt())
                    .expireAt(m.getExpireAt())
                    .build();
            memberRepository.save(entity);
        }
        log.info("灌入会员数量={}", members.size());
    }
}