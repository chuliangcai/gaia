package com.gaia.server.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;

/**
 * 基于 Redis 的分布式锁（SET NX + Lua 释放）。
 *
 * <p>用于签到场景下防止同一用户并发重入。</p>
 */
@Slf4j
@Component
public class RedisLock {

    private static final String RELEASE_LUA =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('DEL', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";

    private final JedisPool jedisPool;

    public RedisLock(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 尝试加锁。
     *
     * @param key      锁 Key
     * @param token    加锁标识（UUID，用于安全释放）
     * @param expireMs 过期时间（毫秒）
     * @return 加锁是否成功
     */
    public boolean tryLock(String key, String token, long expireMs) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(key, token, SetParams.setParams().nx().px(expireMs));
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("加锁失败 key={}", key, e);
            return false;
        }
    }

    /**
     * 释放锁。
     */
    public boolean release(String key, String token) {
        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(RELEASE_LUA,
                    Collections.singletonList(key),
                    Collections.singletonList(token));
            return result != null && "1".equals(result.toString());
        } catch (Exception e) {
            log.error("释放锁失败 key={}", key, e);
            return false;
        }
    }
}
