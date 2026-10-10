package com.gaia.server.service;

import org.springframework.stereotype.Component;

/**
 * 签到奖励规则。
 *
 * <p>默认阶梯：基础 10 积分 + 连签天数叠加，单次上限 100。</p>
 */
@Component
public class SignRewardRule {

    private static final long BASE_REWARD = 10L;
    private static final long STREAK_BONUS_PER_DAY = 5L;
    private static final long MAX_REWARD = 100L;

    /**
     * 计算奖励积分。
     *
     * @param streakDays 累计连续签到天数（包含本次）
     */
    public long calculate(int streakDays) {
        long reward = BASE_REWARD + STREAK_BONUS_PER_DAY * Math.max(0, streakDays - 1);
        return Math.min(reward, MAX_REWARD);
    }
}
