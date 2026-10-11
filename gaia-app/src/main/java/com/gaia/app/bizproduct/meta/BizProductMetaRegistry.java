package com.gaia.app.bizproduct.meta;

import com.gaia.api.ability.GameDescriptor;
import com.gaia.iframework.annotation.BizProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
 * <p>启动时扫描所有 {@code @BizProduct} 标注的 bean，构造
 * {@code code → GameDescriptor} 索引。放在 gaia-app 是为了能扫到本模块下的
 * {@code TreasureBizProduct}（gaia-backend 按 {@code gaia-app-scope} 约束不依赖
 * gaia-app，所以元数据必须由 gaia-bootstrap 通过 8091 暴露给后台 admin）。</p>
 */
@Slf4j
@Component
public class BizProductMetaRegistry implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final ConcurrentMap<String, GameDescriptor> byCode = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BizProduct.class);
        for (Object bean : beans.values()) {
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
    public List<GameDescriptor> listAll() {
        return List.copyOf(byCode.values());
    }

    public Optional<GameDescriptor> get(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCode.get(code));
    }

    private static GameDescriptor toDescriptor(BizProduct ann) {
        return GameDescriptor.builder()
                .code(ann.code())
                .name(ann.name())
                .build();
    }
}
