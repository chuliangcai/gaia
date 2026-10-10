package com.gaia.domain.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员权益实体（含夺宝码账本）。
 *
 * <p>{@code benefitType} 区分：</p>
 * <ul>
 *   <li>{@code LOTTERY_CODE} — 夺宝码，{@code benefitValue} 即 6 位大写字母</li>
 *   <li>{@code PRIZE} — 中奖奖品</li>
 * </ul>
 *
 * <p>夺宝码活动内唯一由数据库生成列 {@code uk_lottery_code} 保证。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberBenefit implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String benefitType;
    private String benefitValue;
    private Long activityId;

    /** 来源明细 gaia_activity_record.id（每次权益发放必对应一条明细） */
    private Long sourceId;

    /** 0=失效,1=有效 */
    private Integer status;

    private LocalDateTime grantedAt;
    private LocalDateTime expireAt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isLotteryCode() {
        return BenefitType.LOTTERY_CODE.name().equals(benefitType);
    }

    public boolean isPrize() {
        return BenefitType.PRIZE.name().equals(benefitType);
    }

    public boolean isActive() {
        return status != null && status == 1;
    }
}