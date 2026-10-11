package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 更新活动请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String prizeDesc;
    private String prizeImgUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer winnerCount;

    /**
     * 玩法 code（@BizProduct.code）；null 表示不变。
     */
    private String bizCode;

    /**
     * 能力配置；语义同 {@link ActivityCreateRequest#getConfigurations()}。
     * 字段为 null 时不覆盖已有配置（保留）。
     */
    private Map<String, Object> configurations;
}
