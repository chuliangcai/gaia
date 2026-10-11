package com.gaia.app.bizproduct.meta;

import com.gaia.api.ability.GameDescriptor;
import com.gaia.api.ability.GameMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link GameMetaService} 的本地实现。
 *
 * <p>委托给本模块内的 {@link BizProductMetaRegistry}。由 gaia-bootstrap 通过
 * Spring 注入（8091 暴露），后台 admin 走前端 proxy 转发到 gaia-bootstrap
 * 拉取玩法元数据 — 因为 {@code TreasureBizProduct} 等 {@code @BizProduct}
 * 实现类只存在于 gaia-app 模块，gaia-backend 不依赖 gaia-app，按
 * {@code gaia-app-scope} 约束保持依赖方向干净。</p>
 */
@Service
@RequiredArgsConstructor
public class GameMetaServiceImpl implements GameMetaService {

    private final BizProductMetaRegistry registry;

    @Override
    public List<GameDescriptor> listGames() {
        return registry.listAll();
    }
}
