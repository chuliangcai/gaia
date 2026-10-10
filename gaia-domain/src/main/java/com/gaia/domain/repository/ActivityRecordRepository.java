package com.gaia.domain.repository;

import com.gaia.domain.activity.ActivityRecord;

import java.util.List;
import java.util.Optional;

/**
 * 活动明细 Repository（事件流 + 幂等键）。
 */
public interface ActivityRecordRepository {

    /**
     * 幂等写入。返回 true 表示新插入；false 表示 actionCode 已存在。
     */
    boolean insertIfAbsent(ActivityRecord record);

    Optional<ActivityRecord> findByActionCode(String actionCode);

    List<ActivityRecord> findByUserAndActivity(Long userId, Long activityId);

    List<ActivityRecord> findByActivityAndType(Long activityId, String actionType);

    /**
     * 统计某用户某活动某任务在某段时间窗口内执行次数。
     */
    long countByUserTask(Long userId, Long activityId, Long taskId);
}