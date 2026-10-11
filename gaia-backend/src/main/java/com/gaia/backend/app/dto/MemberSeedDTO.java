package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 灌会员数据 DTO（后台下发）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSeedDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String level;
    private Integer status;
    private LocalDateTime joinAt;
    private LocalDateTime expireAt;
}
