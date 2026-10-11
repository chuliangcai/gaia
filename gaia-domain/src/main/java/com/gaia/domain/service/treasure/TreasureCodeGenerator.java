package com.gaia.domain.service.treasure;

import com.gaia.domain.member.MemberBenefit;
import com.gaia.domain.repository.MemberBenefitRepository;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * 夺宝码生成器（纯领域服务）。
 *
 * <p>规则：6 位大写字母 {@code [A-Z]{6}}，空间 26^6 ≈ 3.08 亿。</p>
 * <p>活动内唯一由数据库生成列 {@code uk_lottery_code} 的 UK 约束保证；
 * 本生成器在 DB 唯一冲突时本地重试，最多 {@link #MAX_RETRY} 次。</p>
 */
@Slf4j
public class TreasureCodeGenerator {

    /** 码长度 */
    public static final int CODE_LENGTH = 6;

    /** 字母表 A-Z */
    public static final char[] ALPHABET;
    static {
        ALPHABET = new char[26];
        for (int i = 0; i < 26; i++) {
            ALPHABET[i] = (char) ('A' + i);
        }
    }

    /** 单批最大重试次数（超出则抛异常） */
    public static final int MAX_RETRY = 8;

    private final SecureRandom random = new SecureRandom();

    /**
     * 生成一个 6 位大写字母随机串。
     */
    public String generateOne() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    /**
     * 验证是否是合法格式：6 位 A-Z。
     */
    public boolean isValid(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成 {@code count} 个唯一夺宝码，依赖 DB 唯一约束保证活动内唯一。
     *
     * <p>若生成过程中遇到 DB 唯一冲突（极小概率），本方法会局部重试；
     * 重试超过 {@link #MAX_RETRY} 次则抛 {@link IllegalStateException}。</p>
     *
     * @param userId     接收用户
     * @param activityId 所属活动
     * @param count      需要生成的码数量
     * @param repo       权益仓储（依赖唯一约束）
     * @param sourceId   来源明细 {@code gaia_activity_record.id}
     * @return 实际入库的夺宝码列表
     */
    public List<String> generateAndGrant(Long userId, Long activityId, int count,
                                         MemberBenefitRepository repo, Long sourceId) {
        if (count <= 0) {
            return List.of();
        }
        List<String> granted = new ArrayList<>(count);
        int safetyBurst = 0;
        while (granted.size() < count) {
            // 每轮为剩余名额生成候选码
            int remain = count - granted.size();
            List<MemberBenefit> batch = new ArrayList<>(remain);
            List<String> candidates = new ArrayList<>(remain);
            for (int i = 0; i < remain; i++) {
                String code = generateOne();
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
                int inserted = repo.insertBenefits(batch);
                // 依赖 DB UK 保证唯一，inserted == batch.size() 视为全部成功
                if (inserted == batch.size()) {
                    granted.addAll(candidates);
                } else {
                    // 部分冲突：退避重试整批
                    safetyBurst++;
                    if (safetyBurst > MAX_RETRY) {
                        throw new IllegalStateException(
                                "夺宝码生成失败：连续 " + MAX_RETRY + " 轮存在唯一冲突");
                    }
                    log.warn("夺宝码批量插入部分冲突 inserted={} expected={} retry={}",
                            inserted, batch.size(), safetyBurst);
                }
            } catch (Exception ex) {
                // 唯一约束冲突抛出的异常：捕获后重试
                if (isUniqueKeyConflict(ex)) {
                    safetyBurst++;
                    if (safetyBurst > MAX_RETRY) {
                        throw new IllegalStateException(
                                "夺宝码生成失败：连续 " + MAX_RETRY + " 轮存在唯一冲突", ex);
                    }
                    log.warn("夺宝码生成遇唯一冲突 retry={}", safetyBurst);
                } else {
                    throw ex;
                }
            }
        }
        return granted;
    }

    private boolean isUniqueKeyConflict(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null && (msg.contains("Duplicate entry")
                    || msg.contains("uk_lottery_code")
                    || msg.contains("UniqueViolation"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}