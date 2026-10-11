package com.gaia.app.draw.ext;

import com.gaia.domain.ability.draw.ext.CalculateProbabilityExt;
import com.gaia.domain.ability.draw.ext.IssuePrizeExt;
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
 * 集奖开奖 玩法（{@code bizCode = "draw"}）。
 *
 * <p>本类是 {@code @BizProduct(code="draw")} 的 bean，
 * 同时 {@code implements} {@link CalculateProbabilityExt} 与 {@link IssuePrizeExt}
 * 两个扩展点接口；类内方法再用 {@code @Extension} 标注，由
 * {@link com.gaia.app.framework.ExtensionRegistry#init()} 按
 * {@code (bizCode, extCode)} 索引。</p>
 *
 * <p>直接 {@code implements} 扩展接口（而非仅暴露同名方法）使得
 * {@code AbstractBaseAbility.reduceExecute} 拿到的 bean 真正满足
 * {@code (ExtType) ext} 类型转换，避免 {@code ClassCastException}。</p>
 *
 * <p>两个扩展点的默认实现：</p>
 * <ul>
 *   <li>{@link #calculate(Long)} — 按"夺宝码数量"作为权重直接归一化为概率，
 *       与原 {@code DrawService} 的加权策略保持一致，便于平滑迁移。</li>
 *   <li>{@link #issue(Long, List)} — 给每个中奖者生成一条
 *       {@link BenefitType#PRIZE} 类型的 {@link MemberBenefit} 奖品条目，
 *       {@code benefitValue} 形如 {@code "<活动名> - <rank>"}。</li>
 * </ul>
 *
 * <p>本类只依赖 {@code gaia-domain} 内的仓储接口；具体实现
 * （{@code com.gaia.infra.persistence.repository.*RepositoryImpl}）由
 * {@code gaia-bootstrap} 在启动时扫包注入。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@BizProduct(code = "draw", name = "集奖开奖")
public class DrawBizProduct implements CalculateProbabilityExt, IssuePrizeExt {

    private final MemberBenefitRepository memberBenefitRepository;
    private final ActivityRepository activityRepository;

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
        log.debug("DrawBizProduct.calculate activityId={}", activityId);
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
        log.debug("DrawBizProduct.issue activityId={} winners={}",
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
