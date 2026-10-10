package com.gaia.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员权益 PO。
 *
 * <p>对应表 {@code gaia_member_benefit} 上的生成列
 * {@code uk_lottery_code} 在 PO 中不映射（由 DB 自动维护）。</p>
 */
@Data
@TableName("gaia_member_benefit")
public class MemberBenefitPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String benefitType;
    private String benefitValue;
    private Long activityId;
    private Long sourceId;

    /** 0=失效,1=有效 */
    private Integer status;

    private LocalDateTime grantedAt;
    private LocalDateTime expireAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}