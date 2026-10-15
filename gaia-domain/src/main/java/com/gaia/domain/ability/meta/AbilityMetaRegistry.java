package com.gaia.domain.ability.meta;

import com.gaia.api.ability.AbilityDescriptor;
import com.gaia.domain.ability.engine.AbilityEngine;
import com.gaia.iframework.annotation.Ability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * <p>启动时由 {@link AbilityEngine} 完成对 {@code @Ability} bean 的扫描，
 * 本类在自身 {@code @PostConstruct} 中通过 {@link AbilityEngine#snapshot()}
 * 读取同一份 bean 集合并构造 {@code code → AbilityDescriptor} 索引。descriptor 包含：
 * <ul>
 *   <li>{@code code / bizNs / name} — 直接读自注解</li>
 *   <li>{@code configClassName} — 由 {@link Ability#configClass()} 得</li>
 *   <li>{@code configSchema} — 由 {@link ConfigSchemaIntrospector} 反射 configClass 得出</li>
 * </ul>
 * </p>
 *
 * <p>与 {@link AbilityEngine} 解耦：本类只读元数据，不参与请求分派。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbilityMetaRegistry {

    /** {@link DomainMetaRegistry} 通过构造注入，启动期早于 {@link #init()} 完成自身 {@code @PostConstruct}。 */
    private final DomainMetaRegistry domainMetaRegistry;

    /**
     * 由构造器注入，以保证本类 {@code @PostConstruct} 跑在 {@link AbilityEngine#init()}
     * 之后；不要改用字段注入，否则 Spring 不再保证这个顺序。
     */
    private final AbilityEngine abilityEngine;

    /** 非 final 是为了绕开 JDT-LS 内嵌 Lombok 1.18.39 对 {@code @RequiredArgsConstructor} + 已初始化 final 字段的误判；
     *  实际引用不会被赋值（{@link #init()} 只调用 {@code putIfAbsent}），等同于 final。 */
    private ConcurrentMap<String, AbilityDescriptor> byCode = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Map<String, ?> snapshot = abilityEngine.snapshot();
        for (Map.Entry<String, ?> e : snapshot.entrySet()) {
            Object bean = e.getValue();
            // 与 AbilityEngine 同样需要走 AnnotationUtils，避免被 CGLIB 代理遮蔽注解
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

    private AbilityDescriptor toDescriptor(Ability ann) {
        Class<?> cfg = ann.configClass();
        String bizNs = ann.bizNs() == null ? "" : ann.bizNs();
        // 按 bizNs 反查 @DomainNs 中文名；查不到时（bizNs 为空或未声明对应 @DomainNs 类）置 null。
        String domainName = bizNs.isEmpty() ? null : domainMetaRegistry.findName(bizNs).orElse(null);
        return AbilityDescriptor.builder()
                .code(ann.code())
                .bizNs(bizNs)
                .domainName(domainName)
                .name(ann.name())
                .configClassName(cfg == null ? Void.class.getName() : cfg.getName())
                .configSchema(ConfigSchemaIntrospector.introspect(cfg))
                .backendTriggerable(ann.backendTriggerable())
                .build();
    }
}
