package com.gaia.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一返回包装类。
 *
 * <p>所有 HTTP 接口与 Dubbo 调用方均使用该对象作为业务结果载体，
 * 保证协议层与业务层数据结构一致。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码：0 表示成功，其它参见 {@link CodeEnum} */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public static <T> Result<T> success() {
        return new Result<>(CodeEnum.SUCCESS.getCode(), CodeEnum.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CodeEnum.SUCCESS.getCode(), CodeEnum.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(CodeEnum.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(CodeEnum codeEnum) {
        return new Result<>(codeEnum.getCode(), codeEnum.getMessage(), null);
    }

    public boolean isSuccess() {
        return this.code == CodeEnum.SUCCESS.getCode();
    }

    /**
     * 业务状态码枚举。
     */
    public enum CodeEnum {
        SUCCESS(0, "success"),
        BAD_REQUEST(400, "请求参数不合法"),
        UNAUTHORIZED(401, "未登录或登录已过期"),
        FORBIDDEN(403, "无访问权限"),
        NOT_FOUND(404, "资源不存在"),
        CONFLICT(409, "资源冲突"),
        TOO_MANY_REQUESTS(429, "请求过于频繁"),
        INTERNAL_ERROR(500, "系统繁忙，请稍后再试"),
        SIGN_ALREADY(1001, "今日已签到"),
        SIGN_LOCK_FAIL(1002, "签到处理中，请稍后重试");

        private final int code;
        private final String message;

        CodeEnum(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}