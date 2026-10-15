package com.gaia.domain.ability.engine;

import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IAbilityExt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 框架静态桥接配置。
 *
 * <p>将容器侧 {@link ExtensionEngine} 的查找能力注册到
 * {@link AbstractBaseAbility.AbilityExtHolder}，
 * 使得 {@code AbstractBaseAbility#reduceExecute(...)}（位于 gaia-common，无 Spring 依赖）
 * 能跨容器边界找到具体扩展点实例。</p>
 *
 * <p>此桥注册发生在 {@code ApplicationRunner} 阶段：早于任何业务请求。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FrameworkBridgeConfig {

    private final ExtensionEngine extensionEngine;

    /**
     * 启动时把 {@code ExtensionEngine::lookup} 注入到框架侧静态桥。
     */
    @Bean
    public ApplicationRunner bridgeExtensionEngine() {
        return (ApplicationRunner) (ApplicationArguments args) -> {
            AbstractBaseAbility.AbilityExtHolder.register(
                    (bizCode, extCode) -> extensionEngine.lookup(bizCode, extCode, IAbilityExt.class));
            log.info("framework bridge registered: AbstractBaseAbility.AbilityExtHolder -> ExtensionEngine");
        };
    }
}
