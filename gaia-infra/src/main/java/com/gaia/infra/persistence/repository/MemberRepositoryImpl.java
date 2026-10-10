package com.gaia.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gaia.domain.member.Member;
import com.gaia.domain.repository.MemberRepository;
import com.gaia.infra.persistence.mapper.MemberMapper;
import com.gaia.infra.persistence.po.MemberPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 会员 Repository 实现。
 */
@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberMapper mapper;

    @Override
    public Optional<Member> findByUserId(Long userId) {
        QueryWrapper<MemberPO> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        return Optional.ofNullable(mapper.selectOne(qw)).map(MemberRepositoryImpl::toEntity);
    }

    @Override
    public Member save(Member member) {
        MemberPO po = toPO(member);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        member.setId(po.getId());
        return member;
    }

    @Override
    public List<Long> listActiveUserIds() {
        QueryWrapper<MemberPO> qw = new QueryWrapper<>();
        qw.select("DISTINCT user_id")
                .eq("status", 1)
                .isNotNull("user_id")
                .orderByAsc("user_id");
        return mapper.selectList(qw).stream()
                .map(po -> po.getUserId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    // ----------------------- PO ↔ Domain -----------------------

    static Member toEntity(MemberPO po) {
        if (po == null) {
            return null;
        }
        return Member.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .level(po.getLevel())
                .status(po.getStatus())
                .joinAt(po.getJoinAt())
                .expireAt(po.getExpireAt())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    static MemberPO toPO(Member e) {
        if (e == null) {
            return null;
        }
        MemberPO po = new MemberPO();
        po.setId(e.getId());
        po.setUserId(e.getUserId());
        po.setLevel(e.getLevel());
        po.setStatus(e.getStatus());
        po.setJoinAt(e.getJoinAt());
        po.setExpireAt(e.getExpireAt());
        po.setCreateTime(e.getCreateTime());
        po.setUpdateTime(e.getUpdateTime());
        return po;
    }
}