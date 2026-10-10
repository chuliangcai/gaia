package com.gaia.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gaia.domain.entity.AdminUser;
import com.gaia.domain.repository.AdminUserRepository;
import com.gaia.infra.persistence.mapper.AdminUserMapper;
import com.gaia.infra.persistence.po.AdminUserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 管理员用户 Repository 的基础设施实现。
 *
 * <p>骨架阶段：给出 PO ↔ 领域实体的转换工具方法，
 * 具体 SQL 待表结构固化后补齐。</p>
 */
@Repository
@RequiredArgsConstructor
public class AdminUserRepositoryImpl implements AdminUserRepository {

    private final AdminUserMapper mapper;

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        QueryWrapper<AdminUserPO> qw = new QueryWrapper<>();
        qw.eq("username", username);
        return Optional.ofNullable(mapper.selectOne(qw)).map(AdminUserRepositoryImpl::toEntity);
    }

    @Override
    public AdminUser save(AdminUser adminUser) {
        AdminUserPO po = toPO(adminUser);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        // 回填主键与时间戳
        adminUser.setId(po.getId());
        return adminUser;
    }

    @Override
    public Optional<AdminUser> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(AdminUserRepositoryImpl::toEntity);
    }

    // ----------------------- PO ↔ Domain -----------------------

    static AdminUser toEntity(AdminUserPO po) {
        if (po == null) {
            return null;
        }
        return AdminUser.builder()
                .id(po.getId())
                .username(po.getUsername())
                .passwordHash(po.getPasswordHash())
                .displayName(po.getDisplayName())
                .email(po.getEmail())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    static AdminUserPO toPO(AdminUser e) {
        if (e == null) {
            return null;
        }
        AdminUserPO po = new AdminUserPO();
        po.setId(e.getId());
        po.setUsername(e.getUsername());
        po.setPasswordHash(e.getPasswordHash());
        po.setDisplayName(e.getDisplayName());
        po.setEmail(e.getEmail());
        po.setStatus(e.getStatus());
        po.setCreatedAt(e.getCreatedAt());
        po.setUpdatedAt(e.getUpdatedAt());
        return po;
    }
}
