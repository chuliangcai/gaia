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
}