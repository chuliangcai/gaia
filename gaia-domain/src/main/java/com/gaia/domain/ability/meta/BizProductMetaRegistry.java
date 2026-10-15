package com.gaia.domain.ability.meta;

import com.gaia.api.ability.BizProductDescriptor;
import com.gaia.domain.ability.engine.ExtensionEngine;
import com.gaia.iframework.annotation.BizProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 玩法（{@code @BizProduct}）元数据注册表。
 *
 * <p>启动时由 {@link ExtensionEngine} 完成对 {@code @BizProduct} bean 的扫描，
 * 本类在自身 {@code @PostConstruct} 中通过 {@link ExtensionEngine#bizProductSnapshot()}
 * 读取同一份 bean 集合并构造 {@code code → BizProductDescriptor} 索引。</p>
 *
 * <p>与 {@link ExtensionEngine} 解耦：本类只读元数据，不参与扩展点查找。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizProductMetaRegistry {

    /**
     * 由构造器注入，以保证本类 {@code @PostConstruct} 跑在 {@link ExtensionEngine#init()}
     * 之后；不要改用字段注入，否则 Spring 不再保证这个顺序。
     */
    private final ExtensionEngine extensionEngine;

    /** 非 final 是为了绕开 JDT-LS 内嵌 Lombok 1.18.39 对 {@code @RequiredArgsConstructor} + 已初始化 final 字段的误判；
     *  实际引用不会被赋值（{@link #init()} 只调用 {@code putIfAbsent}），等同于 final。 */
    private ConcurrentMap<String, BizProductDescriptor> byCode = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Map<String, Object> snapshot = extensionEngine.bizProductSnapshot();
        for (Map.Entry<String, Object> e : snapshot.entrySet()) {
            Object bean = e.getValue();
            // 与 ExtensionEngine 同样需要走 AnnotationUtils，避免被 CGLIB 代理遮蔽注解
            BizProduct ann = AnnotationUtils.findAnnotation(bean.getClass(), BizProduct.class);
            if (ann == null) {
                log.warn("skip bean without resolvable @BizProduct, class={}", bean.getClass().getName());
                continue;
            }
            String code = ann.code();
            if (byCode.putIfAbsent(code, toDescriptor(ann)) != null) {
                log.warn("duplicate bizProduct code in registry, code={}", code);
            } else {
                log.info("registered bizProduct meta code={} name={} impl={}",
                        code, ann.name(), bean.getClass().getSimpleName());
            }
        }
        log.info("BizProductMetaRegistry initialized, count={}", byCode.size());
    }

    /** 全量不可变快照。 */
    public List<BizProductDescriptor> listAll() {
        return List.copyOf(byCode.values());
    }

    public Optional<BizProductDescriptor> get(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCode.get(code));
    }

    private static BizProductDescriptor toDescriptor(BizProduct ann) {
        return BizProductDescriptor.builder()
                .code(ann.code())
                .name(ann.name())
                .build();
    }
}
