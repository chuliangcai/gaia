package com.gaia.server.treasure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 中奖者信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawWinnerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String code;
    private Integer rank;
    private Integer weight;
}