package com.gaia.backend.app.service;

import com.gaia.backend.app.dto.ActivityCreateRequest;
import com.gaia.backend.app.dto.ActivityUpdateRequest;
import com.gaia.backend.app.dto.ActivityViewDTO;
import com.gaia.backend.app.dto.MemberSeedDTO;
import com.gaia.backend.app.dto.TaskConfigDTO;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.base.activity.ActivityTask;
import com.gaia.domain.member.Member;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.ActivityTaskRepository;
import com.gaia.domain.repository.MemberRepository;
import com.gaia.domain.service.treasure.DrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动管理后台应用服务。
 *
 * <p>直接编排 gaia-domain 仓储与领域服务（DrawService），不再走 Dubbo。
 * 与早期 {@code com.gaia.app.service.impl.ActivityAdminServiceImpl}（已删除并被本类替换）的
 * 行为完全等价，便于前端 wire 形态保持不变。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAdminAppService {

    private final ActivityRepository activityRepository;
    private final ActivityTaskRepository activityTaskRepository;
    private final MemberRepository memberRepository;
    private final DrawService drawService;

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

    public List<ActivityViewDTO> listActivities() {
        return activityRepository.findAll().stream()
                .map(ActivityAdminAppService::toView)
                .collect(Collectors.toList());
    }

    public void deleteActivity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 必填");
        }
        activityRepository.deleteById(id);
        log.info("删除活动 id={}", id);
    }

    /**
     * 领域 Activity → 后台列表 DTO。
     * 状态机映射：0=草稿 1=上架 2=下架。
     */
    static ActivityViewDTO toView(Activity a) {
        Integer viewStatus;
        if (a.getStatus() == null) {
            viewStatus = 0;
        } else {
            switch (a.getStatus()) {
                case 0:
                    viewStatus = 0; // 未开始 → 草稿
                    break;
                case 1:
                case 2:
                    viewStatus = 1; // 进行中 / 待开奖 → 上架
                    break;
                default:
                    viewStatus = 2; // 已开奖 → 下架
            }
        }
        return ActivityViewDTO.builder()
                .id(a.getId())
                .activityCode(a.getActivityCode())
                .name(a.getName())
                .coverUrl(a.getPrizeImgUrl() == null ? "" : a.getPrizeImgUrl())
                .link(a.getActivityCode() == null ? "" : ("#/treasure/" + a.getId()))
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(viewStatus)
                .build();
    }

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

    public void triggerDraw(Long activityId) {
        if (activityId == null) {
            throw new IllegalArgumentException("activityId 必填");
        }
        log.info("管理端手动触发开奖 activityId={}", activityId);
        drawService.draw(activityId);
    }

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
