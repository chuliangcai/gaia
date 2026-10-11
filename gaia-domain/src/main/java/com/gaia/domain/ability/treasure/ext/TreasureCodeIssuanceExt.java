package com.gaia.domain.ability.treasure.ext;

import com.gaia.iframework.annotation.AbilityExtension;
import com.gaia.iframework.core.IAbilityExt;

import java.util.List;

/**
 * 夺宝码发放扩展点。
 *
 * <p>框架视为玩法内的钩子：{@link com.gaia.iframework.annotation.BizProduct} 类下
 * 通过 {@code @Extension(code = "issuance")} 方法给出该扩展的实现，
 * 由 {@link com.gaia.iframework.core.AbstractBaseAbility#reduceExecute(String, String, com.gaia.iframework.core.ExtensionCallback)}
 * 按 (bizCode, extCode) 取出后回调。</p>
 *
 * <p>v1 唯一实现位于
 * {@link com.gaia.app.treasure.ext.TreasureBizProduct#issuance(Long, Long, int, Long)}，
 * 1:1 委托给 {@code com.gaia.app.treasure.service.TreasureCodeIssuer#issue(...)}。
 * 未来如需替换发放策略（例如 promo 专用路径或测试 fake），只需新增一个
 * {@code @Extension(code = "issuance")} 的方法实现，框架会按
 * {@code @BizProduct.code} + {@code @Extension.code} 严格匹配。</p>
 */
@AbilityExtension(code = "issuance", name = "夺宝码发放")
public interface TreasureCodeIssuanceExt extends IAbilityExt {

    /**
     * 为指定用户发放 {@code count} 个夺宝码（活动内唯一）。
     *
     * @param userId     接收用户
     * @param activityId 所属活动
     * @param count      需要发放的码数（{@code <= 0} 时实现可返回空列表）
     * @param sourceId   来源明细 {@code gaia_activity_record.id}
     * @return 实际入库的夺宝码列表（已去重）
     */
    List<String> issue(Long userId, Long activityId, int count, Long sourceId);
}