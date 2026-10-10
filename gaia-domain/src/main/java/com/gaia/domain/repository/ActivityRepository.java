package com.gaia.domain.repository;

import com.gaia.domain.activity.Activity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 活动 Repository。
 */
public interface ActivityRepository {

    Optional<Activity> findById(Long id);

    Optional<Activity> findByCode(String activityCode);

    /**
     * 查找应推进为"进行中"的活动：status=0 且 start_time <= now。
     */
    List<Activity> findStartingAt(LocalDateTime now);

    /**
     * 查找应推进为"待开奖"的活动：status=1 且 end_time <= now。
     */
    List<Activity> findEndingAt(LocalDateTime now);

    /**
     * 查找已开奖活动。
     */
    List<Activity> findDrawn();

    /**
     * 保存（含乐观锁 version）。新增场景 id 会被回填。
     */
    Activity save(Activity activity);
}