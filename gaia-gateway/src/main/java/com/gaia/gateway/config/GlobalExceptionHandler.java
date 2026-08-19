package com.gaia.gateway.config;

import com.gaia.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 *
 * <p>将所有 Controller 抛出的异常统一收敛为 {@link Result} 结构，
 * 避免敏感堆栈外泄。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(Result.CodeEnum.BAD_REQUEST));
    }

    /** 非法参数 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArg(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(Result.CodeEnum.BAD_REQUEST.getCode(), e.getMessage()));
    }

    /** Dubbo / 业务超时 */
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<Result<Void>> handleTimeout(java.util.concurrent.TimeoutException e) {
        log.warn("调用超时: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Result.fail(Result.CodeEnum.INTERNAL_ERROR.getCode(), "服务调用超时"));
    }

    /** 兜底异常 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Result<Void>> handleUnknown(Throwable e) {
        log.error("网关层未知异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(Result.CodeEnum.INTERNAL_ERROR));
    }
}