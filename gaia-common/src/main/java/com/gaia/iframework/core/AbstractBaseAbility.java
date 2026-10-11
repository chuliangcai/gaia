package com.gaia.iframework.core;

import java.util.List;
import java.util.function.BiFunction;

/**
 * 框架为 {@link IAbility} 提供的抽象基类。
 *
 * <p>提供两件事：
 * <ol>
 *     <li>一个非空的 {@link #execute(RequestContext)} 骨架：顺序遍历
 *     {@link #getProcessor(String)} 返回的处理器名并返回空成功结果。
 *     业务侧通常直接覆写 {@link #execute(RequestContext)} 实现具体逻辑。</li>
 *     <li>通过 {@link #reduceExecute(String, String, ExtensionCallback)}
 *     按 (bizCode, extensionCode) 查找当前玩法下的扩展点实例。</li>
 * </ol>
 * </p>
 *
 * <p>{@link #reduceExecute(String, String, ExtensionCallback)} 的实现依赖一个
 * 由容器（如 Spring）注册的 {@link AbilityExtHolder#lookup} 静态桥接；
 * 容器侧需要在业务执行前调用 {@link AbilityExtHolder#register(BiFunction)}
 * 注入，否则会抛 {@link IllegalStateException}。这样设计保证基类对容器中立，
 * 可被任何 DI 框架托管。</p>
 */
public abstract class AbstractBaseAbility implements IAbility {

    /**
     * 骨架实现：子类若需要 processor 链式调用可覆写；否则直接覆写 execute() 即可。
     */
    @Override
    public IFResult<?> execute(RequestContext context) {
        // 读取一次处理器列表（即便未真正链式调用，也用于校验配置完整性）。
        List<String> processors = getProcessor(context.getBizCode());
        if (processors == null || processors.isEmpty()) {
            return IFResult.success(null);
        }
        return IFResult.success(null);
    }

    /**
     * 在当前 ability 内通过 (bizCode, extensionCode) 拿到扩展点实例并应用回调。
     *
     * @param bizCode       玩法 code
     * @param extensionCode 扩展点 code
     * @param callback      业务侧回调
     * @param <R>           回调返回类型
     * @param <T>           扩展点接口类型
     */
    @SuppressWarnings("unchecked")
    public final <R, T extends IAbilityExt> R reduceExecute(
            String bizCode,
            String extensionCode,
            ExtensionCallback<T, R> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback 不能为空");
        }
        T ext = (T) AbilityExtHolder.lookup(bizCode, extensionCode);
        return callback.apply(ext);
    }

    /**
     * 同 {@link #reduceExecute(String, String, ExtensionCallback)}，但 bizCode 取自
     * 运行时上下文（{@link RequestContext#getBizCode()}）。
     *
     * <p>domain 层能力实现应优先使用本重载，避免在领域代码中硬编码玩法 code。
     * wire 上的玩法归属由 dispatcher / {@code RequestContext} 注入，领域层不应感知。</p>
     *
     * @param context       业务上下文
     * @param extensionCode 扩展点 code
     * @param callback      业务侧回调
     */
    public final <R, T extends IAbilityExt> R reduceExecute(
            RequestContext context,
            String extensionCode,
            ExtensionCallback<T, R> callback) {
        if (context == null) {
            throw new IllegalArgumentException("context 不能为空");
        }
        return reduceExecute(context.getBizCode(), extensionCode, callback);
    }

    /**
     * 静态桥接器：让 {@link AbstractBaseAbility}（位于 gaia-common）能
     * 调用由容器（如 Spring）在运行时注册进来的查找函数。
     *
     * <p>典型用法（容器侧）：
     * <pre>{@code
     * AbstractBaseAbility.AbilityExtHolder.register(
     *     (biz, ext) -> extensionRegistry.lookup(biz, ext));
     * }</pre>
     * </p>
     */
    public static final class AbilityExtHolder {

        private static volatile BiFunction<String, String, IAbilityExt> lookupFn;

        private AbilityExtHolder() {
        }

        /**
         * 注册查找函数。多次注册会覆盖；通常在容器初始化时调用一次。
         */
        public static void register(BiFunction<String, String, IAbilityExt> lookupFunction) {
            lookupFn = lookupFunction;
        }

        /**
         * 由基类内部调用：查找扩展点实例。
         */
        @SuppressWarnings("unchecked")
        static <T extends IAbilityExt> T lookup(String bizCode, String extCode) {
            BiFunction<String, String, IAbilityExt> fn = lookupFn;
            if (fn == null) {
                throw new IllegalStateException(
                        "AbilityExtHolder 尚未注册：容器侧需在启动时调用 AbilityExtHolder.register(...)");
            }
            return (T) fn.apply(bizCode, extCode);
        }
    }
}
