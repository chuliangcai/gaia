package com.gaia.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisStringCommands.BitOperation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * BitMap 签到存储。
 *
 * <p>Redis BitMap Key 格式：{@code gaia:sign:bitmap:{userId}:{yyyyMM}}</p>
 *
 * <p>偏移量为日期（1-based）。每月一个 BitMap，自然按月归档，
 * 内存占用低且查询效率 O(1)。</p>
 *
 * <p>底层使用 {@link StringRedisTemplate}（Spring Data Redis + Lettuce），
 * 由 Lettuce Micrometer Tracing 自动为每条 Redis 命令产出 Brave span，
 * 业务代码无需显式埋点。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignBitMapService {

    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String KEY_PREFIX = "gaia:sign:bitmap:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 设置某天已签到。
     *
     * @return true 表示该天之前未签到，本次置位成功；false 表示已签到
     */
    public boolean setSigned(long userId, LocalDate date) {
        String key = key(userId, date);
        long offset = date.getDayOfMonth() - 1L;
        Boolean before = redisTemplate.opsForValue().setBit(key, offset, true);
        return Boolean.FALSE.equals(before);
    }

    /**
     * 查询某天是否签到。
     */
    public boolean isSigned(long userId, LocalDate date) {
        String key = key(userId, date);
        long offset = date.getDayOfMonth() - 1L;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, offset));
    }

    /**
     * 查询当月已签到日期集合。
     */
    public List<String> signedDates(long userId, String yearMonth) {
        String key = KEY_PREFIX + userId + ":" + yearMonth;
        LocalDate monthStart = LocalDate.parse(yearMonth + "01",
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        int daysInMonth = monthStart.lengthOfMonth();

        // 用 BITFIELD 一次读出当月所有 bit，再在内存中解析，避免 N 次 GETBIT。
        // Spring Data Redis 用 BitFieldSubCommands 表达。
        BitFieldSubCommands subCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(daysInMonth))
                .valueAt(0L);

        List<String> result = new ArrayList<>();
        List<Long> raw = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<List<Long>>) connection ->
                        connection.stringCommands().bitField(key.getBytes(), subCommands));
        if (raw == null || raw.isEmpty() || raw.get(0) == null) {
            return result;
        }
        long bits = raw.get(0);
        for (int day = 1; day <= daysInMonth; day++) {
            if (((bits >> (day - 1)) & 1L) == 1L) {
                LocalDate d = monthStart.withDayOfMonth(day);
                result.add(d.format(DATE_FORMATTER));
            }
        }
        return result;
    }

    /**
     * 计算累计连续签到天数（从今天往回连续命中）。
     */
    public int currentStreak(long userId, LocalDate today) {
        LocalDate cursor = today;
        int streak = 0;
        // 一次最多回看 31 天，循环 BITGET BITCOUNT 无法跨月聚合，故逐月 BITCOUNT + GETBIT
        while (true) {
            String key = KEY_PREFIX + userId + ":" + cursor.format(YM_FORMATTER);
            long offset = cursor.getDayOfMonth() - 1L;
            Boolean signed = redisTemplate.opsForValue().getBit(key, offset);
            if (!Boolean.TRUE.equals(signed)) {
                if (cursor.isEqual(today)) {
                    cursor = cursor.minusDays(1);
                    continue;
                }
                break;
            }
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * 批量 OR 运算检查（用于跨月判断）。
     */
    public long countInMonth(long userId, String yearMonth) {
        String key = KEY_PREFIX + userId + ":" + yearMonth;
        Long count = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Long>) connection ->
                        connection.stringCommands().bitCount(key.getBytes()));
        return count == null ? 0L : count;
    }

    private String key(long userId, LocalDate date) {
        return KEY_PREFIX + userId + ":" + date.format(YM_FORMATTER);
    }

    // 预留工具方法（BitOP 多 Key 聚合）
    @SuppressWarnings("unused")
    private long mergeMonths(long userId, String... yearMonths) {
        String[] keys = new String[yearMonths.length];
        for (int i = 0; i < yearMonths.length; i++) {
            keys[i] = KEY_PREFIX + userId + ":" + yearMonths[i];
        }
        String destKey = "tmp_" + userId;
        Long len = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Long>) connection ->
                        connection.stringCommands().bitOp(BitOperation.AND,
                                destKey.getBytes(),
                                keysToBytes(keys)));
        return len == null ? 0L : len;
    }

    private byte[][] keysToBytes(String[] keys) {
        byte[][] arr = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            arr[i] = keys[i].getBytes();
        }
        return arr;
    }

    static {
        ZoneId.of("Asia/Shanghai");
    }
}
