package com.gaia.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
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

    /** 玩法 code（@BizProduct.code） */
    private String bizCode;

    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 0=未开始,1=进行中,2=待开奖,3=已开奖 */
    private Integer status;

    @Version
    private Integer version;

    /** 能力配置 JSON；key=configClass FQN，value=该类配置对象 */
    private String configurations;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}