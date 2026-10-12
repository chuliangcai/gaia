package com.gaia.app.bizproduct.treasure;

import com.gaia.domain.ability.draw.ext.CalculateProbabilityExt;
import com.gaia.domain.ability.draw.ext.IssuePrizeExt;
import com.gaia.domain.ability.task.dto.RewardIssueResult;
import com.gaia.domain.ability.treasure.ext.TaskRewardIssueExt;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.draw.DrawCandidate;
import com.gaia.domain.draw.DrawWinner;
import com.gaia.domain.member.BenefitType;
import com.gaia.domain.member.MemberBenefit;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.iframework.annotation.BizProduct;
import com.gaia.iframework.annotation.Extension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员夺宝 玩法（{@code bizCode = "treasure"}）。
 *
 * <p>本类是全仓库唯一一个被
 * {@link com.gaia.app.framework.ExtensionRegistry#init()} 索引的 {@code @BizProduct} bean，
 * 承载该玩法下所有 {@code @Extension} 扩展点实现。</p>
 *
 * <p>v1 聚合了三个扩展点：
 * <ul>
 *   <li>{@code "issuance"} — {@link com.gaia.domain.ability.task.TaskExecuteAbility#issueReward}
 *       按 {@code (bizCode="treasure", extCode="issuance")} 取出，1:1 委托给
 *       {@link TreasureCodeIssuer#issue(int)}（纯生成候选码，不写账本）；
 *       账本写入由 {@code TaskExecuteAbility} 在拿到本方法返回的
 *       {@code List<RewardIssueResult>} 后统一完成。</li>
 *   <li>{@code "calc_probability"} — {@link com.gaia.domain.ability.draw.CollectDrawAbility#calculateProbabilities}
 *       按 {@code (bizCode="treasure", extCode="calc_probability")} 取出，
 *       实现 {@link CalculateProbabilityExt}：每个用户的概率 ∝ 其在该活动下的夺宝码数量。
 *       {@code CollectDrawAbility.calculateProbabilities} 会按总和归一化再用作加权权重。</li>
 *   <li>{@code "issue_prize"} — {@code CollectDrawAbility.issuePrize}
 *       按 {@code (bizCode="treasure", extCode="issue_prize")} 取出，
 *       实现 {@link IssuePrizeExt}：为每个中奖者生成一条
 *       {@link BenefitType#PRIZE} 类型的 {@link MemberBenefit} 奖品条目，
 *       {@code benefitValue} 形如 {@code "<活动名> - <rank>"}；
 *       账本写入由 {@code CollectDrawAbility.issuePrize} 在拿到本方法返回结果后统一完成。</li>
 * </ul>
 * 玩法内其他关注点（如 draw 算法调度）当前仍由 service / scheduler 直接调用，
 * 待出现真实替代实现需求时再行抽象为扩展点。</p>
 *
 * <p>直接 {@code implements} 扩展接口（而非仅暴露同名方法）使得
 * {@code AbstractBaseAbility.reduceExecute} 拿到的 bean 真正满足
 * {@code (ExtType) ext} 类型转换，避免 {@code ClassCastException}。</p>
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
public class TreasureBizProduct implements CalculateProbabilityExt, IssuePrizeExt, TaskRewardIssueExt {

    private final TreasureCodeIssuer treasureCodeIssuer;
    private final MemberBenefitRepository memberBenefitRepository;
    private final ActivityRepository activityRepository;

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
    @Override
    public List<RewardIssueResult> issue(Long userId, Long activityId, int count, Long sourceId) {
        log.debug("TreasureBizProduct.issue userId={} activityId={} count={} sourceId={}",
                userId, activityId, count, sourceId);
        // 只生成候选码；账本写入（sourceId 已透传给 issueReward）由领域层统一负责
        List<String> codes = treasureCodeIssuer.issue(count);
        List<RewardIssueResult> out = new ArrayList<>(codes.size());
        for (String code : codes) {
            out.add(RewardIssueResult.builder()
                    .benefitType(BenefitType.LOTTERY_CODE)
                    .benefitContent(code)
                    .availableActivityId(activityId)
                    .build());
        }
        return out;
    }

    /**
     * 计算参与用户概率（{@code calc_probability} 扩展点）。
     *
     * <p>v1 策略：每个用户的概率 ∝ 其在该活动下的夺宝码数量。
     * 此处仅返回"原始权重"，{@code CollectDrawAbility.calculateProbabilities}
     * 会按总和归一化再用作加权权重。</p>
     */
    @Extension(code = "calc_probability", name = "计算参与用户概率")
    @Override
    public Map<Long, Double> calculate(Long activityId) {
        log.debug("TreasureBizProduct.calculate activityId={}", activityId);
        List<DrawCandidate> cands = memberBenefitRepository.countLotteryCodesByActivity(activityId);
        Map<Long, Double> weights = new HashMap<>(cands.size());
        for (DrawCandidate c : cands) {
            if (c.getUserId() == null || c.getWeight() == null || c.getWeight() <= 0) {
                continue;
            }
            weights.put(c.getUserId(), (double) c.getWeight());
        }
        return weights;
    }

    /**
     * 发放奖品（{@code issue_prize} 扩展点）。
     *
     * <p>为每个中奖者生成一条 {@link BenefitType#PRIZE} 的权益条目。
     * 账本写入由 {@code CollectDrawAbility.issuePrize} 在拿到本方法返回结果后
     * 统一完成；此处仅构造对象。</p>
     */
    @Extension(code = "issue_prize", name = "开奖奖品发放")
    @Override
    public List<MemberBenefit> issue(Long activityId, List<DrawWinner> winners) {
        log.debug("TreasureBizProduct.issue activityId={} winners={}",
                activityId, winners == null ? 0 : winners.size());
        if (winners == null || winners.isEmpty()) {
            return List.of();
        }
        String activityName = resolveActivityName(activityId);
        List<MemberBenefit> out = new ArrayList<>(winners.size());
        LocalDateTime now = LocalDateTime.now();
        for (DrawWinner w : winners) {
            String prizeValue = activityName + " - " + w.getRank();
            out.add(MemberBenefit.builder()
                    .userId(w.getUserId())
                    .benefitType(BenefitType.PRIZE.name())
                    .benefitValue(prizeValue)
                    .activityId(activityId)
                    .effectiveActivityId(activityId)
                    .sourceId(null)
                    .status(1)
                    .grantedAt(now)
                    .build());
        }
        return out;
    }

    private String resolveActivityName(Long activityId) {
        return activityRepository.findById(activityId)
                .map(Activity::getName)
                .orElse("activity-" + activityId);
    }
}
