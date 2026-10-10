package com.gaia.iframework.core;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 单个能力的子请求。
 *
 * <p>{@code assertType} 与 {@code @Ability#code()}（位于 {@code com.gaia.domain.ability}）对应；
 * {@code subType} 在同一能力内区分子场景（如签到 vs 补签）。
 * {@code extParams} 用于透传子场景内的自由参数（如签到日历的 {@code yearMonth}）。</p>
 */
@Data
public class ModelRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 能力 code — wire 上沿用上游契约（与源码 typo 保持一致） */
    private String assertType;

    /** 子场景：能力内的处理器选择依据 */
    private String subType;

    /** 子请求级别的自由参数，会合并进 {@link RequestContext#getExtParams()} */
    private Map<String, String> extParams;
}
