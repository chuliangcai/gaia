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

    /**
     * 是否允许后台管理端人工触发该能力。
     *
     * <p>默认 {@code false}：能力只通过 wire 上的 dispatcher / {@code assertType}
     * 路径被调用，不会出现在后台"触发能力"选择列表中，也不接受管理端触发请求。</p>
     *
     * <p>当且仅当该值为 {@code true} 时：
     * <ul>
     *   <li>{@code AbilityMetaRegistry} 透出的 {@code AbilityDescriptor.backendTriggerable}
     *   字段为 {@code true}；</li>
     *   <li>{@code /api/admin/meta/abilities/triggerable} 端点会包含该能力；</li>
     *   <li>管理端 {@code POST /api/admin/activity/{id}/ability/{code}} 会
     *   真正调用 {@link com.gaia.iframework.core.IAbility#execute}。</li>
     * </ul>
     * </p>
     *
     * <p>设置该值为 {@code true} 前，请确保该能力的 {@code execute(ctx)} 语义
     * 与"无用户上下文、由系统/管理员触发"的场景自洽（多数能力仍需要
     * {@code userId / requestId} 等入参，强行启用会让运行期 preCheck 短路）。</p>
     */
    boolean backendTriggerable() default false;
}
