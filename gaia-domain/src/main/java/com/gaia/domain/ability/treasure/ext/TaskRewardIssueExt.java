package com.gaia.domain.ability.treasure.ext;

import com.gaia.domain.ability.task.dto.RewardIssueResult;
import com.gaia.iframework.annotation.AbilityExtension;
import com.gaia.iframework.core.IAbilityExt;

import java.util.List;

/**
 * 任务奖励发放扩展点。
 *
 * <p>框架视为玩法内的钩子：{@link com.gaia.iframework.annotation.BizProduct} 类下
 * 通过 {@code @Extension(code = "issuance")} 方法给出该扩展的实现，
 * 由 {@link com.gaia.iframework.core.AbstractBaseAbility#reduceExecute(String, String, com.gaia.iframework.core.ExtensionCallback)}
 * 按 (bizCode, extCode) 取出后回调。</p>
 *
 * <p>扩展点与具体玩法解耦：返回的 {@link RewardIssueResult} 只描述
 * "发了什么权益 + 在哪个活动可用"，由各玩法自行填充。
 * 会员夺宝场景下，{@code benefitContent} 即夺宝码，
 * {@code availableActivityId} 即活动 ID；
 * 其他玩法可发奖品、优惠券等，对应填充即可。</p>
 */
@AbilityExtension(code = "issuance", name = "任务奖励发放")
public interface TaskRewardIssueExt extends IAbilityExt {

    /**
     * 为指定用户发放任务奖励。
     *
     * @param userId     接收用户
     * @param activityId 所属活动
     * @param count      本次希望发放的权益条数（{@code <= 0} 时实现可返回空列表）
     * @param sourceId   来源明细 {@code gaia_activity_record.id}
     * @return 实际入库的权益结果列表（已去重；顺序无要求）
     */
    List<RewardIssueResult> issue(Long userId, Long activityId, int count, Long sourceId);
}