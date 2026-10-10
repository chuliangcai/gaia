package com.gaia.api.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动管理后台列表 DTO。
 *
 * <p>适配前端管理后台字段约定：coverUrl/link/status(0=草稿 1=上架 2=下架)，
 * 字段从 domain Activity 映射而来（status 由领域状态机 0/1/2/3 转译）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityViewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String activityCode;
    private String name;
    private String coverUrl;
    private String link;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}