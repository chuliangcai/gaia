package com.gaia.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gaia 后台管理启动入口。
 *
 * <p>依赖关系：依赖 gaia-domain（领域接口 + 领域服务 DrawService / RedisLock / ActivityStatusRefresher / TreasureDrawScheduler）
 * 与 gaia-infra（Repository 实现），由 backend 自带的 ActivityAdminAppService 直接编排。
 * 不依赖 gaia-app，不走 Dubbo。</p>
 *
 * <p>scanBasePackages 同时扫描：</p>
 * <ul>
 *   <li>{@code com.gaia.backend} — 本模块的 controller / app service</li>
 *   <li>{@code com.gaia.domain} — 已下沉的 @Component / @Service Bean</li>
 *   <li>{@code com.gaia.common} — 公共模块</li>
 * </ul>
 *
 * <p>gaia-infra 通过 Spring Boot AutoConfiguration（{@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}）自动注册，无需显式 component scan。</p>
 *
 * <p>端口与 gaia-gateway（8080）/ gaia-app（Dubbo tri 20881）错开，使用 8088。</p>
 *
 * <p>故意不加 {@code @EnableScheduling}：调度器 bean 会随 context 启动，但不会 tick —
 * 开奖的自动调度由 gaia-bootstrap 上的 TreasureDrawScheduler 负责，backend 仅承担手动触发。</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.gaia.backend",
        "com.gaia.domain",
        "com.gaia.common"
})
public class GaiaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaiaBackendApplication.class, args);
    }
}
