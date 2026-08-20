package com.gaia.server;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Gaia 服务进程启动入口。
 *
 * <p>同时承载所有 Dubbo Provider（SignService 等），
 * 内部以 com.gaia.server.* 承载 entity / mapper / service / config。</p>
 */
@SpringBootApplication(scanBasePackages = {"com.gaia.server", "com.gaia.common", "com.gaia.api"})
@EnableDubbo
@EnableTransactionManagement
public class GaiaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaiaServerApplication.class, args);
    }
}
