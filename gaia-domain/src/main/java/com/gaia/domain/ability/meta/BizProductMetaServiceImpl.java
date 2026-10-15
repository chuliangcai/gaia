package com.gaia.domain.ability.meta;

import com.gaia.api.ability.BizProductDescriptor;
import com.gaia.api.ability.BizProductMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link BizProductMetaService} 的领域实现。
 *
 * <p>直接由 {@link BizProductMetaRegistry} 提供只读视图；不做二级缓存，启动期之后结构不变。</p>
 */
@Service
@RequiredArgsConstructor
public class BizProductMetaServiceImpl implements BizProductMetaService {

    private final BizProductMetaRegistry registry;

    @Override
    public List<BizProductDescriptor> listBizProducts() {
        return registry.listAll();
    }
}
