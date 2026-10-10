package com.gaia.api.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 会员摘要（切换用户候选列表用）。
 *
 * <p>仅透出前端"切用户"面板所需的最小字段，不暴露任何敏感信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（前端选择后写入 session 的身份） */
    private Long userId;

    /** 会员等级（如 NORMAL / VIP） */
    private String level;

    /** 状态：1=有效，0=失效 */
    private Integer status;
}