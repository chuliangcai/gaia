package com.gaia.app.treasure.ext;

import com.gaia.domain.ability.task.dto.RewardIssueResult;
import com.gaia.domain.member.BenefitType;
import com.gaia.iframework.annotation.BizProduct;
import com.gaia.iframework.annotation.Extension;
import com.gaia.app.treasure.service.TreasureCodeIssuer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会员夺宝 玩法（{@code bizCode = "treasure"}）。
 *
 * <p>本类是全仓库唯一一个被
 * {@link com.gaia.app.framework.ExtensionRegistry#init()} 索引的 {@code @BizProduct} bean，
 * 承载该玩法下所有 {@code @Extension} 扩展点实现。</p>
 *
 * <p>v1 只提供 {@code "issuance"} 一个扩展点（{@link com.gaia.domain.ability.treasure.ext.TaskRewardIssueExt}），
 * 1:1 委托给 {@link TreasureCodeIssuer#issue(int)}（纯生成候选码，不写账本）；
 * 账本写入由 {@link com.gaia.domain.ability.task.TaskExecuteAbility#issueReward}
 * 在拿到本方法返回的 {@code List<RewardIssueResult>} 后统一完成。
 * 其他玩法内的关注点（如 draw 算法）当前仍由 service / scheduler 直接调用，
 * 待出现真实替代实现需求时再行抽象为扩展点。</p>
 *
 * <p>本类<strong>不是</strong> {@code @Ability}，<strong>不实现</strong>
 * {@link com.gaia.iframework.core.IAbility}，也<strong>不继承</strong>
 * {@link com.gaia.iframework.core.AbstractBaseAbility}——它只为扩展点机制服务，
 * 由 {@code AbilityDispatcher} 干净跳过。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@BizProduct(code = "treasure", name = "会员夺宝")
public class TreasureBizProduct {

    private final TreasureCodeIssuer treasureCodeIssuer;

    /**
     * 任务奖励发放扩展点实现（夺宝玩法）。
     *
     * <p>{@code (bizCode="treasure", extCode="issuance")} → 本方法。
     * 由 {@code AbstractBaseAbility.reduceExecute(...)} 在
     * {@link com.gaia.domain.ability.task.TaskExecuteAbility} 中调用。</p>
     *
     * <p>本玩法下每次发放的 {@link RewardIssueResult#benefitContent} 即夺宝码，
     * {@link RewardIssueResult#benefitType} 为 {@link BenefitType#LOTTERY_CODE}，
     * {@link RewardIssueResult#availableActivityId} 与活动 ID 一致
     * （夺宝码仅在所属活动内生效）。</p>
     */
    @Extension(code = "issuance", name = "任务奖励发放")
    public List<RewardIssueResult> issuance(Long userId, Long activityId, int count, Long sourceId) {
        log.debug("TreasureBizProduct.issuance userId={} activityId={} count={} sourceId={}",
                userId, activityId, count, sourceId);
        // 只生成候选码；账本写入（sourceId 已透传给 issueReward）由领域层统一负责
        List<String> codes = treasureCodeIssuer.issue(count);
        List<RewardIssueResult> out = new java.util.ArrayList<>(codes.size());
        for (String code : codes) {
            out.add(RewardIssueResult.builder()
                    .benefitType(BenefitType.LOTTERY_CODE)
                    .benefitContent(code)
                    .availableActivityId(activityId)
                    .build());
        }
        return out;
    }
}