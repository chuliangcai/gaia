package com.gaia.server.sign;

import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import com.gaia.server.entity.UserSignRecord;
import com.gaia.server.lock.RedisLock;
import com.gaia.server.mapper.UserSignRecordMapper;
import com.gaia.server.service.SignBitMapService;
import com.gaia.server.service.SignRewardRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 签到能力 — 框架唯一的 {@code @Ability} 实现（v1）。
 *
 * <p>由 {@link com.gaia.server.framework.AbilityDispatcher} 按
 * {@code ModelRequest.assertType == "daily_checkin"} 分派，
 * 在该能力内根据 {@code ModelRequest.subType} 选择子场景。</p>
 *
 * <p>支持的子场景：
 * <ul>
 *     <li>{@code sign}（默认）：单日签到</li>
 *     <li>{@code calendar}：返回指定年月的签到日历，{@code yearMonth} 从 {@link RequestContext#getExtParams()} 取</li>
 *     <li>{@code streak}：返回当前连续签到天数</li>
 * </ul>
 *
 * <p>签到主体的实现原样从旧 {@code SignServiceImpl} 移植：
 * Redis BitMap 作为热路径主存储，SETNX + Lua 释放分布式锁防重入，
 * DB 仅做异步对账（异常丢失由定时补偿任务兜底）。</p>
 */
@Slf4j
@Component
@Ability(code = "daily_checkin", bizNs = "checkin", name = "每日签到")
public class SignAbility extends AbstractBaseAbility {

    private static final long LOCK_EXPIRE_MS = 5_000L;

    private static final String SUB_SIGN = "sign";
    private static final String SUB_CALENDAR = "calendar";
    private static final String SUB_STREAK = "streak";

    private final SignBitMapService bitMapService;
    private final SignRewardRule rewardRule;
    private final RedisLock redisLock;
    private final UserSignRecordMapper signRecordMapper;

    public SignAbility(SignBitMapService bitMapService,
                       SignRewardRule rewardRule,
                       RedisLock redisLock,
                       UserSignRecordMapper signRecordMapper) {
        this.bitMapService = bitMapService;
        this.rewardRule = rewardRule;
        this.redisLock = redisLock;
        this.signRecordMapper = signRecordMapper;
    }

    @Override
    public IFResult<?> execute(RequestContext context) {
        String sub = resolveSubType(context);
        return switch (sub) {
            case SUB_SIGN -> doSign(context.getUserId());
            case SUB_CALENDAR -> doCalendar(context);
            case SUB_STREAK -> doStreak(context.getUserId());
            default -> IFResult.fail("UNKNOWN_SUBTYPE");
        };
    }

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of(SUB_SIGN, SUB_CALENDAR, SUB_STREAK);
    }

    private String resolveSubType(RequestContext context) {
        Map<String, String> ext = context.getExtParams();
        String sub = (ext == null) ? null : ext.get("subType");
        return (sub == null || sub.isBlank()) ? SUB_SIGN : sub;
    }

    private IFResult<SignResultDTO> doSign(Long userId) {
        log.info("签到请求入口 userId={}", userId);
        if (userId == null || userId <= 0) {
            log.warn("签到参数非法 userId={}", userId);
            throw new IllegalArgumentException("userId 不能为空");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        String dateStr = today.toString();
        String lockKey = "gaia:sign:lock:" + userId + ":" + dateStr;
        String token = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();

        boolean acquired = redisLock.tryLock(lockKey, token, LOCK_EXPIRE_MS);
        if (!acquired) {
            log.info("签到命中防重入 userId={}, date={}, lockKey={}", userId, dateStr, lockKey);
            return IFResult.success(new SignResultDTO(false, 0, 0L, dateStr));
        }
        log.debug("签到分布式锁获取成功 userId={}, lockKey={}, token={}", userId, lockKey, token);
        try {
            // 1. BitMap 抢占（CAS 语义）
            boolean firstTime = bitMapService.setSigned(userId, today);
            if (!firstTime) {
                // 当日已签到，读取累计天数返回
                int streak = bitMapService.currentStreak(userId, today);
                log.info("签到重复请求 userId={}, date={}, streak={}", userId, dateStr, streak);
                return IFResult.success(new SignResultDTO(false, streak, 0L, dateStr));
            }
            log.info("签到 BitMap 抢占成功 userId={}, date={}", userId, dateStr);

            // 2. 计算连续天数与奖励
            int streak = bitMapService.currentStreak(userId, today);
            long reward = rewardRule.calculate(streak);
            log.info("签到奖励计算完成 userId={}, date={}, streak={}, reward={}",
                    userId, dateStr, streak, reward);

            // 3. 持久化对账（异步可优化为 MQ，本次同步落库简化）
            persistRecord(userId, today, streak, reward);

            log.info("签到成功 userId={}, date={}, streak={}, reward={}, cost={}ms",
                    userId, dateStr, streak, reward, System.currentTimeMillis() - start);
            return IFResult.success(new SignResultDTO(true, streak, reward, dateStr));
        } catch (Exception e) {
            log.error("签到异常 userId={}, date={}, cost={}ms",
                    userId, dateStr, System.currentTimeMillis() - start, e);
            throw e;
        } finally {
            redisLock.release(lockKey, token);
            log.debug("签到分布式锁释放 userId={}, lockKey={}, token={}", userId, lockKey, token);
        }
    }

    private IFResult<SignCalendarDTO> doCalendar(RequestContext ctx) {
        Long userId = ctx.getUserId();
        String yearMonth = ctx.getExtParams() == null ? null : ctx.getExtParams().get("yearMonth");
        log.info("查询签到日历入口 userId={}, yearMonth={}", userId, yearMonth);
        if (userId == null || userId <= 0) {
            log.warn("签到日历参数非法 userId={}", userId);
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (yearMonth == null || yearMonth.length() != 6) {
            log.warn("签到日历参数非法 yearMonth={}", yearMonth);
            throw new IllegalArgumentException("yearMonth 必须为 yyyyMM");
        }
        List<String> dates = bitMapService.signedDates(userId, yearMonth);
        LocalDate monthStart = LocalDate.parse(yearMonth + "01",
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        int size = dates == null ? 0 : dates.size();
        log.info("查询签到日历完成 userId={}, yearMonth={}, signedCount={}", userId, yearMonth, size);
        return IFResult.success(new SignCalendarDTO(yearMonth,
                dates == null ? new ArrayList<>() : dates,
                monthStart.lengthOfMonth()));
    }

    private IFResult<Integer> doStreak(Long userId) {
        log.info("查询连续签到天数入口 userId={}", userId);
        if (userId == null || userId <= 0) {
            log.warn("连续签到参数非法 userId={}", userId);
            throw new IllegalArgumentException("userId 不能为空");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        int streak = bitMapService.currentStreak(userId, today);
        log.info("查询连续签到天数完成 userId={}, streak={}", userId, streak);
        return IFResult.success(streak);
    }

    /**
     * 持久化签到记录。
     */
    @Transactional(rollbackFor = Exception.class)
    protected void persistRecord(Long userId, LocalDate date, int streak, long reward) {
        UserSignRecord record = new UserSignRecord();
        record.setUserId(userId);
        record.setSignDate(date.toString());
        record.setYearMonth(date.format(DateTimeFormatter.ofPattern("yyyyMM")));
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
