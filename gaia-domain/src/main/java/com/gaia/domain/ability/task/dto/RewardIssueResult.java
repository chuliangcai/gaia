package com.gaia.domain.ability.task.dto;

import com.gaia.domain.member.BenefitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务奖励发放扩展点的单条结果。
 *
 * <p>{@link com.gaia.domain.ability.task.ext.TaskRewardIssueExt#issue} 返回
 * {@code List<RewardIssueResult>}，每条记录描述一次发放产生的单条权益：</p>
 * <ul>
 *   <li>{@link #benefitType} — 权益类型（夺宝码 / 中奖奖品 / 优惠券等）。</li>
 *   <li>{@link #benefitContent} — 权益内容：对应类型的具体值（码 / 奖品标识 / 券码等）；
 *   具体形态由玩法实现决定。</li>
 *   <li>{@link #availableActivityId} — 该权益可用的活动 ID；
 *   为 {@code null} 时表示该权益不限定活动（如全局券）。</li>
 * </ul>
 *
 * <p>从 {@link com.gaia.domain.member.MemberBenefit} 的视角看，
 * {@code benefitContent} 对应 {@code benefitValue}，
 * {@code availableActivityId} 对应 {@code effectiveActivityId}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardIssueResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权益类型 */
    private BenefitType benefitType;

    /** 权益内容（码 / 奖品标识 / 优惠券码等） */
    private String benefitContent;

    /** 可用活动 ID；{@code null} 表示不限定活动 */
    private Long availableActivityId;
}