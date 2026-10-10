package com.gaia.gateway.controller;

import com.gaia.api.interact.InteractEngineService;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import com.gaia.iframework.core.GatewayMtopRequest;
import com.gaia.iframework.core.InteractResultSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 玩法统一入口 — gateway 的唯一 HTTP 互动入口。
 *
 * <p>本 controller 不做任何业务计算，仅透传 {@link GatewayMtopRequest}
 * 至 server 端的 {@link InteractEngineService}，由其上的
 * {@code AbilityDispatcher} 完成能力分派。</p>
 *
 * <p>所有由框架驱动的玩法功能均通过 {@code POST /api/interact} 触达，
 * 旧的 {@code /api/sign/*} 等 per-biz 路由已下线。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/interact")
@RequiredArgsConstructor
@Tag(name = "玩法统一入口", description = "互动玩法框架的唯一 HTTP 入口")
public class InteractController {

    @DubboReference(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
    private InteractEngineService interactEngineService;

    /**
     * 执行一笔玩法请求。
     */
    @PostMapping
    @Operation(summary = "执行玩法",
            description = "按请求体中 models 列出的能力，逐个分派至 server 端 AbilityDispatcher；"
                    + "返回各能力执行结果的聚合")
    public Result<InteractResultSet> execute(
            @RequestBody GatewayMtopRequest request) {
        if (request == null) {
            log.warn("interact 收到空请求");
            return Result.fail(Result.CodeEnum.BAD_REQUEST);
        }
        log.info("interact entry bizCode={} userId={} models={}",
                request.getBizCode(), request.getUserId(),
                request.getModels() == null ? 0 : request.getModels().size());
        InteractResultSet out = interactEngineService.execute(request);
        return Result.success(out);
    }
}
