package com.gaia.domain.ability.draw.config;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集奖开奖（{@code collect_draw}）能力配置。
 *
 * <p>序列化为活动记录的 {@code configurations} JSON 字段（key 为本类 FQN），
 * 运行时由 {@link com.gaia.domain.ability.draw.CollectDrawAbility} 反序列化读取。
 * 玩法（{@code @BizProduct(code = "draw")}）无差异化时也使用同一份配置。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("集奖开奖配置")
public class CollectDrawConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 开奖时间；{@code preCheck} 校验当前时间是否已到达该时刻，未到则短路返回 DRAW_NOT_DUE。 */
    @JsonProperty(required = true)
    @JsonPropertyDescription("开奖时间")
    private LocalDateTime drawTime;

    /** 本次开奖的奖品总个数；{@code <= 0} 时 {@code draw} 步视作"无人中奖"直接结束。 */
    @JsonProperty(required = true)
    @JsonPropertyDescription("奖品总数")
    private Integer totalPrizeCount;

    /** 每位用户最多可中得的奖品个数；{@code null} 时视为"不限制单人中奖上限"。 */
    @JsonPropertyDescription("单人中奖上限")
    private Integer perUserPrizeLimit;
}
