package com.gaia.gateway.controller;

import com.gaia.api.interact.InteractEngineService;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import com.gaia.iframework.core.GatewayMtopRequest;
import com.gaia.iframework.core.InteractResultSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
     *
     * <p>userId 解析优先级：HttpSession 中保存的 "gaia.currentUserId" &gt; body.userId。
     * 前者由 {@code AuthController#switchUser} 写入，后者保留作为未登录场景下的 fallback。</p>
     */
    @PostMapping
    @Operation(summary = "执行玩法",
            description = "按请求体中 models 列出的能力，逐个分派至 server 端 AbilityDispatcher；"
                    + "返回各能力执行结果的聚合")
    public Result<InteractResultSet> execute(
            @RequestBody GatewayMtopRequest request,
            HttpServletRequest http) {
        if (request == null) {
            log.warn("interact 收到空请求");
            return Result.fail(Result.CodeEnum.BAD_REQUEST);
        }
        Long sessionUserId = AuthController.currentUserId(http);
        Long bodyUserId = request.getUserId();
        Long effectiveUserId = sessionUserId != null ? sessionUserId : bodyUserId;
        if (effectiveUserId != null) {
            request.setUserId(effectiveUserId);
        }
        log.info("interact entry bizCode={} userId={} (session={}, body={}) models={}",
                request.getBizCode(), effectiveUserId,
                sessionUserId, bodyUserId,
                request.getModels() == null ? 0 : request.getModels().size());
        InteractResultSet out = interactEngineService.execute(request);
        return Result.success(out);
    }
}
