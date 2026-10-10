package com.gaia.iframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 领域命名空间标注。
 *
 * <p>用于将若干相关能力 / 玩法聚合成一个领域单元（如「签到域」、「抽奖域」）。
 * 仅作为可读性的元信息（当前版本不参与路由）。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DomainNs {

    /** 领域展示名 */
    String name();

    /** 领域 code */
    String code();
}
