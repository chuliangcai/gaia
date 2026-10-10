package com.gaia.infra.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Gaia 基础设施层自动配置入口。
 *
 * <p>通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 暴露，任何引入 gaia-infra 的 Spring Boot 应用都会自动加载。</p>
 */
@AutoConfiguration
@ComponentScan("com.gaia.infra")
@MapperScan("com.gaia.infra.persistence.mapper")
public class InfraAutoConfiguration {
}
