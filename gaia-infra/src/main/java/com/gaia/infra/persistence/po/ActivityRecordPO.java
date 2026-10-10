package com.gaia.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动明细 PO。
 */
@Data
@TableName("gaia_activity_record")
public class ActivityRecordPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;

    /** 动作类型：TASK_EXECUTED/DRAW_EXECUTED/DRAW_WON/... */
    private String actionType;

    /** 幂等键（UK） */
    @TableField(value = "`action_code`")
    private String actionCode;

    private Long bizId;

    /** 入参 JSON 字符串 */
    private String payload;

    /** 结果 JSON 字符串 */
    private String result;

    private LocalDateTime createdAt;
}