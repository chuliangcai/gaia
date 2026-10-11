package com.gaia.domain.repository;

import com.gaia.domain.draw.DrawCandidate;
import com.gaia.domain.member.MemberBenefit;

import java.util.List;

/**
 * 会员权益 Repository（含夺宝码账本）。
 */
public interface MemberBenefitRepository {

    /**
     * 批量插入权益（类型无关：LOTTERY_CODE / PRIZE / 未来其他类型共用）。
     *
     * <p>UK 冲突由 DB 抛出 {@link org.springframework.dao.DuplicateKeyException}，
     * Repository 不吞异常——是否重试由调用方按业务决定。</p>
     *
     * @return 实际插入条数
     */
    int insertBenefits(List<MemberBenefit> benefits);

    /**
     * 查询某用户在某活动的夺宝码。
     */
    List<String> listLotteryCodesByUserAndActivity(Long userId, Long activityId);

    /**
     * 统计活动所有夺宝码，按用户聚合（候选权重）。
     */
    List<DrawCandidate> countLotteryCodesByActivity(Long activityId);

    /**
     * 查询某用户的所有权益。
     */
    List<MemberBenefit> listByUserId(Long userId);
}