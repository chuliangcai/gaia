package com.gaia.server.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 全链路追踪配置（Lettuce + Micrometer Observation）。
 *
 * <p>把 Spring Boot 3 / Spring Data Redis 3.2 内置的 Lettuce Micrometer Tracing
 * 接入到 Brave / Zipkin，使每条 Redis 命令自动产出 Brave span。
 * 业务侧调用 {@link StringRedisTemplate} 或 {@link RedisConnectionFactory}
 * 时无需任何改动，span 由底层 Lettuce 自动创建。</p>
 *
 * <p>由于 {@code dubbo-spring-boot-tracing-brave-zipkin-starter} 已装配
 * {@code BraveTracer} 与 {@code ObservationRegistry}，本配置只需要：
 * <ol>
 *   <li>创建带 Micrometer Tracing 适配的 {@link ClientResources}</li>
 *   <li>通过 {@link LettuceClientConfigurationBuilderCustomizer} 把 ClientResources 注入到
 *       Spring Boot 自动装配的 {@link LettuceConnectionFactory}</li>
 * </ol>
 * </p>
 */
@Slf4j
@Configuration
public class RedisTracingConfig {

    /**
     * 让 Lettuce 使用带 Micrometer Tracing 的 ClientResources。
     *
     * <p>通过 Spring Boot 自动装配提供的 customizer 钩子注入，
     * 不需要直接 new LettuceConnectionFactory，避免与 Spring Boot 的
     * spring.data.redis.* 自动配置冲突。</p>
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceTracingCustomizer(ObservationRegistry observationRegistry) {
        return clientConfigurationBuilder -> {
            // 与 Spring Boot 自动装配合并：优先复用已有 ClientResources，
            // 若用户未配置，则创建带 Micrometer Tracing 的实例。
            ClientResources clientResources = ClientResources.builder()
                    .tracing(new MicrometerTracing(observationRegistry, "gaia-redis"))
                    .build();
            clientConfigurationBuilder.clientResources(clientResources);
            clientConfigurationBuilder.clientOptions(ClientOptions.builder().build());
            log.info("已为 Lettuce 启用 Micrometer Tracing，Redis 命令将自动产出 Brave span");
        };
    }

    /**
     * 提供 {@link StringRedisTemplate} Bean，业务侧直接注入即可使用，
     * 不再需要 JedisPool。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        // 关闭事务 / Pipeline 透传，全部走默认同步模式
        template.setExposeConnection(true);
        template.afterPropertiesSet();
        return template;
    }
}
