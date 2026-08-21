package com.gaia.server.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.gaia.server.observability.MybatisSqlObservationInterceptor;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis SQL Observation 拦截器注册。
 *
 * <p>由于 MyBatis-Plus 3.5.7 在 Spring Boot 3 下不再通过 Spring BeanPostProcessor
 * 暴露 {@code SqlSessionFactoryBean}，且当前依赖中未传递
 * {@code mybatis-spring-boot-autoconfigure}（无法用 {@code ConfigurationCustomizer}），
 * 走原生 Interceptor 注册路径非常曲折。
 *
 * <p>这里改用 MyBatis-Plus 官方推荐的方式：把我们的 SQL Observation 拦截器实现为
 * {@link com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor}，
 * 通过 {@code MybatisPlusInterceptor.addInnerInterceptor(...)} 注入。
 * MyBatis-Plus 自身已注册的 {@code MybatisPlusInterceptor} Bean 会被自动
 * 注入到 MyBatis Configuration 中，从而我们的 SQL Observation 拦截器也会
 * 一起生效。
 *
 * <p>业务侧 Mapper 不需要任何改动。</p>
 */
@Slf4j
@Configuration
public class MybatisPluginRegistration {

    /**
     * 注册 SQL Observation 拦截器 Bean（实现 MyBatis-Plus InnerInterceptor）。
     */
    @Bean
    public MybatisSqlObservationInterceptor mybatisSqlObservationInterceptor(ObservationRegistry observationRegistry) {
        return new MybatisSqlObservationInterceptor(observationRegistry);
    }

    /**
     * 显式声明 MybatisPlusInterceptor Bean，把我们的 SQL Observation 拦截器加到内部链中。
     * MyBatis-Plus auto-configuration 看到用户已声明 MybatisPlusInterceptor Bean 后会跳过默认装配。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisSqlObservationInterceptor sqlInterceptor) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 我们的 SQL Observation 拦截器放在最前面
        interceptor.addInnerInterceptor(sqlInterceptor);
        log.info("已注册 MyBatis-Plus SQL Observation 拦截器：MySQL 调用将自动产出 Brave span");
        return interceptor;
    }
}
