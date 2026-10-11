package com.gaia.domain.ability.draw.ext;

import com.gaia.domain.draw.DrawWinner;
import com.gaia.domain.member.MemberBenefit;
import com.gaia.iframework.annotation.AbilityExtension;
import com.gaia.iframework.core.IAbilityExt;

import java.util.List;

/**
 * 奖品发放扩展点（{@code collect_draw} 第四步）。
 *
 * <p>{@code CollectDrawAbility.issuePrize} 在按概率抽出 {@link DrawWinner} 列表后，
 * 通过
 * {@link com.gaia.iframework.core.AbstractBaseAbility#reduceExecute(
 *   com.gaia.iframework.core.RequestContext, String,
 *   com.gaia.iframework.core.ExtensionCallback)}
 * 按 {@code (bizCode="draw", extCode="issue_prize")} 取出该扩展并回调。
 * 扩展负责把"中奖者 + 活动"映射为具体应入 {@code memberbenefit} 账本的权益条目；
 * 账本写入与活动明细由 {@code CollectDrawAbility} 在拿到返回结果后统一完成。</p>
 */
@AbilityExtension(code = "issue_prize", name = "开奖奖品发放")
public interface IssuePrizeExt extends IAbilityExt {

    /**
     * 为本次开奖的中奖者生成奖品条目。
     *
     * @param activityId 所属活动 ID
     * @param winners    本次抽签得到的中奖者列表（已按 rank 排序，{@code rank} 从 1 开始）
     * @return 待入库的权益条目；长度可不等于 {@code winners.size()}（如某些 rank 不发奖），
     *         但需保证每条 {@link MemberBenefit#getUserId()} 出现在 {@code winners} 中。
     */
    List<MemberBenefit> issue(Long activityId, List<DrawWinner> winners);
}
