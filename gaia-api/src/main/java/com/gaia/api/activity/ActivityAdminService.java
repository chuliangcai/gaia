package com.gaia.api.activity;

import java.util.List;

/**
 * 活动管理 Dubbo 服务（C 端不可见）。
 *
 * <p>供 gaia-backend 后台管理调用，对活动的全生命周期进行 CRUD 与手动开奖。</p>
 */
public interface ActivityAdminService {

    /**
     * 创建活动，返回活动 ID。
     */
    Long createActivity(ActivityCreateRequest req);

    /**
     * 更新活动（乐观锁）。
     */
    void updateActivity(Long id, ActivityUpdateRequest req);

    /**
     * 配置某活动的任务清单（覆盖式）。
     */
    void configureTasks(Long activityId, List<TaskConfigDTO> tasks);

    /**
     * 强制立即开奖（管理端手动触发）。
     */
    void triggerDraw(Long activityId);

    /**
     * 灌会员测试数据。
     */
    void seedMembers(List<MemberSeedDTO> members);
}