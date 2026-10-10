package com.gaia.domain.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动明细实体（事件流 + 幂等键）。
 *
 * <p>每次"用户做了什么 + 拿到什么"都写一条记录，
 * 通过 {@link #actionCode} 唯一约束保证幂等。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long activityId;

    /** 动作类型，见 {@link ActionType} */
    private String actionType;

    /** 幂等键（UK） */
    private String actionCode;

    /** 业务 ID（taskId / activityId 等） */
    private Long bizId;

    /** 入参 JSON */
    private String payload;

    /** 结果 JSON */
    private String result;

    private LocalDateTime createdAt;
}