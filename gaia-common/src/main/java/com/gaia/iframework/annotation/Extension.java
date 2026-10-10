package com.gaia.iframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扩展实现标注。
 *
 * <p>标注 {@link BizProduct}-annotated 类中的方法，表示该方法对某个
 * {@link AbilityExtension}-annotated 扩展点接口在该玩法下的具体实现。
 * 框架启动时按 (bizCode, extCode) 对其进行索引。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Extension {

    /** 对应的扩展点 code */
    String code();

    /** 展示名 */
    String name();
}
