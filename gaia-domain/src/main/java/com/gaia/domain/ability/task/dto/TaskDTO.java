package com.gaia.domain.ability.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String taskName;
    private String taskDesc;
    private Integer rewardCount;
    private Integer maxExecutions;
    private Integer executedCount;

    /** 当前用户是否还可执行 */
    private Boolean available;
    private Integer sortOrder;
}
