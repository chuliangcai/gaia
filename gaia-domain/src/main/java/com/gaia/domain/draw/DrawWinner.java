package com.gaia.domain.draw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 开奖中奖者值对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawWinner implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    /** 中奖夺宝码（来自用户的夺宝码集合） */
    private String code;

    /** 名次（1-based） */
    private Integer rank;

    /** 该用户夺宝码总数（权重） */
    private Integer weight;
}