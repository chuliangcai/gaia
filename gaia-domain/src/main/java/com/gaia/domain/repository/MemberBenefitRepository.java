package com.gaia.domain.repository;

import com.gaia.domain.draw.DrawCandidate;
import com.gaia.domain.member.MemberBenefit;

import java.util.List;

/**
 * 会员权益 Repository（含夺宝码账本）。
 */
public interface MemberBenefitRepository {

    /**
     * 批量插入夺宝码权益。依赖数据库生成列 UK 保证活动内唯一。
     *
     * @return 实际插入条数
     */
    int insertLotteryCodes(List<MemberBenefit> benefits);

    /**
     * 批量插入中奖奖品权益。
     */
    int insertPrizes(List<MemberBenefit> prizes);

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