package com.gaia.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动任务 PO。
 */
@Data
@TableName("gaia_activity_task")
public class ActivityTaskPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private String taskCode;
    private String taskName;
    private String taskDesc;
    private Integer rewardCodes;
    private Integer maxExecutions;
    private Integer sortOrder;

    /** 0=禁用,1=启用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}