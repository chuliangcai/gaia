package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务配置 DTO（后台下发）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskCode;
    private String taskName;
    private String taskDesc;
    private Integer rewardCodes;
    private Integer maxExecutions;
    private Integer sortOrder;
    private Integer status;
}
