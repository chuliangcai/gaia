package com.gaia.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Jedis 连接池配置。
 *
 * <p>所有 BitMap 操作、分布式锁都通过该连接池获取连接，避免每次新建。</p>
 */
@Configuration
public class JedisConfig {

    @Value("${gaia.redis.host:127.0.0.1}")
    private String host;

    @Value("${gaia.redis.port:6379}")
    private int port;

    @Value("${gaia.redis.password:}")
    private String password;

    @Value("${gaia.redis.database:0}")
    private int database;

    @Value("${gaia.redis.timeout:2000}")
    private int timeout;

    @Value("${gaia.redis.pool.max-total:64}")
    private int maxTotal;

    @Value("${gaia.redis.pool.max-idle:16}")
    private int maxIdle;

    @Value("${gaia.redis.pool.min-idle:4}")
    private int minIdle;

    @Bean(destroyMethod = "close")
    public JedisPool jedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);

        if (password == null || password.isEmpty()) {
            return new JedisPool(config, host, port, timeout, null, database);
        }
        return new JedisPool(config, host, port, timeout, password, database);
    }
}
