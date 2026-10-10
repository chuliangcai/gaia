package com.gaia.server.framework;

import com.gaia.iframework.annotation.BizProduct;
import com.gaia.iframework.annotation.Extension;
import com.gaia.iframework.core.IAbilityExt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 扩展点注册表。
 *
 * <p>启动时扫描所有 {@link BizProduct}-annotated bean 中的
 * {@link Extension}-annotated 方法并按 (bizCode, extCode) 建立索引，
 * 由 {@link com.gaia.iframework.core.AbstractBaseAbility#reduceExecute(String, String, com.gaia.iframework.core.ExtensionCallback)}
 * 经 {@link AbstractBaseAbility.AbilityExtHolder} 调用。</p>
 *
 * <p>v1 当前没有具体 {@code @BizProduct}，所以扫描结果应为空但索引结构就位。</p>
 */
@Slf4j
@Component
public class ExtensionRegistry implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    /** (bizCode, extCode) -> 持有 bean 与 method 的 holder */
    private final ConcurrentMap<String, ConcurrentMap<String, Holder>> byBiz = new ConcurrentHashMap<>();

    public ExtensionRegistry() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BizProduct.class);
        for (Object bean : beans.values()) {
            String biz = bean.getClass().getAnnotation(BizProduct.class).code();
            ConcurrentMap<String, Holder> inner = byBiz.computeIfAbsent(biz,
                    k -> new ConcurrentHashMap<>());
            for (Method m : bean.getClass().getMethods()) {
                Extension e = m.getAnnotation(Extension.class);
                if (e == null) {
                    continue;
                }
                Holder prev = inner.putIfAbsent(e.code(), new Holder(bean, m));
                if (prev != null) {
                    throw new IllegalStateException(
                            "Duplicate @Extension bizCode=" + biz + " extCode=" + e.code());
                }
            }
        }
        log.info("ExtensionRegistry initialized, bizProductCount={}", byBiz.size());
    }

    /**
     * 按 (bizCode, extCode) 查找扩展点实例。
     *
     * @param bizCode    玩法 code
     * @param extCode    扩展点 code
     * @param extType    期望的扩展点接口类型（运行时校验）
     * @param <T>        扩展点接口
     * @return bean 实例（其类型必然实现 extType）
     */
    @SuppressWarnings("unchecked")
    public <T extends IAbilityExt> T lookup(String bizCode, String extCode, Class<T> extType) {
        ConcurrentMap<String, Holder> inner = byBiz.get(bizCode);
        if (inner == null) {
            throw new IllegalStateException("no BizProduct bizCode=" + bizCode);
        }
        Holder h = inner.get(extCode);
        if (h == null) {
            throw new IllegalStateException(
                    "no @Extension bizCode=" + bizCode + " extCode=" + extCode);
        }
        if (extType != null && !extType.isInstance(h.bean)) {
            throw new IllegalStateException(
                    "extension impl class=" + h.bean.getClass().getName()
                            + " is not instance of " + extType.getName());
        }
        return (T) h.bean;
    }

    /**
     * 内部值对象：扩展点实现 (bean, method)。
     * 当前版本只对外暴露 bean；method 字段为后续按方法级调用预留。
     */
    private record Holder(Object bean, Method method) {
    }
}
