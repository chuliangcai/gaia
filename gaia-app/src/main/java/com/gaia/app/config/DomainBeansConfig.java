package com.gaia.app.config;

import com.gaia.domain.service.treasure.TreasureCodeGenerator;
import com.gaia.domain.service.treasure.WeightedDrawAlgorithm;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 领域层 Bean 装配。
 *
 * <p>gaia-domain 故意不引入 {@code org.springframework.*}（见 gaia-domain/README.md），
 * 所以纯领域类没有 {@code @Component} 注解。本配置类在应用层把领域类声明为 Spring Bean，
 * 既保持领域层纯净，又能让 {@code @Autowired} 正常工作。</p>
 */
@Configuration
public class DomainBeansConfig {

    @Bean
    public TreasureCodeGenerator treasureCodeGenerator() {
        return new TreasureCodeGenerator();
    }

    @Bean
    public WeightedDrawAlgorithm weightedDrawAlgorithm() {
        return new WeightedDrawAlgorithm();
    }
}
