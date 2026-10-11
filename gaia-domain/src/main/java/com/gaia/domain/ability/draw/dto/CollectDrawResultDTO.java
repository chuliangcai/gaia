package com.gaia.domain.ability.draw.dto;

import com.gaia.domain.draw.DrawWinner;
import com.gaia.domain.member.MemberBenefit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 集奖开奖（{@code collect_draw}）结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectDrawResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long activityId;
    /** 活动推进后的状态；{@code collect_draw} 成功后固定为 3（DRAWN）。 */
    private Integer status;
    private LocalDateTime drawAt;
    /** 抽签得到的中奖者列表（按 rank 升序；可能为空）。 */
    private List<DrawWinner> winners;
    /** 实际入库的奖品条目（{@code MemberBenefit} 形态，由 {@code issue_prize} 扩展返回并已写入账本）。 */
    private List<MemberBenefit> prizes;
}
