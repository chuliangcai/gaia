package com.gaia.iframework.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 框架统一业务结果。
 *
 * <p>作为 RPC 上的传输对象，{@code model} 经泛型擦除后在 wire 上为 {@link Object}。
 * 当 {@link #success} 为 {@code false} 时，{@link #msgCode} 必填，
 * {@link #model} 可携带错误细节（可空）。</p>
 *
 * @param <T> 业务数据类型（运行时擦除为 {@link Object}）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IFResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务是否成功 */
    private boolean success;

    /** 业务消息码：成功时为 {@code null}，失败时携带原因（如 {@code ACTIVITY_NOT_FOUND}） */
    private String msgCode;

    /** 业务数据 */
    private T model;

    /**
     * 成功结果（model 为 {@code null}）。
     */
    public static <T> IFResult<T> success() {
        return new IFResult<>(true, null, null);
    }

    /**
     * 成功结果（携带 model）。
     */
    public static <T> IFResult<T> success(T model) {
        return new IFResult<>(true, null, model);
    }

    /**
     * 失败结果（携带 msgCode，model 为 {@code null}）。
     */
    public static <T> IFResult<T> fail(String msgCode) {
        return new IFResult<>(false, msgCode, null);
    }

    /**
     * 失败结果（携带 msgCode 与 model）。
     */
    public static <T> IFResult<T> fail(String msgCode, T model) {
        return new IFResult<>(false, msgCode, model);
    }
}
