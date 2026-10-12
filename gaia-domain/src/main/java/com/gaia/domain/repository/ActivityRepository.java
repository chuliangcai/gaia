package com.gaia.domain.repository;

import com.gaia.domain.base.activity.Activity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 活动 Repository。
 */
public interface ActivityRepository {

    Optional<Activity> findById(Long id);

    /**
     * 全量列表（后台管理用，按 id 倒序）。
     */
    List<Activity> findAll();

    /**
     * 物理删除（后台管理用）。
     */
    void deleteById(Long id);

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
     * 移动端"在线"活动：领域 status ∈ {ONGOING, WAITING_DRAW} 且落在时间窗内，按 id 倒序。
     *
     * <p>过滤口径：{@code start_time <= now AND end_time > now}。
     * 与 {@link #findStartingAt}（{@code start_time <= now}）和
     * {@link #findEndingAt}（{@code end_time <= now}）互补，三者在边界时刻不冲突。
     * {@code ActivityStatusRefresher} 一旦把 {@code end_time == now} 的活动推到
     * {@code WAITING_DRAW}（{@code status=2}），{@code findLive} 仍会通过
     * {@code status IN (1,2)} 把它捞回来，对用户保持可见直至被手动下架。</p>
     */
    List<Activity> findLive(LocalDateTime now);

    /**
     * 保存（含乐观锁 version）。新增场景 id 会被回填。
     */
    Activity save(Activity activity);
}