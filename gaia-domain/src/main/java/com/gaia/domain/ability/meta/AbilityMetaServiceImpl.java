package com.gaia.domain.ability.meta;

import com.gaia.api.ability.AbilityDescriptor;
import com.gaia.api.ability.AbilityMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * {@link AbilityMetaService} 的领域实现。
 *
 * <p>直接由 {@link AbilityMetaRegistry} 提供只读视图；不做二级缓存，启动期之后结构不变。</p>
 */
@Service
@RequiredArgsConstructor
public class AbilityMetaServiceImpl implements AbilityMetaService {

    private final AbilityMetaRegistry registry;

    @Override
    public List<AbilityDescriptor> listAbilities() {
        return registry.listAll();
    }

    @Override
    public List<AbilityDescriptor> listAbilities(String bizNs) {
        return registry.listByBizNs(bizNs);
    }

    @Override
    public Optional<AbilityDescriptor> findAbility(String code) {
        return registry.get(code);
    }
}
