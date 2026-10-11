package com.gaia.api.ability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 领域命名空间（{@code @DomainNs}）元数据 DTO。
 *
 * <p>{@code code} 对应 {@code @DomainNs#code()}，亦是 {@code @Ability#bizNs()}。
 * {@code name} 是中文展示名（如 {@code "任务域"}），由后台 admin
 * 在「能力分组」卡片标题处使用。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 领域唯一标识 — 与 {@code @Ability#bizNs()} 对齐。 */
    private String code;

    /** 领域中文展示名。 */
    private String name;
}
