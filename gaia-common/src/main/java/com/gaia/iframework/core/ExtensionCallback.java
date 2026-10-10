package com.gaia.iframework.core;

/**
 * 扩展点回调。
 *
 * <p>用于 {@link AbstractBaseAbility#reduceExecute(String, String, ExtensionCallback)}
 * 中，业务侧传入 lambda 在拿到的扩展实例上做处理。</p>
 *
 * @param <T> 扩展点类型，必须继承 {@link IAbilityExt}
 * @param <R> 回调返回值
 */
@FunctionalInterface
public interface ExtensionCallback<T extends IAbilityExt, R> {

    R apply(T abilityExt);
}
