package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

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
}
