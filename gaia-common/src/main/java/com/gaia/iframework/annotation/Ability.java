package com.gaia.iframework.annotation;

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
 * <p>本注解放在通用框架层：能力作为 wire 上的可寻址契约（{@code assertType}）属于
 * 框架协议的一部分，需要被 gaia-common 自身（{@code ModelRequest} 等）反向引用，
 * 因此不应留在领域层。</p>
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

    /**
     * 该能力的配置 DTO 类型（可选）。
     *
     * <p>用于指向承载本能力配置/契约的 {@code Class}，例如玩法特有的开关、阈值、
     * 规则集等运行时配置。框架不强制该类有共同父接口。</p>
     *
     * <p>本能力无独立配置时填 {@link Void#TYPE}（默认值）。</p>
     */
    Class<?> configClass() default Void.class;
}
