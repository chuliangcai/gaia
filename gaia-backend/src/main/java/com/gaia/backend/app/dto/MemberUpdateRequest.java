package com.gaia.backend.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 更新会员请求。
 *
 * <p>三个字段全部可选，{@code null} 表示"不修改"。典型用法：单独传
 * {@code status=0} 做停用、单独传 {@code expireAt} 续期。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String level;
    private Integer status;
    private LocalDateTime expireAt;
}
