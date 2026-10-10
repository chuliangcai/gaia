package com.gaia.domain.repository;

import com.gaia.domain.entity.AdminUser;

import java.util.Optional;

/**
 * 管理员用户 Repository 接口。
 *
 * <p>依赖倒置：gaia-infra 给实现，gaia-backend 通过该接口访问数据。</p>
 */
public interface AdminUserRepository {

    /**
     * 按用户名查询（用于登录校验）。
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * 保存（新增或更新）。
     */
    AdminUser save(AdminUser adminUser);

    /**
     * 按主键查询。
     */
    Optional<AdminUser> findById(Long id);
}
