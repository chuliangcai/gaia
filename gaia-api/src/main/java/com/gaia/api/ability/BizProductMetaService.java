package com.gaia.api.ability;

import java.util.List;

/**
 * 玩法（{@code @BizProduct}）元数据查询服务。
 *
 * <p>由 gaia-domain 的实现类（{@code BizProductMetaServiceImpl}）提供，
 * 供后台 admin 拉取"所有玩法"用于活动创建表单的玩法下拉。</p>
 */
public interface BizProductMetaService {

    /** 返回所有玩法元数据（{@code @BizProduct} 扫描结果）。 */
    List<BizProductDescriptor> listBizProducts();
}
