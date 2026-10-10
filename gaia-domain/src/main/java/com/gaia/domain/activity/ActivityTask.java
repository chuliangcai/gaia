package com.gaia.domain.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动任务配置实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private String taskCode;
    private String taskName;
    private String taskDesc;

    /** 每执行一次发放几个夺宝码 */
    private Integer rewardCodes;

    /** 单用户最多执行次数 */
    private Integer maxExecutions;

    private Integer sortOrder;

    /** 0=禁用,1=启用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /**
     * 用户当前已执行 {@code userDoneCount} 次，是否还可以继续执行。
     */
    public boolean isAvailableFor(int userDoneCount) {
        return isEnabled()
                && maxExecutions != null
                && userDoneCount < maxExecutions;
    }
}