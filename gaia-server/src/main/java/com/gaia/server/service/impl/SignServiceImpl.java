package com.gaia.server.service.impl;

import com.gaia.api.sign.SignService;
import com.gaia.common.constants.DubboConstants;
import com.gaia.server.lock.RedisLock;
import com.gaia.server.mapper.UserSignRecordMapper;
import com.gaia.server.entity.UserSignRecord;
import com.gaia.server.service.SignBitMapService;
import com.gaia.server.service.SignRewardRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 签到核心实现。
 *
 * <p>设计要点：
 * <ul>
 *     <li>Redis BitMap 作为热路径主存储，O(1) 判定当日是否签到</li>
 *     <li>SETNX + Lua 释放 分布式锁防并发重入</li>
 *     <li>DB 仅做异步对账，定时补偿 BitMap 异常丢失</li>
 * </ul>
 * </p>
 */
@Slf4j
@DubboService(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
public class SignServiceImpl implements SignService {

    private static final long LOCK_EXPIRE_MS = 5_000L;

    @Autowired
    private SignBitMapService bitMapService;

    @Autowired
    private SignRewardRule rewardRule;

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private UserSignRecordMapper signRecordMapper;

    @Override
    public SignResultDTO sign(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        String dateStr = today.toString();
        String lockKey = "gaia:sign:lock:" + userId + ":" + dateStr;
        String token = UUID.randomUUID().toString();

        boolean acquired = redisLock.tryLock(lockKey, token, LOCK_EXPIRE_MS);
        if (!acquired) {
            // 命中防重入
            return new SignResultDTO(false, 0, 0L, dateStr);
        }
        try {
            // 1. BitMap 抢占（CAS 语义）
            boolean firstTime = bitMapService.setSigned(userId, today);
            if (!firstTime) {
                // 当日已签到，读取累计天数返回
                int streak = bitMapService.currentStreak(userId, today);
                return new SignResultDTO(false, streak, 0L, dateStr);
            }

            // 2. 计算连续天数与奖励
            int streak = bitMapService.currentStreak(userId, today);
            long reward = rewardRule.calculate(streak);

            // 3. 持久化对账（异步可优化为 MQ，本次同步落库简化）
            persistRecord(userId, today, streak, reward);

            return new SignResultDTO(true, streak, reward, dateStr);
        } finally {
            redisLock.release(lockKey, token);
        }
    }

    @Override
    public SignCalendarDTO calendar(Long userId, String yearMonth) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (yearMonth == null || yearMonth.length() != 6) {
            throw new IllegalArgumentException("yearMonth 必须为 yyyyMM");
        }
        List<String> dates = bitMapService.signedDates(userId, yearMonth);
        LocalDate monthStart = LocalDate.parse(yearMonth + "01",
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return new SignCalendarDTO(yearMonth, dates == null ? new ArrayList<>() : dates,
                monthStart.lengthOfMonth());
    }

    @Override
    public Integer streak(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        return bitMapService.currentStreak(userId, today);
    }

    /**
     * 持久化签到记录。
     */
    @Transactional(rollbackFor = Exception.class)
    protected void persistRecord(Long userId, LocalDate date, int streak, long reward) {
        UserSignRecord record = new UserSignRecord();
        record.setUserId(userId);
        record.setSignDate(date.toString());
        record.setYearMonth(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")));
        record.setRewardPoints(reward);
        record.setStreakDays(streak);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        try {
            signRecordMapper.insert(record);
        } catch (Exception e) {
            // DB 失败不影响 BitMap 主流程，对账任务后续补偿
            log.warn("签到记录持久化失败 userId={}, date={}", userId, date, e);
        }
    }
}
