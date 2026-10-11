package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 活动管理后台列表 DTO。
 *
 * <p>适配前端管理后台字段约定：link/status(0=草稿 1=上架 2=下架)，
 * 字段从 domain Activity 映射而来（status 由领域状态机 0/1/2/3 转译）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityViewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 玩法 code（@BizProduct.code）。 */
    private String bizCode;

    private String name;
    private String link;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;

    /** 反序列化后的能力配置（key=configClass FQN）。 */
    private Map<String, Object> configurations;
}
