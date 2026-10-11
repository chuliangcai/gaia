package com.gaia.domain.repository;

import com.gaia.domain.base.activity.ActivityRecord;

import java.time.LocalDateTime;
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

    /**
     * 统计某用户某活动某任务在 {@code [dayStart, dayEnd)} 时间窗口内的
     * TASK_EXECUTED 明细条数。{@code dayStart} 含，{@code dayEnd} 不含，
     * 由调用方负责保证该窗口的合法性（如 {@code dayEnd = dayStart.plusDays(1)}）。
     */
    long countByUserTaskOnDay(Long userId, Long activityId, Long taskId,
                              LocalDateTime dayStart, LocalDateTime dayEnd);
}