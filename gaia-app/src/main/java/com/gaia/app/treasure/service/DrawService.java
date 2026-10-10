package com.gaia.app.treasure.service;

import com.gaia.common.constants.RedisKeys;
import com.gaia.domain.base.activity.ActionType;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.base.activity.ActivityRecord;
import com.gaia.domain.draw.DrawCandidate;
import com.gaia.domain.draw.DrawWinner;
import com.gaia.domain.member.BenefitType;
import com.gaia.domain.member.MemberBenefit;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.domain.service.treasure.WeightedDrawAlgorithm;
import com.gaia.app.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 开奖服务（带幂等 + 分布式锁 + 乐观锁）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrawService {

    private static final long LOCK_EXPIRE_MS = 30_000L;

    private final ActivityRepository activityRepository;
    private final MemberBenefitRepository memberBenefitRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final WeightedDrawAlgorithm drawAlgorithm;
    private final RedisLock redisLock;

    /**
     * 对活动执行开奖：幂等，已开过则跳过。
     */
    public void draw(Long activityId) {
        String actionCode = "draw:" + activityId;
        // 幂等检查
        Optional<ActivityRecord> existing = activityRecordRepository.findByActionCode(actionCode);
        if (existing.isPresent()) {
            log.info("活动已开奖，跳过 activityId={}", activityId);
            return;
        }
        String lockKey = RedisKeys.TREASURE_DRAW_LOCK_KEY
                .replace("{activityId}", String.valueOf(activityId));
        String token = UUID.randomUUID().toString();
        boolean acquired = redisLock.tryLock(lockKey, token, LOCK_EXPIRE_MS);
        if (!acquired) {
            log.warn("开奖获取分布式锁失败 activityId={}", activityId);
            throw new IllegalStateException("开奖进行中，请稍后重试");
        }
        try {
            // 双重检查：拿到锁后再确认一次幂等
            existing = activityRecordRepository.findByActionCode(actionCode);
            if (existing.isPresent()) {
                log.info("活动已开奖，跳过（锁内二次校验）activityId={}", activityId);
                return;
            }
            doDrawTx(activityId, actionCode);
        } finally {
            redisLock.release(lockKey, token);
        }
    }

    /**
     * 开奖事务：活动标记 + 抽签 + 写明细 + 写奖品。
     */
    @Transactional(rollbackFor = Exception.class)
    public void doDrawTx(Long activityId, String actionCode) {
        // 1. load + 状态推进
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalStateException("活动不存在 id=" + activityId));
        if (activity.isDrawn()) {
            log.warn("活动已开奖，跳过 activityId={}", activityId);
            return;
        }
        activity.markDrawn();
        try {
            activityRepository.save(activity);
        } catch (Exception e) {
            log.error("活动状态推进 2→3 失败 activityId={}", activityId, e);
            throw e;
        }

        // 2. 加权抽奖
        List<DrawCandidate> candidates = memberBenefitRepository.countLotteryCodesByActivity(activityId);
        if (candidates.isEmpty()) {
            log.warn("活动无人参与 activityId={}", activityId);
            // 仍写一条 DRAW_EXECUTED 明细记录，但 winners 为空
            ActivityRecord rec = ActivityRecord.builder()
                    .userId(0L) // 系统记录
                    .activityId(activityId)
                    .actionType(ActionType.DRAW_EXECUTED)
                    .actionCode(actionCode)
                    .bizId(activityId)
                    .payload(null)
                    .result("{\"winners\":[]}")
                    .createdAt(LocalDateTime.now())
                    .build();
            activityRecordRepository.insertIfAbsent(rec);
            return;
        }
        int winnerCount = activity.getWinnerCount() == null ? 1 : activity.getWinnerCount();
        List<DrawWinner> winners = drawAlgorithm.draw(candidates, winnerCount);
        log.info("活动开奖候选数={} 中奖数={} activityId={}", candidates.size(), winners.size(), activityId);

        // 3. 给每个中奖者选一条夺宝码作为"中奖码" + 写奖品 + 写中奖明细
        List<MemberBenefit> prizes = new ArrayList<>();
        List<ActivityRecord> winRecords = new ArrayList<>();
        for (DrawWinner w : winners) {
            // 给中奖者从其夺宝码中取一条作为中奖码
            List<String> userCodes = memberBenefitRepository
                    .listLotteryCodesByUserAndActivity(w.getUserId(), activityId);
            String winningCode = userCodes.isEmpty() ? null : userCodes.get(0);
            w.setCode(winningCode);

            // 奖品描述 = 活动名 + rank
            String prizeValue = activity.getName() + " - " + w.getRank();
            MemberBenefit prizeEntity = MemberBenefit.builder()
                    .userId(w.getUserId())
                    .benefitType(BenefitType.PRIZE.name())
                    .benefitValue(prizeValue)
                    .activityId(activityId)
                    .sourceId(null)
                    .status(1)
                    .grantedAt(LocalDateTime.now())
                    .build();
            prizes.add(prizeEntity);
        }
        // 先插奖品拿到 sourceId，再写 DRAW_WON 明细
        memberBenefitRepository.insertPrizes(prizes);
        for (int i = 0; i < prizes.size(); i++) {
            MemberBenefit p = prizes.get(i);
            DrawWinner w = winners.get(i);
            ActivityRecord rec = ActivityRecord.builder()
                    .userId(w.getUserId())
                    .activityId(activityId)
                    .actionType(ActionType.DRAW_WON)
                    .actionCode("draw_win:" + activityId + ":" + w.getUserId())
                    .bizId(activityId)
                    .payload(null)
                    .result("{\"rank\":" + w.getRank() + ",\"code\":\"" + safeCode(w.getCode()) + "\"}")
                    .createdAt(LocalDateTime.now())
                    .build();
            activityRecordRepository.insertIfAbsent(rec);
        }

        // 4. 写 DRAW_EXECUTED 明细（含完整 winners 列表）
        String winnersJson = buildWinnersJson(winners);
        ActivityRecord exec = ActivityRecord.builder()
                .userId(0L)
                .activityId(activityId)
                .actionType(ActionType.DRAW_EXECUTED)
                .actionCode(actionCode)
                .bizId(activityId)
                .payload(null)
                .result("{\"winners\":" + winnersJson + "}")
                .createdAt(LocalDateTime.now())
                .build();
        activityRecordRepository.insertIfAbsent(exec);

        log.info("活动开奖完成 activityId={} winners={}", activityId, winners.size());
    }

    private static String safeCode(String code) {
        return code == null ? "" : code.replace("\"", "\\\"");
    }

    private static String buildWinnersJson(List<DrawWinner> winners) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < winners.size(); i++) {
            DrawWinner w = winners.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"userId\":").append(w.getUserId())
                    .append(",\"code\":\"").append(safeCode(w.getCode())).append("\"")
                    .append(",\"rank\":").append(w.getRank())
                    .append(",\"weight\":").append(w.getWeight() == null ? 0 : w.getWeight())
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}