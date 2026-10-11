package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 创建活动请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 玩法 code（@BizProduct.code），必填（service 层校验）。 */
    private String bizCode;

    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * 后台 UI 状态：0=草稿 1=上架 2=下架。null 表示用 service 默认值（草稿）。
     * 提交时由 service 映射成领域状态机（{@code ActivityStatus}）：
     * 0→0 NOT_STARTED，1→1 ONGOING，2→3 DRAWN（领域无"下架"概念，复用已开奖态）。
     */
    private Integer status;

    /**
     * 能力配置；key=configClass FQN（如
     * {@code com.gaia.domain.ability.task.config.TaskConfig}），value 为该 FQN
     * 对应配置对象的 JSON 表示。后台保存时序列化进活动的 {@code configurations} 字段。
     */
    private Map<String, Object> configurations;
}
