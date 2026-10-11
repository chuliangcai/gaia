package com.gaia.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 后台管理进程健康检查（骨架阶段）。
 *
 * <p>仅用于验证进程可达，后续会替换为登录、用户管理等接口。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "module", "gaia-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
