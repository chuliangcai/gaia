package com.gaia.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员用户（领域实体）。
 *
 * <p>本期仅作为后台管理域的骨架示例，承载 gaia-backend 后续登录、权限管理等功能。
 * 字段最终根据后台管理实际诉求调整（角色、状态、最后登录时间等）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {

    /** 主键 */
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 密码哈希（不在领域内加密，转换在 infra 层完成） */
    private String passwordHash;

    /** 显示名 */
    private String displayName;

    /** 邮箱 */
    private String email;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
