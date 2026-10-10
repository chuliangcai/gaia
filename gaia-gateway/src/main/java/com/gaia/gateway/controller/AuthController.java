package com.gaia.gateway.controller;

import com.gaia.api.member.MemberQueryService;
import com.gaia.api.member.MemberSummaryDTO;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 鉴权 / 当前用户控制。
 *
 * <p>提供三个 dev 用接口以支撑会员页"切用户"面板：</p>
 * <ul>
 *   <li>{@code GET  /api/auth/me}    — 当前会话用户（401 表示未切过）</li>
 *   <li>{@code GET  /api/auth/users} — 可切换的会员用户候选（来自 gaia_member）</li>
 *   <li>{@code POST /api/auth/switch} — 把指定 userId 写入 HttpSession</li>
 * </ul>
 *
 * <p>会话由 Spring Session 自动写入 Redis，
 * 之后 {@code /api/interact} 会以 session.userId 为准（覆盖请求体 userId）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "当前用户", description = "dev 用切用户面板的 HTTP 入口")
public class AuthController {

    /** Session 中保存当前用户 ID 的属性 key */
    public static final String SESSION_USER_ID = "gaia.currentUserId";

    @DubboReference(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
    private MemberQueryService memberQueryService;

    @GetMapping("/me")
    @Operation(summary = "读取当前会话用户",
            description = "返回当前 HttpSession 中保存的 userId；未切过则返回 401")
    public Result<MeView> me(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) {
            return Result.fail(Result.CodeEnum.UNAUTHORIZED);
        }
        return Result.success(new MeView(userId));
    }

    @GetMapping("/users")
    @Operation(summary = "列出可切换的会员用户")
    public Result<List<MemberSummaryDTO>> listUsers() {
        return Result.success(memberQueryService.listActiveMembers());
    }

    @PostMapping("/switch")
    @Operation(summary = "切换当前会话用户",
            description = "把 userId 写入 HttpSession；后续 /api/interact 即以此用户身份执行")
    public Result<MeView> switchUser(@RequestBody SwitchRequest body, HttpServletRequest request) {
        if (body == null || body.getUserId() == null || body.getUserId() <= 0) {
            return Result.fail(Result.CodeEnum.BAD_REQUEST);
        }
        Long userId = body.getUserId();
        // ensure session exists；Spring Session 会自动下发 Set-Cookie
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_ID, userId);
        log.info("切换当前会话用户 userId={} sessionId={}", userId, session.getId());
        return Result.success(new MeView(userId));
    }

    /**
     * 从 HttpSession 读取当前 userId；session 不存在或属性未设时返回 null。
     * 与 {@link InteractController} 共用同一段逻辑（静态方法暴露）。
     */
    public static Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object attr = session.getAttribute(SESSION_USER_ID);
        if (attr instanceof Long l && l > 0) {
            return l;
        }
        return null;
    }

    /** GET /api/auth/me 返回体 */
    public record MeView(Long userId) {}

    /** POST /api/auth/switch 请求体 */
    @lombok.Data
    public static class SwitchRequest {
        private Long userId;
    }
}