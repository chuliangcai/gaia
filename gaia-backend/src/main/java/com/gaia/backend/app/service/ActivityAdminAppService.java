package com.gaia.backend.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia.backend.app.dto.ActivityCreateRequest;
import com.gaia.backend.app.dto.ActivityUpdateRequest;
import com.gaia.backend.app.dto.ActivityViewDTO;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.service.treasure.DrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 活动管理后台应用服务。
 *
 * <p>直接编排 gaia-domain 仓储与领域服务（DrawService），不再走 Dubbo。
 * 与早期 {@code com.gaia.app.service.impl.ActivityAdminServiceImpl}（已删除并被本类替换）的
 * 行为保持兼容；本版本不再单独管理 {@code gaia_activity_task} 表，
 * 任务清单走活动行的 {@code configurations} JSON（key=configClass FQN）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAdminAppService {

    private final ActivityRepository activityRepository;
    private final DrawService drawService;
    private final ObjectMapper objectMapper;

    public Long createActivity(ActivityCreateRequest req) {
        if (req == null || req.getActivityCode() == null) {
            throw new IllegalArgumentException("activityCode 必填");
        }
        if (req.getBizCode() == null || req.getBizCode().isBlank()) {
            throw new IllegalArgumentException("bizCode 必填");
        }
        Activity a = Activity.builder()
                .activityCode(req.getActivityCode())
                .bizCode(req.getBizCode())
                .name(req.getName())
                .prizeDesc(req.getPrizeDesc() == null ? "" : req.getPrizeDesc())
                .prizeImgUrl(req.getPrizeImgUrl() == null ? "" : req.getPrizeImgUrl())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(0)
                .winnerCount(req.getWinnerCount() == null ? 1 : req.getWinnerCount())
                .configurations(serializeConfig(req.getConfigurations()))
                .build();
        Activity saved = activityRepository.save(a);
        log.info("创建活动 id={} code={} bizCode={}", saved.getId(), saved.getActivityCode(), saved.getBizCode());
        return saved.getId();
    }

    public void updateActivity(Long id, ActivityUpdateRequest req) {
        if (id == null || req == null) {
            throw new IllegalArgumentException("id 与 req 必填");
        }
        Activity a = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("活动不存在 id=" + id));
        if (req.getName() != null)        a.setName(req.getName());
        if (req.getPrizeDesc() != null)   a.setPrizeDesc(req.getPrizeDesc());
        if (req.getPrizeImgUrl() != null) a.setPrizeImgUrl(req.getPrizeImgUrl());
        if (req.getStartTime() != null)   a.setStartTime(req.getStartTime());
        if (req.getEndTime() != null)     a.setEndTime(req.getEndTime());
        if (req.getWinnerCount() != null) a.setWinnerCount(req.getWinnerCount());
        if (req.getBizCode() != null && !req.getBizCode().isBlank()) a.setBizCode(req.getBizCode());
        if (req.getConfigurations() != null) {
            a.setConfigurations(serializeConfig(req.getConfigurations()));
        }
        activityRepository.save(a);
        log.info("更新活动 id={}", id);
    }

    public List<ActivityViewDTO> listActivities() {
        return activityRepository.findAll().stream()
                .map(this::toView)
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
     * 领域 Activity → 后台列表 DTO。状态机映射：0=草稿 1=上架 2=下架。
     */
    ActivityViewDTO toView(Activity a) {
        Integer viewStatus;
        if (a.getStatus() == null) {
            viewStatus = 0;
        } else {
            switch (a.getStatus()) {
                case 0:  viewStatus = 0; break; // 未开始 → 草稿
                case 1:
                case 2:  viewStatus = 1; break; // 进行中 / 待开奖 → 上架
                default: viewStatus = 2;        // 已开奖 → 下架
            }
        }
        return ActivityViewDTO.builder()
                .id(a.getId())
                .activityCode(a.getActivityCode())
                .bizCode(a.getBizCode())
                .name(a.getName())
                .coverUrl(a.getPrizeImgUrl() == null ? "" : a.getPrizeImgUrl())
                .link(a.getActivityCode() == null ? "" : ("#/treasure/" + a.getId()))
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(viewStatus)
                .configurations(parseConfig(a.getConfigurations()))
                .build();
    }

    public void triggerDraw(Long activityId) {
        if (activityId == null) {
            throw new IllegalArgumentException("activityId 必填");
        }
        log.info("管理端手动触发开奖 activityId={}", activityId);
        drawService.draw(activityId);
    }

    // ---------- configurations JSON 序列化 ----------

    private String serializeConfig(Map<String, Object> configurations) {
        if (configurations == null || configurations.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(configurations);
        } catch (Exception e) {
            throw new IllegalStateException("configurations 序列化失败", e);
        }
    }

    private Map<String, Object> parseConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("configurations 反序列化失败", e);
            return Collections.emptyMap();
        }
    }
}
