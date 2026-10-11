package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员管理后台列表 / 详情 DTO。
 *
 * <p>完整透出领域 {@code Member} 字段，供后台运营查询。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberViewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String level;
    private Integer status;
    private LocalDateTime joinAt;
    private LocalDateTime expireAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
