package com.gaia.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gaia 后台管理启动入口。
 *
 * <p>依赖关系：仅显式依赖 {@code com.gaia:gaia-domain}，
 * 不直接依赖 gaia-infra —— 基础设施层通过 Spring Boot
 * 自动配置（AutoConfiguration.imports）在启动时被发现。</p>
 *
 * <p>端口与 gaia-gateway（8080）/ gaia-app（Dubbo tri 20881）错开，使用 8088。</p>
 */
@SpringBootApplication(scanBasePackages = {"com.gaia.backend", "com.gaia.common"})
public class GaiaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaiaBackendApplication.class, args);
    }
}
