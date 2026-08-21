package com.gaia.server.config;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 全链路追踪配置（Lettuce + Micrometer Observation）。
 *
 * <p>Spring Boot 3.2 + Spring Data Redis 3.2 默认只把 Brave/Micrometer Tracer 注入到 Lettuce 的
 * {@link ClientResources}，并不会自动启用 Lettuce 的 {@link MicrometerTracing}（即不会把每条
 * Redis 命令包装为 {@code redis.command.*} Observation）。这里通过 {@link LettuceClientConfigurationBuilderCustomizer}
 * 把 {@code MicrometerTracing} 注入到自动装配的 {@link io.lettuce.core.RedisClient}，
 * 这样每条 Redis 命令都会成为 Observation，并由
 * {@code micrometer-tracing-bridge-brave} 桥接为 Brave span，
 * 在 Zipkin UI 中显示为 {@code redis.command.<command>} 节点。</p>
 */
@Slf4j
@Configuration
public class RedisTracingConfig {

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceTracingCustomizer(ObservationRegistry observationRegistry) {
        return clientConfigurationBuilder -> {
            // 仅注入带 Micrometer Tracing 的 ClientResources；不要覆盖 clientOptions，
            // 否则会丢失 Spring Boot 默认装配的 CommandLatencyMetrics / 连接超时配置。
            ClientResources clientResources = ClientResources.builder()
                    .tracing(new MicrometerTracing(observationRegistry, "redis"))
                    .build();
            clientConfigurationBuilder.clientResources(clientResources);
            log.info("已为 lettuce 启用 Micrometer Tracing，Redis 命令将自动产出 Brave span");
        };
    }
}
