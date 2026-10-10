package com.gaia.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动表 PO。
 */
@Data
@TableName("gaia_activity")
public class ActivityPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String activityCode;
    private String name;
    private String prizeDesc;
    private String prizeImgUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 0=未开始,1=进行中,2=待开奖,3=已开奖 */
    private Integer status;

    private Integer winnerCount;
    private LocalDateTime drawAt;

    @Version
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}