package com.gaia.domain.repository;

import com.gaia.domain.member.Member;

import java.util.Optional;

/**
 * 会员 Repository。
 */
public interface MemberRepository {

    Optional<Member> findByUserId(Long userId);

    Member save(Member member);
}