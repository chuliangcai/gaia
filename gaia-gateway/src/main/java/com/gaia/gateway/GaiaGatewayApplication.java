package com.gaia.gateway;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gaia 统一网关启动入口。
 *
 * <p>本工程不承载任何业务逻辑，仅负责 HTTP 入口、Dubbo 引用以及监控暴露。</p>
 */
@SpringBootApplication(scanBasePackages = {"com.gaia.gateway", "com.gaia.common"})
@EnableDubbo
public class GaiaGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaiaGatewayApplication.class, args);
    }
}
