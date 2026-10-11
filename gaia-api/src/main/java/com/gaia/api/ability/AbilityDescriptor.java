package com.gaia.api.ability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 能力元数据 DTO（OpenAPI 风格）。
 *
 * <p>由后台 admin 通过 {@link AbilityMetaService} 拉取，用于按玩法+能力动态渲染表单。
 * {@code configSchema} 是按字段反射 configClass 得出的 JSON Schema；该能力没有
 * 独立配置时为 {@code null}（与 {@code configClassName == "java.lang.Void"} 配套识别）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 能力唯一标识 — wire 上的 assertType */
    private String code;

    /** 玩法命名空间 */
    private String bizNs;

    /**
     * 玩法命名空间对应 {@code @DomainNs} 类的中文名（"任务域" / "开奖域" / …）。
     * 未匹配到 {@code @DomainNs} 类或 {@code bizNs} 为空时为 {@code null}。
     */
    private String domainName;

    /** 展示名 */
    private String name;

    /**
     * 配置类 FQN。本能力无配置时为 {@code java.lang.Void}，配合
     * {@link #configSchema} 为 {@code null} 让前端识别。
     */
    private String configClassName;

    /**
     * 配置类的 JSON Schema。结构遵循 OpenAPI：包含 {@code type / properties / items / enum / format} 等；
     * 嵌套深度上限由 introspector 决定。
     */
    private Map<String, Object> configSchema;
}
