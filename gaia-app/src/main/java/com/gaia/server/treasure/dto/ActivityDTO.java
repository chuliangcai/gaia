package com.gaia.server.treasure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动基本信息（活动页奖品区）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String activityCode;
    private String name;
    private String prizeDesc;
    private String prizeImgUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;

    /** 0=未开始,1=进行中,2=待开奖,3=已开奖 */
    private String statusDesc;
    private Integer winnerCount;
    private LocalDateTime drawAt;
}