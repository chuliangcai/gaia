package com.gaia.gateway.controller;

import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import com.gaia.iframework.core.GatewayMtopRequest;
import com.gaia.iframework.core.InteractResultSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 玩法统一入口 — gateway 的唯一 HTTP 互动入口。
 *
 * <p>本 controller 不做任何业务计算，仅透传 {@link GatewayMtopRequest}
 * 至 server 端的 {@code InteractEngineService}，由其上的
 * {@code AbilityDispatcher} 完成能力分派。</p>
 *
 * <p>所有由框架驱动的玩法功能均通过 {@code POST /api/interact} 触达，
 * 旧的 {@code /api/sign/*} 等 per-biz 路由已下线。</p>
 *
 * <p><b>Dubbo 调用方式：泛化调用（{@link GenericService#$invoke}）。</b>
 * 选用泛化调用的原因：response 中嵌套的 {@code TaskDTO} / {@code TaskExecuteResultDTO}
 * 等领域 DTO 在 Dubbo 3.2 STRICT 反序列化安全检查下默认不在 allowlist 中，
 * 直接 typed 引用会触发 {@code Serialization Security} 异常。改成
 * 泛化调用后，consumer 端只持有 {@code Map<String,Object>}，反序列化由
 * 框架按 Map 路径处理，绕开业务 DTO 的 allowlist 检查。
 */
@Slf4j
@RestController
@RequestMapping("/api/interact")
@RequiredArgsConstructor
@Tag(name = "玩法统一入口", description = "互动玩法框架的唯一 HTTP 入口")
public class InteractController {

    /** 目标服务在注册中心暴露的接口 FQN（泛化调用必须显式指定） */
    private static final String TARGET_SERVICE = "com.gaia.api.interact.InteractEngineService";

    /** 目标方法名（与 {@code InteractEngineService#execute} 一致） */
    private static final String TARGET_METHOD = "execute";

    /**
     * 泛化调用代理：返回值为 {@link Object}（泛型模式下通常是 {@code Map}），
     * 因此 consumer 端不参与业务 DTO 的反序列化。
     *
     * <p>Dubbo 3.2+ 会按"注入类型 = {@link GenericService}"自动启用泛化调用，
     * 无需显式设 {@code generic = true}（该字段已 deprecated）。</p>
     */
    @DubboReference(
            interfaceName = TARGET_SERVICE,
            group = DubboConstants.GROUP,
            version = DubboConstants.VERSION)
    private GenericService interactEngineService;

    private final ObjectMapper objectMapper;

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

        // 泛化调用：parameterTypes 用 FQN 字符串描述，args 直接传 POJO；
        // 返回值在 generic=true (Map 模式) 下为 Map<String,Object>，
        // 不触发业务 DTO 的 STRICT allowlist 检查。
        Object raw = interactEngineService.$invoke(
                TARGET_METHOD,
                new String[]{GatewayMtopRequest.class.getName()},
                new Object[]{request});

        InteractResultSet out = toResultSet(raw);
        return Result.success(out);
    }

    /**
     * 把泛化调用返回的 {@code Map<String,Object>} 还原为 {@link InteractResultSet}。
     *
     * <p>{@code items} 在 wire 上是 {@code Map<String, IFResult>}，但泛化模式
     * 反序列化只到 {@code Map} 层——直接用 {@code convertValue(targetType)} 会
     * 把 {@code items} 的 value 当 {@code IFResult} 转换，失败时回退为
     * raw Map 的浅拷贝，保证响应 JSON 形状不变。</p>
     */
    private InteractResultSet toResultSet(Object raw) {
        if (raw == null) {
            InteractResultSet empty = new InteractResultSet();
            empty.setSuccess(false);
            empty.setMsgCode("EMPTY_RESPONSE");
            return empty;
        }
        if (raw instanceof InteractResultSet already) {
            return already;
        }
        try {
            return objectMapper.convertValue(raw, InteractResultSet.class);
        } catch (IllegalArgumentException ex) {
            log.warn("interact generic result 转换 InteractResultSet 失败，按 raw Map 返回: {}",
                    ex.getMessage());
            // 兜底：手撸一个 result set，至少把 success/msgCode/items 透传出去，
            // 移动端 pickAbilityResult 仍能按 items 字典取到各 ability 的 result。
            InteractResultSet fallback = new InteractResultSet();
            if (raw instanceof java.util.Map<?, ?> m) {
                Object ok = m.get("success");
                Object msg = m.get("msgCode");
                Object items = m.get("items");
                if (ok instanceof Boolean b) fallback.setSuccess(b);
                if (msg instanceof String s) fallback.setMsgCode(s);
                if (items instanceof java.util.Map<?, ?> im) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, com.gaia.iframework.core.IFResult<?>> casted =
                            (java.util.Map<String, com.gaia.iframework.core.IFResult<?>>) im;
                    fallback.setItems(casted);
                }
            }
            return fallback;
        }
    }
}
