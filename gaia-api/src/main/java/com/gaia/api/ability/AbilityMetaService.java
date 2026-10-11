package com.gaia.api.ability;

import java.util.List;
import java.util.Optional;

/**
 * 能力元数据 RPC 契约。
 *
 * <p>由 backend 端消费，用于后台 admin 动态渲染"玩法 → 能力 → 配置表单"。
 * 实现位于 gaia-domain（{@code AbilityMetaServiceImpl}）。</p>
 */
public interface AbilityMetaService {

    /**
     * 列出全部能力的元数据。
     */
    List<AbilityDescriptor> listAbilities();

    /**
     * 按玩法命名空间过滤。
     *
     * @param bizNs 玩法 code；{@code null} 或空字符串 = 全部
     */
    List<AbilityDescriptor> listAbilities(String bizNs);

    /**
     * 按能力 code 取单条元数据。
     */
    Optional<AbilityDescriptor> findAbility(String code);
}
