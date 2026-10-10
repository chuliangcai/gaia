package com.gaia.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gaia.infra.persistence.po.AdminUserPO;

/**
 * 管理员用户 MyBatis-Plus Mapper。
 *
 * <p>仅基础设施层可见：PO（持久化对象）形态由本层决定，
 * 与领域实体 {@link com.gaia.domain.entity.AdminUser} 互不耦合，
 * 二者转换由 {@link com.gaia.infra.persistence.repository.AdminUserRepositoryImpl} 承担。</p>
 */
public interface AdminUserMapper extends BaseMapper<AdminUserPO> {
}
