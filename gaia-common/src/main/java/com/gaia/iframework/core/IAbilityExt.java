package com.gaia.iframework.core;

/**
 * 能力扩展点标记接口。
 *
 * <p>具体扩展点接口继承该接口，并由
 * {@link com.gaia.iframework.annotation.AbilityExtension} 标注。
 * 玩法内的具体扩展由 {@link com.gaia.iframework.annotation.Extension}
 * 在 {@link com.gaia.iframework.annotation.BizProduct}-annotated 类的方法上提供。</p>
 */
public interface IAbilityExt {
}
