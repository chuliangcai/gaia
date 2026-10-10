package com.gaia.api.interact;

import com.gaia.iframework.core.GatewayMtopRequest;
import com.gaia.iframework.core.InteractResultSet;

/**
 * 玩法互动引擎 — 框架唯一的 RPC 入口契约。
 *
 * <p>所有由 gateway 收口的玩法请求最终都通过该接口转发至 server 端，
 * 由 {@code AbilityDispatcher} 分派并聚合各能力执行结果。</p>
 */
public interface InteractEngineService {

    /**
     * 执行一笔玩法请求。
     *
     * @param request 网关透传的请求体（持有 {@code bizCode} 与若干 {@code ModelRequest}）
     * @return 聚合结果（含每个能力 code 的执行结果）
     */
    InteractResultSet execute(GatewayMtopRequest request);
}
