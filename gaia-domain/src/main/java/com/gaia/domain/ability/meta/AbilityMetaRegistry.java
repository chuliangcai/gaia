package com.gaia.domain.ability.meta;

import com.gaia.api.ability.AbilityDescriptor;
import com.gaia.iframework.annotation.Ability;
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
import java.util.stream.Collectors;

/**
 * 能力元数据注册表。
 *
 * <p>启动时扫描所有 {@code @Ability} 标注的 bean，构造
 * {@code code → AbilityDescriptor} 索引。descriptor 包含：
 * <ul>
 *   <li>{@code code / bizNs / name} — 直接读自注解</li>
 *   <li>{@code configClassName} — 由 {@link Ability#configClass()} 得</li>
 *   <li>{@code configSchema} — 由 {@link ConfigSchemaIntrospector} 反射 configClass 得出</li>
 * </ul>
 * </p>
 *
 * <p>与框架的 {@code AbilityDispatcher} 解耦：本类只读元数据，不参与请求分派。</p>
 */
@Slf4j
@Component
public class AbilityMetaRegistry implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final ConcurrentMap<String, AbilityDescriptor> byCode = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Ability.class);
        for (Object bean : beans.values()) {
            // 与 AbilityDispatcher 同样需要走 AnnotationUtils，避免被 CGLIB 代理遮蔽注解
            Ability ann = AnnotationUtils.findAnnotation(bean.getClass(), Ability.class);
            if (ann == null) {
                log.warn("skip bean without resolvable @Ability, class={}", bean.getClass().getName());
                continue;
            }
            String code = ann.code();
            if (byCode.putIfAbsent(code, toDescriptor(ann)) != null) {
                log.warn("duplicate ability code in registry, code={}", code);
            } else {
                log.info("registered ability meta code={} bizNs={} config={}",
                        code, ann.bizNs(), ann.configClass().getName());
            }
        }
        log.info("AbilityMetaRegistry initialized, count={}", byCode.size());
    }

    /** 全量不可变快照。 */
    public List<AbilityDescriptor> listAll() {
        return List.copyOf(byCode.values());
    }

    public List<AbilityDescriptor> listByBizNs(String bizNs) {
        if (bizNs == null || bizNs.isBlank()) {
            return listAll();
        }
        return byCode.values().stream()
                .filter(d -> Objects.equals(d.getBizNs(), bizNs))
                .collect(Collectors.toUnmodifiableList());
    }

    public Optional<AbilityDescriptor> get(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCode.get(code));
    }

    public Map<String, AbilityDescriptor> snapshot() {
        return Collections.unmodifiableMap(byCode);
    }

    private static AbilityDescriptor toDescriptor(Ability ann) {
        Class<?> cfg = ann.configClass();
        return AbilityDescriptor.builder()
                .code(ann.code())
                .bizNs(ann.bizNs() == null ? "" : ann.bizNs())
                .name(ann.name())
                .configClassName(cfg == null ? Void.class.getName() : cfg.getName())
                .configSchema(ConfigSchemaIntrospector.introspect(cfg))
                .build();
    }
}
