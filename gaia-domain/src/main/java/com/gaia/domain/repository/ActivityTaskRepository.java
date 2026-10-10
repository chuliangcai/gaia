package com.gaia.domain.repository;

import com.gaia.domain.activity.ActivityTask;

import java.util.List;
import java.util.Optional;

/**
 * 活动任务 Repository。
 */
public interface ActivityTaskRepository {

    Optional<ActivityTask> findById(Long id);

    List<ActivityTask> findByActivityId(Long activityId);

    /**
     * 覆盖式配置某活动的任务清单。
     *
     * @return 受影响行数
     */
    int replaceByActivityId(Long activityId, List<ActivityTask> tasks);
}