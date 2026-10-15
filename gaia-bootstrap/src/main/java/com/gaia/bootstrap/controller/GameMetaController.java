package com.gaia.bootstrap.controller;

import com.gaia.api.ability.BizProductDescriptor;
import com.gaia.api.ability.BizProductMetaService;
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
 * 转发到这里）拉取"全部玩法"。底层由 gaia-domain 的 {@code ExtensionEngine} 在启动时
 * 扫 {@code @BizProduct} 注入 {@code BizProductMetaRegistry}。</p>
 *
 * <p>注意：{@code @BizProduct} 实现类（{@code TreasureBizProduct}）位于 gaia-app，
 * 必须放在 gaia-bootstrap（依赖 gaia-app）才能扫描到；gaia-backend 按 {@code gaia-app-scope}
 * 不依赖 gaia-app，所以元数据接口不能放 gaia-backend。</p>
 */
@RestController
@RequestMapping("/api/admin/meta/games")
@RequiredArgsConstructor
public class GameMetaController {

    private final BizProductMetaService bizProductMetaService;

    @GetMapping
    public Result<List<BizProductDescriptor>> list() {
        return Result.success(bizProductMetaService.listBizProducts());
    }
}
