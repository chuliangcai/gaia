package com.gaia.iframework.core;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 业务执行上下文。
 *
 * <p>由 dispatcher 从 {@link GatewayMtopRequest} 构建并透传给
 * {@link IAbility#execute(RequestContext)}。</p>
 */
@Data
public class RequestContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userNick;
    private String assetType;
    private Map<String, String> extParams;
    private String bizCode;
    private String activityId;
}
