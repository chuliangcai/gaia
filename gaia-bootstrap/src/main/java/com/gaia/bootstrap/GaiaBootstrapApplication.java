package com.gaia.bootstrap;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Gaia 应用进程启动入口。
 *
 * <p>scanBasePackages 同时扫描：</p>
 * <ul>
 *   <li>{@code com.gaia.bootstrap} — 本模块的主类所在包</li>
 *   <li>{@code com.gaia.server} — gaia-app 的应用层 Bean（@Service / @DubboService 等）</li>
 *   <li>{@code com.gaia.infra} — gaia-infra 的基础设施 Bean（Repository 实现 / Mapper）</li>
 *   <li>{@code com.gaia.common} / {@code com.gaia.api} — 公共模块</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {
        "com.gaia.bootstrap",
        "com.gaia.server",
        "com.gaia.infra",
        "com.gaia.common",
        "com.gaia.api"
})
@MapperScan({
        "com.gaia.server.mapper",       // gaia-app 内的 MyBatis Mapper
        "com.gaia.infra.persistence.mapper"  // gaia-infra 内的 MyBatis Mapper
})
@EnableDubbo
@EnableTransactionManagement
@EnableScheduling
public class GaiaBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaiaBootstrapApplication.class, args);
    }
}
