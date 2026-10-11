package com.gaia.domain.ability.draw.ext;

import com.gaia.iframework.annotation.AbilityExtension;
import com.gaia.iframework.core.IAbilityExt;

import java.util.Map;

/**
 * 计算参与用户中奖概率的扩展点（{@code collect_draw} 第二步）。
 *
 * <p>{@code CollectDrawAbility.calculateProbabilities} 在拿到候选池（夺宝码账本）
 * 之后通过
 * {@link com.gaia.iframework.core.AbstractBaseAbility#reduceExecute(
 *   com.gaia.iframework.core.RequestContext, String,
 *   com.gaia.iframework.core.ExtensionCallback)}
 * 按 {@code (bizCode="draw", extCode="calc_probability")} 取出该扩展并回调。
 * 不同玩法可替换为不同概率策略（如夺宝码数量加权、活跃度加成等）。</p>
 */
@AbilityExtension(code = "calc_probability", name = "计算参与用户概率")
public interface CalculateProbabilityExt extends IAbilityExt {

    /**
     * 为本次开奖的候选用户计算概率。
     *
     * @param activityId 所属活动 ID
     * @return {@code userId → probability}；返回空 map 视为无人参与，
     *         {@code CollectDrawAbility} 会跳过抽奖直接结束。允许概率之和不严格等于 1，
     *         内部会按总和归一化用作加权权重。
     */
    Map<Long, Double> calculate(Long activityId);
}
