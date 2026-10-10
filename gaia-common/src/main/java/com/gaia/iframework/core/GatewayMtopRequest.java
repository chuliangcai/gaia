package com.gaia.iframework.core;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 网关收到的玩法请求体（统一入口契约）。
 *
 * <p>由 HTTP 网关透传至 RPC 服务方；包含目标玩法的 {@code bizCode} 与
 * 一次请求内需要调用的若干 {@link ModelRequest}。</p>
 */
@Data
public class GatewayMtopRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 玩法 code — 对应 {@link com.gaia.iframework.annotation.BizProduct#code()} */
    private String bizCode;

    /** 调用方用户 ID */
    private Long userId;

    /** 是否并发执行 model 列表（v1 暂未使用，由 dispatcher 串行执行；保留字段） */
    private Boolean parallel;

    /** 一次请求内需要执行的能力清单 */
    private List<ModelRequest> models;
}
