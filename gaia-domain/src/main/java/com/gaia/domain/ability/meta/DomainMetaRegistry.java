package com.gaia.domain.ability.meta;

import com.gaia.api.ability.DomainDescriptor;
import com.gaia.iframework.annotation.DomainNs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 领域命名空间（{@code @DomainNs}）元数据注册表。
 *
 * <p>启动期扫描所有 Spring bean 上标注的 {@link DomainNs}，构建
 * {@code code → DomainDescriptor} 索引。<strong>key（{@code code}）与
 * {@code @Ability#bizNs()} 字符串对齐</strong>，由 {@code AbilityMetaRegistry}
 * 在构造 {@code AbilityDescriptor} 时按 {@code bizNs} 反查
 * {@code domainName}。</p>
 *
 * <p>扫描实现参考 {@code BizProductMetaRegistry}：只扫 Spring 容器内的 bean，
 * 因此 {@code @DomainNs} 类需同时标注 {@code @Component} 让其成为 bean。</p>
 */
@Slf4j
@Component
public class DomainMetaRegistry implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final ConcurrentMap<String, DomainDescriptor> byCode = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(DomainNs.class);
        for (Object bean : beans.values()) {
            // 同 BizProductMetaRegistry 处理：CGLIB 代理下 getAnnotation 拿不到，
            // 走 AnnotationUtils 沿代理链向上找。
            DomainNs ann = AnnotationUtils.findAnnotation(bean.getClass(), DomainNs.class);
            if (ann == null) {
                log.warn("skip bean without resolvable @DomainNs, class={}", bean.getClass().getName());
                continue;
            }
            String code = ann.code();
            if (byCode.putIfAbsent(code, toDescriptor(ann)) != null) {
                log.warn("duplicate domain code in registry, code={}", code);
            } else {
                log.info("registered domain meta code={} name={} impl={}",
                        code, ann.name(), bean.getClass().getSimpleName());
            }
        }
        log.info("DomainMetaRegistry initialized, count={}", byCode.size());
    }

    /** 全量不可变快照。 */
    public List<DomainDescriptor> listAll() {
        return List.copyOf(byCode.values());
    }

    public Optional<DomainDescriptor> get(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCode.get(code));
    }

    public Map<String, DomainDescriptor> snapshot() {
        return Collections.unmodifiableMap(byCode);
    }

    /** 按 bizNs 批量反查，对 {@link AbilityDescriptor#domainName} 注入用。 */
    public Optional<String> findName(String bizNs) {
        if (bizNs == null) return Optional.empty();
        return get(bizNs).map(DomainDescriptor::getName);
    }

    private static DomainDescriptor toDescriptor(DomainNs ann) {
        return DomainDescriptor.builder()
                .code(Objects.requireNonNullElse(ann.code(), ""))
                .name(Objects.requireNonNullElse(ann.name(), ""))
                .build();
    }
}
