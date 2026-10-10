package com.gaia.iframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 能力扩展点（Extension Point）标注。
 *
 * <p>标注一个扩展点接口（要求继承
 * {@link com.gaia.iframework.core.IAbilityExt}），
 * 框架以此作为扩展实现的契约查找依据。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AbilityExtension {

    /** 扩展点 code（玩法内唯一） */
    String code();

    /** 扩展点展示名 */
    String name();
}
