package com.gaia.iframework.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 框架为 {@link IAbility} 提供的抽象基类。
 *
 * <p>提供两件事：
 * <ol>
 *     <li>{@link #execute(RequestContext)} 的默认执行骨架：按
 *     {@link #getProcessor(String)} 返回的处理器名顺序，对每个名字通过反射调用
 *     子类同名方法（参数为 {@link RequestContext}、返回
 *     {@link IFResult}），任一步返回失败即短路返回。</li>
 *     <li>通过 {@link #reduceExecute(String, String, ExtensionCallback)}
 *     按 (bizCode, extensionCode) 查找当前玩法下的扩展点实例。</li>
 * </ol>
 * </p>
 *
 * <p>子类无需覆写任何方法：{@link #getProcessor(String)} 声明的每个处理器名
 * 直接对应一个签名匹配的方法（如 {@code preCheck(RequestContext)}），由基类
 * 在 {@link #invokeProcessor(String, RequestContext)} 中反射调用并缓存。
 * 查找不到同名方法时视为配置错误，抛 {@link IllegalStateException}。</p>
 *
 * <p>如需完全自定义流程，仍可覆写 {@link #execute(RequestContext)}。
 * 如需自定义分派（如混入非 {@code (RequestContext) -> IFResult<?>}
 * 形式的方法），仍可覆写 {@link #invokeProcessor(String, RequestContext)}。</p>
 *
 * <p>{@link #reduceExecute(String, String, ExtensionCallback)} 的实现依赖一个
 * 由容器（如 Spring）注册的 {@link AbilityExtHolder#lookup} 静态桥接；
 * 容器侧需要在业务执行前调用 {@link AbilityExtHolder#register(BiFunction)}
 * 注入，否则会抛 {@link IllegalStateException}。这样设计保证基类对容器中立，
 * 可被任何 DI 框架托管。</p>
 */
public abstract class AbstractBaseAbility implements IAbility {

    /**
     * 处理器方法缓存：按处理器名缓存反射到的 {@link Method}，避免每次
     * {@link #invokeProcessor(String, RequestContext)} 都重新反射查找。
     */
    private final Map<String, Method> processorMethodCache = new ConcurrentHashMap<>();

    /**
     * 默认执行骨架：按 {@link #getProcessor(String)} 返回的处理器名顺序，
     * 逐个调用 {@link #invokeProcessor(String, RequestContext)}；
     * 任一步返回失败即短路返回失败结果，否则返回最后一步的成功结果。
     *
     * <p>处理器列表为空时直接返回空成功结果（用于"该 ability 暂无可执行业务"的占位）。</p>
     */
    @Override
    public IFResult<?> execute(RequestContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context 不能为空");
        }
        List<String> processors = getProcessor(context.getBizCode());
        if (processors == null || processors.isEmpty()) {
            return IFResult.success(null);
        }
        IFResult<?> last = IFResult.success(null);
        for (String name : processors) {
            IFResult<?> r = invokeProcessor(name, context);
            if (r == null) {
                throw new IllegalStateException(
                        "处理器 " + name + " 返回了 null 结果");
            }
            if (!r.isSuccess()) {
                return r;
            }
            last = r;
        }
        return last;
    }

    /**
     * 按处理器名反射调用子类同名方法。
     *
     * <p>子类方法签名要求：参数类型为 {@link RequestContext}，返回类型为
     * {@link IFResult}（或其子类型），方法名与 {@link #getProcessor(String)}
     * 返回列表中的条目一致。方法可见性不限（包级私有、私有均可）。</p>
     *
     * <p>首次按名查找到对应 {@link Method} 后会缓存到本实例的
     * {@link #processorMethodCache}。同名方法被多次声明（继承链上重载/隐藏）
     * 或参数类型不匹配时，查找阶段会抛 {@link NoSuchMethodException}
     * （被本方法翻译成 {@link IllegalStateException}）。
     * 由于参数类型精确指定为 {@code RequestContext}，
     * 不会误匹配 {@link Object} 上的方法（如 {@code toString()}）。</p>
     */
    protected IFResult<?> invokeProcessor(String name, RequestContext context) {
        Method m = processorMethodCache.computeIfAbsent(name, this::findProcessorMethod);
        if (m == null) {
            throw new IllegalStateException(
                    "能力 " + getClass().getName() + " 声明了处理器 [" + name
                            + "] 但未声明同名方法 (RequestContext)");
        }
        try {
            return (IFResult<?>) m.invoke(this, context);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new RuntimeException("处理器 " + name + " 执行失败", cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("处理器 " + name + " 不可访问", e);
        }
    }

    /**
     * 在本类上查找与处理器名同名的方法（参数为 {@link RequestContext}）。
     * 找不到返回 {@code null}，由 {@link #invokeProcessor(String, RequestContext)}
     * 翻译为 {@link IllegalStateException}。
     */
    private Method findProcessorMethod(String name) {
        try {
            Method m = getClass().getDeclaredMethod(name, RequestContext.class);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
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
