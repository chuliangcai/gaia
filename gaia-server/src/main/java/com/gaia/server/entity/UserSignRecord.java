package com.gaia.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户签到记录。
 *
 * <p>BitMap 是热路径，DB 表用于持久化对账与重置恢复。</p>
 */
@Data
@TableName("user_sign_record")
public class UserSignRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 签到日期 yyyy-MM-dd */
    private String signDate;

    /** 年月 yyyyMM, MySQL 8 中 YEAR_MONTH 是关键字, 需反引号 */
    @TableField(value = "`year_month`")
    private String yearMonth;

    /** 当次奖励积分 */
    private Long rewardPoints;

    /** 连续天数 */
    private Integer streakDays;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
