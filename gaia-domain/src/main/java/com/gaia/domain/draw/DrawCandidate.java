package com.gaia.domain.draw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 开奖候选人值对象。
 *
 * <p>用于 {@link com.gaia.domain.service.treasure.WeightedDrawAlgorithm} 输入。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawCandidate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    /** 权重 = 用户夺宝码数量 */
    private Integer weight;
}