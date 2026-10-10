package com.gaia.app.sign;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 签到结果。
 *
 * <p>作为 {@code IFResult.model} 的载荷在 RPC 上透传。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否首次签到成功 */
    private boolean success;
    /** 累计连续签到天数 */
    private int streakDays;
    /** 当次奖励积分 */
    private long rewardPoints;
    /** 签到日期 yyyy-MM-dd */
    private String signDate;
}
