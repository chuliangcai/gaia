package com.gaia.server.treasure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 活动页聚合数据（C 端首屏）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private ActivityDTO activity;

    /** 用户在该活动的所有夺宝码 */
    private List<String> myCodes;

    /** 任务清单（含状态） */
    private List<TaskDTO> tasks;

    /** 用户总夺宝码数（冗余字段，便于前端展示） */
    private Integer totalCodes;
}