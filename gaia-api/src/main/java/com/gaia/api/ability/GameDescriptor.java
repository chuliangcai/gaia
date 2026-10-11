package com.gaia.api.ability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 玩法（{@code @BizProduct}）元数据 DTO。
 *
 * <p>由后台 admin 通过 {@link GameMetaService} 拉取，用于活动创建/编辑页面的
 * "玩法"下拉选择。code 对应 {@code @BizProduct#code()}，name 对应
 * {@code @BizProduct#name()}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 玩法唯一标识 — wire 上的 bizCode。 */
    private String code;

    /** 玩法展示名。 */
    private String name;
}
