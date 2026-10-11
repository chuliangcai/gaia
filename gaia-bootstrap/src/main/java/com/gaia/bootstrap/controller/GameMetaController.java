package com.gaia.bootstrap.controller;

import com.gaia.api.ability.GameDescriptor;
import com.gaia.api.ability.GameMetaService;
import com.gaia.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 玩法元数据接口（8091）。
 *
 * <p>由后台 admin 通过前端 proxy（vite proxy 把 {@code /api/admin/meta/games}
 * 转发到这里）拉取"全部玩法"。底层由 gaia-app 的 {@code BizProductMetaRegistry}
 * 在启动时扫 {@code @BizProduct} 注入。</p>
 *
 * <p>注意：必须放在 gaia-bootstrap（与 {@code TreasureBizProduct} 一起）才能扫描到
 * 所有 {@code @BizProduct} bean；gaia-backend 按 {@code gaia-app-scope} 不依赖 gaia-app，
 * 所以元数据接口不能放 gaia-backend。</p>
 */
@RestController
@RequestMapping("/api/admin/meta/games")
@RequiredArgsConstructor
public class GameMetaController {

    private final GameMetaService gameMetaService;

    @GetMapping
    public Result<List<GameDescriptor>> list() {
        return Result.success(gameMetaService.listGames());
    }
}
