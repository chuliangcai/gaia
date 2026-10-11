package com.gaia.domain.ability.task.config;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
 * {@code rewardCount} 为每执行一次发放的夺宝码数。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("任务项")
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务记录 id（保留为配置项自身的稳定 id，框架不强制唯一）。 */
    @JsonPropertyDescription("任务 ID")
    private Long id;

    @JsonPropertyDescription("任务名称")
    private String taskName;

    @JsonPropertyDescription("任务说明")
    private String taskDesc;

    /** 每执行一次发放几个夺宝码 */
    @JsonPropertyDescription("单次奖励数")
    private Integer rewardCount;

    /** 单用户最多执行次数 */
    @JsonPropertyDescription("单人最大次数")
    private Integer maxExecutions;

    /** 展示顺序 */
    @JsonPropertyDescription("排序")
    private Integer sortOrder;

    /** 0=禁用,1=启用 */
    @JsonPropertyDescription("启用状态")
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
