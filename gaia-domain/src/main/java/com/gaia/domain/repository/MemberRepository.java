package com.gaia.domain.repository;

import com.gaia.domain.member.Member;

import java.util.List;
import java.util.Optional;

/**
 * 会员 Repository。
 */
public interface MemberRepository {

    Optional<Member> findByUserId(Long userId);

    Member save(Member member);

    /**
     * 当前所有"有效"会员（status=1）的 user_id 列表。
     * 用于"切换用户"面板的候选名单。
     */
    List<Long> listActiveUserIds();

    /**
     * 全量列表（后台管理用，按 id 倒序）。
     */
    List<Member> findAll();

    /**
     * 物理删除（后台管理用，幂等：userId 为 null 直接返回）。
     */
    void deleteByUserId(Long userId);
}