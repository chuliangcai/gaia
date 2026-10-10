package com.gaia.iframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 玩法（Business Product）实现标注。
 *
 * <p>标注一个具体的玩法实现类。{@link #code()} 与 {@code GatewayMtopRequest.bizCode}
 * 对应；类内的 {@link Extension}-annotated 方法被框架收集为该玩法下的扩展实现。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BizProduct {

    /** 玩法唯一标识 — wire 上的 bizCode */
    String code();

    /** 玩法展示名 */
    String name();
}
