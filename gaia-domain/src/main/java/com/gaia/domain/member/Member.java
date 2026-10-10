package com.gaia.domain.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员实体。
 *
 * <p>本系统中"会员"与"用户"同义（{@code userId} 是核心标识），
 * 此表主要用于跟踪会员等级、有效期等扩展属性。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String level;

    /** 0=失效,1=有效 */
    private Integer status;

    private LocalDateTime joinAt;
    private LocalDateTime expireAt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isActive() {
        return status != null && status == 1;
    }
}