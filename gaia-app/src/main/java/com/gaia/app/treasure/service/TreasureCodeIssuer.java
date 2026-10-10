package com.gaia.app.treasure.service;

import com.gaia.common.constants.RedisKeys;
import com.gaia.domain.member.MemberBenefit;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.domain.service.treasure.TreasureCodeGenerator;
import com.gaia.app.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 夺宝码发放服务。
 *
 * <p>封装"生成 → 入权益账本"的完整流程，
 * 在 DB 唯一约束冲突时自动重试（最多 {@link TreasureCodeGenerator#MAX_RETRY} 次）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TreasureCodeIssuer {

    private static final long LOCK_EXPIRE_MS = 5_000L;

    private final TreasureCodeGenerator generator;
    private final MemberBenefitRepository memberBenefitRepository;
    private final RedisLock redisLock;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 为指定用户发放 {@code count} 个夺宝码（活动内唯一）。
     *
     * @param userId      用户 ID
     * @param activityId  活动 ID
     * @param count       发放数量
     * @param sourceId    来源明细 {@code gaia_activity_record.id}
     * @return 实际入库的夺宝码列表（已去重）
     */
    public List<String> issue(Long userId, Long activityId, int count, Long sourceId) {
        if (count <= 0) {
            return List.of();
        }
        String lockKey = RedisKeys.TREASURE_TASK_LOCK_KEY
                .replace("{userId}", String.valueOf(userId))
                .replace("{taskId}", String.valueOf(activityId));
        String token = UUID.randomUUID().toString();
        boolean acquired = redisLock.tryLock(lockKey, token, LOCK_EXPIRE_MS);
        if (!acquired) {
            log.warn("夺宝码发放获取锁失败 userId={} activityId={}", userId, activityId);
            throw new IllegalStateException("夺宝码发放繁忙，请稍后重试");
        }
        try {
            List<String> granted = new ArrayList<>(count);
            int safetyBurst = 0;
            while (granted.size() < count) {
                int remain = count - granted.size();
                List<MemberBenefit> batch = new ArrayList<>(remain);
                List<String> candidates = new ArrayList<>(remain);
                for (int i = 0; i < remain; i++) {
                    String code = generator.generateOne();
                    candidates.add(code);
                    MemberBenefit b = MemberBenefit.builder()
                            .userId(userId)
                            .benefitType("LOTTERY_CODE")
                            .benefitValue(code)
                            .activityId(activityId)
                            .sourceId(sourceId)
                            .status(1)
                            .build();
                    batch.add(b);
                }
                try {
                    memberBenefitRepository.insertLotteryCodes(batch);
                    granted.addAll(candidates);
                    bumpUserCodeCacheVersion(userId, activityId);
                } catch (DuplicateKeyException dup) {
                    safetyBurst++;
                    if (safetyBurst > TreasureCodeGenerator.MAX_RETRY) {
                        log.error("夺宝码生成连续冲突超过 {} 次 userId={} activityId={}",
                                TreasureCodeGenerator.MAX_RETRY, userId, activityId, dup);
                        throw new IllegalStateException("夺宝码生成失败，请稍后重试", dup);
                    }
                    log.warn("夺宝码发放遇唯一冲突，本批重试 retry={} userId={} activityId={}",
                            safetyBurst, userId, activityId);
                }
            }
            return granted;
        } finally {
            redisLock.release(lockKey, token);
        }
    }

    /**
     * 用户夺宝码缓存版本自增，使相关缓存失效。
     */
    private void bumpUserCodeCacheVersion(Long userId, Long activityId) {
        try {
            String key = RedisKeys.TREASURE_USER_CODES_KEY
                    .replace("{userId}", String.valueOf(userId))
                    .replace("{activityId}", String.valueOf(activityId));
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("用户夺宝码缓存清理异常 userId={} activityId={}", userId, activityId, e);
        }
    }
}