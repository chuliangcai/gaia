package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 新增会员请求。
 *
 * <p>{@code userId} 必填，其余字段缺省时由 AppService 兜底
 * （level 默认 NORMAL，status 默认 1，joinAt 默认当前时间）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String level;
    private Integer status;
    private LocalDateTime joinAt;
    private LocalDateTime expireAt;
}
