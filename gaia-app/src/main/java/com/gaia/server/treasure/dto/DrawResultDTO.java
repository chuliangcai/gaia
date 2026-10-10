package com.gaia.server.treasure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 开奖结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Integer status;
    private LocalDateTime drawAt;
    private List<DrawWinnerDTO> winners;
}