package com.gaia.app.service.impl;

import com.gaia.api.interact.InteractEngineService;
import com.gaia.common.constants.DubboConstants;
import com.gaia.iframework.core.GatewayMtopRequest;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.InteractResultSet;
import com.gaia.app.framework.AbilityDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Map;

/**
 * 互动引擎 RPC 服务实现 — 框架唯一的 Dubbo Provider。
 *
 * <p>gateway 的所有玩法请求统一从此处进入；本实现不进行任何业务计算，
 * 仅委托 {@link AbilityDispatcher} 完成分派与聚合。</p>
 */
@Slf4j
@DubboService(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
@RequiredArgsConstructor
public class InteractEngineServiceImpl implements InteractEngineService {

    private final AbilityDispatcher dispatcher;

    @Override
    public InteractResultSet execute(GatewayMtopRequest req) {
        if (req == null) {
            log.warn("interact entry called with null request");
            InteractResultSet empty = new InteractResultSet();
            empty.setSuccess(false);
            empty.setMsgCode("REQUEST_NULL");
            return empty;
        }
        log.info("interact entry bizCode={} userId={} models={}",
                req.getBizCode(), req.getUserId(),
                req.getModels() == null ? 0 : req.getModels().size());
        Map<String, IFResult<?>> items = dispatcher.dispatch(req);
        boolean allOk = items.values().stream().allMatch(IFResult::isSuccess);
        InteractResultSet out = new InteractResultSet();
        out.setSuccess(allOk);
        if (!allOk) {
            out.setMsgCode("PARTIAL_OR_ALL_FAILED");
        }
        out.setItems(items);
        return out;
    }
}
