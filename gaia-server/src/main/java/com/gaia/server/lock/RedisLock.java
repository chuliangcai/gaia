package com.gaia.server.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/**
 * 基于 Redis 的分布式锁（SET NX + Lua 释放）。
 *
 * <p>用于签到场景下防止同一用户并发重入。</p>
 *
 * <p>底层使用 {@link StringRedisTemplate}（Spring Data Redis + Lettuce），
 * 配合 Lettuce 的 Micrometer Tracing 集成自动产出 Brave span，
 * 业务代码无需显式埋点。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private static final String RELEASE_LUA =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('DEL', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(RELEASE_LUA, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试加锁。
     *
     * @param key      锁 Key
     * @param token    加锁标识（UUID，用于安全释放）
     * @param expireMs 过期时间（毫秒）
     * @return 加锁是否成功
     */
    public boolean tryLock(String key, String token, long expireMs) {
        try {
            Boolean ok = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Boolean>) connection ->
                    connection.stringCommands().set(
                            key.getBytes(),
                            token.getBytes(),
                            Expiration.from(Duration.ofMillis(expireMs)),
                            RedisStringCommands.SetOption.SET_IF_ABSENT));
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.error("加锁失败 key={}", key, e);
            return false;
        }
    }

    /**
     * 释放锁。
     */
    public boolean release(String key, String token) {
        try {
            Long result = redisTemplate.execute(RELEASE_SCRIPT,
                    Collections.singletonList(key),
                    token);
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("释放锁失败 key={}", key, e);
            return false;
        }
    }
}
