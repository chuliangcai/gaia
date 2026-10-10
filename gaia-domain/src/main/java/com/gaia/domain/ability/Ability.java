package com.gaia.domain.ability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 能力（Capability）标注。
 *
 * <p>标注一个实现 {@code com.gaia.iframework.core.IAbility} 的类，
 * 由框架启动时扫描并按 {@link #code()} 注册到能力索引中。
 * 客户端请求体中的 {@code ModelRequest.assertType} 即对应该 {@link #code()}。</p>
 *
 * <p>本注解属于领域层元数据：能力（Capability）作为业务抽象可在多个域复用，
 * 不应与 iframework 通用件混在同一层。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Ability {

    /** 能力所属玩法命名空间（可选） */
    String bizNs() default "";

    /** 能力唯一标识 — wire 上的 assertType */
    String code();

    /** 能力展示名 */
    String name();
}