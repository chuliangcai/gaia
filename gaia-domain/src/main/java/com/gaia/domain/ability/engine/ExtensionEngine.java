package com.gaia.domain.ability.engine;

import com.gaia.iframework.annotation.BizProduct;
import com.gaia.iframework.annotation.Extension;
import com.gaia.iframework.core.IAbilityExt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 扩展点引擎。
 *
 * <p>启动时扫描所有 {@link BizProduct}-annotated bean 中的
 * {@link Extension}-annotated 方法并按 (bizCode, extCode) 建立索引，
 * 由 {@link com.gaia.iframework.core.AbstractBaseAbility#reduceExecute(String, String, com.gaia.iframework.core.ExtensionCallback)}
 * 经 {@link AbstractBaseAbility.AbilityExtHolder} 调用。</p>
 *
 * <p>v1 接入 treasure 后，扫描到 1 个 BizProduct（{@code code="treasure"}），
 * 按 {@code extCode} 索引其下全部 {@code @Extension} 方法。</p>
 *
 * <p>本类是 {@code @BizProduct} bean 的<strong>唯一</strong>扫描点，承载两份索引：
 * <ul>
 *   <li>{@code byBiz} — 扩展点索引（{@code bizCode → extCode → (bean, method)}），
 *       由 {@link #lookup(String, String, Class)} 在运行时使用；</li>
 *   <li>{@code byBizCode} — 玩法 bean 索引（{@code bizCode → bean}），
 *       通过 {@link #bizProductSnapshot()} 暴露给同模块的
 *       {@code BizProductMetaRegistry} 订阅，以避免重复扫描。</li>
 * </ul>
 * 两份索引共用一次 {@code getBeansWithAnnotation(BizProduct.class)} 扫描。</p>
 */
@Slf4j
@Component
public class ExtensionEngine implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    /** (bizCode, extCode) -> 持有 bean 与 method 的 holder */
    private final ConcurrentMap<String, ConcurrentMap<String, Holder>> byBiz = new ConcurrentHashMap<>();
    /** bizCode -> @BizProduct bean 自身，供元数据订阅使用。 */
    private final ConcurrentMap<String, Object> byBizCode = new ConcurrentHashMap<>();

    public ExtensionEngine() {
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
            // 与 AbilityEngine 同语义：同一 bizCode 重复时后者被忽略并记 warn。
            Object prev = byBizCode.putIfAbsent(biz, bean);
            if (prev != null) {
                log.warn("duplicate @BizProduct code={}, prev={} ignored={}",
                        biz, prev.getClass().getName(), bean.getClass().getName());
            }
            ConcurrentMap<String, Holder> inner = byBiz.computeIfAbsent(biz,
                    k -> new ConcurrentHashMap<>());
            for (Method m : bean.getClass().getMethods()) {
                Extension e = m.getAnnotation(Extension.class);
                if (e == null) {
                    continue;
                }
                Holder holderPrev = inner.putIfAbsent(e.code(), new Holder(bean, m));
                if (holderPrev != null) {
                    throw new IllegalStateException(
                            "Duplicate @Extension bizCode=" + biz + " extCode=" + e.code());
                }
            }
        }
        log.info("ExtensionEngine initialized, bizProductCount={}", byBizCode.size());
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
     * 当前已注册 {@code @BizProduct} bean 的不可变快照。
     *
     * <p>供同模块的 {@code BizProductMetaRegistry} 在自身 {@code @PostConstruct} 中读取，
     * 以避免重复扫描 {@code @BizProduct} bean。</p>
     *
     * @return bizCode → {@code @BizProduct} bean 的不可变视图
     */
    public Map<String, Object> bizProductSnapshot() {
        return Collections.unmodifiableMap(byBizCode);
    }

    /**
     * 内部值对象：扩展点实现 (bean, method)。
     * 当前版本只对外暴露 bean；method 字段为后续按方法级调用预留。
     */
    private record Holder(Object bean, Method method) {
    }
}
