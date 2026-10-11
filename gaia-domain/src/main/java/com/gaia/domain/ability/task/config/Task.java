package com.gaia.domain.ability.task.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 单条任务配置项。
 *
 * <p>取代 {@code gaia_activity_task} 表的一行；状态 0=禁用,1=启用，
 * {@code maxExecutions} 为单用户最大可执行次数，
 * {@code rewardCodes} 为每执行一次发放的夺宝码数。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务记录 id（保留为配置项自身的稳定 id，框架不强制唯一）。 */
    private Long id;

    private String taskCode;
    private String taskName;
    private String taskDesc;

    /** 每执行一次发放几个夺宝码 */
    private Integer rewardCodes;

    /** 单用户最多执行次数 */
    private Integer maxExecutions;

    /** 展示顺序 */
    private Integer sortOrder;

    /** 0=禁用,1=启用 */
    private Integer status;

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
