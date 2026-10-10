package com.gaia.infra.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
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

    /**
     * MyBatis-Plus 拦截器链。
     *
     * <p>当前装配：</p>
     * <ul>
     *   <li>{@link OptimisticLockerInnerInterceptor} — 处理 {@code @Version} 字段。
     *       注意：{@code BaseMapper.updateById} 的 SQL 一旦 PO 中存在 {@code @Version} 字段,
     *       MP 会自动在 WHERE 子句追加 {@code AND version=#{MP_OPTLOCK_VERSION_ORIGINAL}}。
     *       该参数由本拦截器在执行前注入到 ParamMap；
     *       <strong>mybatis-plus-spring-boot3-starter 不会默认注册此拦截器</strong>，
     *       必须显式启用，否则执行 {@code updateById} 时会抛
     *       {@code Parameter 'MP_OPTLOCK_VERSION_ORIGINAL' not found}。</li>
     * </ul>
     *
     * <p>未来如需分页 / 多租户 / 防全表更新等能力，直接在此处追加 {@link com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor}。</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
