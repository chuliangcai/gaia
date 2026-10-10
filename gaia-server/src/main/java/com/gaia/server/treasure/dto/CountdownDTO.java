package com.gaia.server.treasure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 倒计时 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountdownDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long activityId;
    private String activityCode;

    /** 0=未开始,1=进行中,2=待开奖,3=已开奖 */
    private Integer status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime serverNow;
    private LocalDateTime drawAt;
}