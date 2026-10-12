package com.gaia.api.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动摘要（移动端"热门活动"列表用）。
 *
 * <p>字段与 {@code frontend-mobile/src/api/activities.js} 的契约对齐：
 * {@code status} 是 UI 状态（0=草稿 1=已上架 2=已下架），由
 * {@code ActivityQueryServiceImpl} 把领域 status 0/1/2/3 翻译过来，
 * 移动端 {@code status === 1} 判断"进行中"角标才不会隐性失效。</p>
 *
 * <p>仅透出列表所需的最小字段；详情页需要的 {@code configurations} 不在此处，
 * 避免重 JSON 拖慢首屏 deserialization。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动主键 */
    private Long id;

    /** 玩法 code（{@code @BizProduct.code}） */
    private String bizCode;

    /** 活动名称 */
    private String name;

    /**
     * 移动端 hash 跳转路径：{@code /treasure/<id>}。
     *
     * <p>不带 {@code #} 前缀，由 mobile 前端
     * {@code navigate(activity.link.replace(/^#/, ''))} 直接 hash 路由。</p>
     */
    private String link;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** UI 状态：0=草稿 1=已上架 2=已下架 */
    private Integer status;
}