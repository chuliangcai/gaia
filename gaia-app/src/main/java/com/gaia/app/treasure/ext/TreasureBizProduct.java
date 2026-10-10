package com.gaia.app.treasure.ext;

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
 * <p>v1 只提供 {@code "issuance"} 一个扩展点（{@link TreasureCodeIssuanceExt}），
 * 1:1 委托给 {@link TreasureCodeIssuer#issue(Long, Long, int, Long)}；
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
     * 夺宝码发放扩展点实现。
     *
     * <p>{@code (bizCode="treasure", extCode="issuance")} → 本方法。
     * 由 {@code AbstractBaseAbility.reduceExecute(...)} 在 {@link com.gaia.app.treasure.ability.TaskAbility}
     * 中调用。</p>
     */
    @Extension(code = "issuance", name = "夺宝码发放")
    public List<String> issuance(Long userId, Long activityId, int count, Long sourceId) {
        log.debug("TreasureBizProduct.issuance userId={} activityId={} count={} sourceId={}",
                userId, activityId, count, sourceId);
        return treasureCodeIssuer.issue(userId, activityId, count, sourceId);
    }
}