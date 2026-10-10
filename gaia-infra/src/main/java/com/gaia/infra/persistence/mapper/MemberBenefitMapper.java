package com.gaia.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gaia.infra.persistence.po.MemberBenefitPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会员权益 Mapper。
 */
@Mapper
public interface MemberBenefitMapper extends BaseMapper<MemberBenefitPO> {

    /**
     * 按活动统计每个用户的夺宝码数量（候选权重）。
     */
    @Select("SELECT user_id AS userId, COUNT(*) AS weight "
            + "FROM gaia_member_benefit "
            + "WHERE activity_id = #{activityId} AND benefit_type = 'LOTTERY_CODE' AND status = 1 "
            + "GROUP BY user_id")
    List<WeightRow> countLotteryCodesByActivity(@Param("activityId") Long activityId);

    /**
     * 权重结果行映射。
     */
    class WeightRow {
        private Long userId;
        private Long weight;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getWeight() {
            return weight;
        }

        public void setWeight(Long weight) {
            this.weight = weight;
        }
    }
}