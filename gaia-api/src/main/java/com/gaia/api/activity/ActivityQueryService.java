package com.gaia.api.activity;

import java.util.List;

/**
 * 活动查询 Dubbo 服务（gateway 侧消费）。
 *
 * <p>用于支撑移动端"热门活动"列表 —— gateway 不直连 DB，
 * 经此 RPC 拿当前"在线"活动（领域 status ∈ {ONGOING, WAITING_DRAW}）。</p>
 */
public interface ActivityQueryService {

    /**
     * 列出移动端可见的"在线"活动。
     *
     * <p>过滤口径：{@code ActivityRepository#findLive(now)} 内部按
     * {@code status ∈ {1, 2} AND start_time <= now < end_time} 过滤，
     * 按 id 倒序。时间窗与 {@code findStartingAt}（{@code <=}）/
     * {@code findEndingAt}（{@code <=}）互补，三者在边界时刻不冲突；
     * 状态机一刷新把 {@code end_time == now} 的活动推到
     * {@code WAITING_DRAW}，列表仍会通过 {@code status ∈ {1,2}} 把它捞回来。</p>
     *
     * <p>返回的 DTO 中 {@code status} 字段已翻译为 UI 状态（0=草稿/1=已上架/2=已下架），
     * 与 mobile 前端 {@code status === 1} 判断对齐。</p>
     */
    List<ActivitySummaryDTO> listLiveActivities();
}