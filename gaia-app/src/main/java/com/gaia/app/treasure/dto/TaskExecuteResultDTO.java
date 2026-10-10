package com.gaia.app.treasure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务执行结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecuteResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 本次新增的夺宝码 */
    private List<String> newCodes;

    /** 该用户在本活动的夺宝码总数 */
    private Integer totalCodes;

    /** 任务累计已执行次数 */
    private Integer executedCount;

    /** 幂等命中：true 表示本次是重复请求 */
    private Boolean idempotent;
}