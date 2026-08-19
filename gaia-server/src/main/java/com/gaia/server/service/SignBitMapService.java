package com.gaia.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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
 */
@Slf4j
@Component
public class SignBitMapService {

    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String KEY_PREFIX = "gaia:sign:bitmap:";

    private final JedisPool jedisPool;

    public SignBitMapService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 设置某天已签到。
     *
     * @return true 表示该天之前未签到，本次置位成功；false 表示已签到
     */
    public boolean setSigned(long userId, LocalDate date) {
        String key = key(userId, date);
        long offset = date.getDayOfMonth() - 1L;
        try (Jedis jedis = jedisPool.getResource()) {
            Boolean before = jedis.setbit(key, offset, true);
            return Boolean.FALSE.equals(before);
        }
    }

    /**
     * 查询某天是否签到。
     */
    public boolean isSigned(long userId, LocalDate date) {
        String key = key(userId, date);
        long offset = date.getDayOfMonth() - 1L;
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.getbit(key, offset);
        }
    }

    /**
     * 查询当月已签到日期集合。
     */
    public List<String> signedDates(long userId, String yearMonth) {
        String key = KEY_PREFIX + userId + ":" + yearMonth;
        LocalDate monthStart = LocalDate.parse(yearMonth + "01",
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        int daysInMonth = monthStart.lengthOfMonth();

        List<String> result = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            for (int day = 1; day <= daysInMonth; day++) {
                if (Boolean.TRUE.equals(jedis.getbit(key, day - 1L))) {
                    LocalDate d = monthStart.withDayOfMonth(day);
                    result.add(d.format(DATE_FORMATTER));
                }
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
        try (Jedis jedis = jedisPool.getResource()) {
            while (true) {
                String key = key(cursor);
                long offset = cursor.getDayOfMonth() - 1L;
                boolean signed = jedis.getbit(key, offset);
                if (!signed) {
                    // 允许昨日之前才断签；如果是当天首次签到场景，调用方需自行判断
                    if (cursor.isEqual(today)) {
                        cursor = cursor.minusDays(1);
                        continue;
                    }
                    break;
                }
                streak++;
                cursor = cursor.minusDays(1);
            }
        }
        return streak;
    }

    /**
     * 批量 OR 运算检查（用于跨月判断）。
     */
    public long countInMonth(long userId, String yearMonth) {
        String key = KEY_PREFIX + userId + ":" + yearMonth;
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.bitcount(key);
        }
    }

    private String key(long userId, LocalDate date) {
        return KEY_PREFIX + userId + ":" + date.format(YM_FORMATTER);
    }

    private String key(LocalDate date) {
        return date.format(YM_FORMATTER);
    }

    // 预留工具方法（BitOP 多 Key 聚合）
    @SuppressWarnings("unused")
    private long mergeMonths(long userId, String... yearMonths) {
        String[] keys = new String[yearMonths.length];
        for (int i = 0; i < yearMonths.length; i++) {
            keys[i] = KEY_PREFIX + userId + ":" + yearMonths[i];
        }
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.bitop(BitOP.AND, "tmp_" + userId, keys);
        }
    }

    static {
        ZoneId.of("Asia/Shanghai");
    }
}
