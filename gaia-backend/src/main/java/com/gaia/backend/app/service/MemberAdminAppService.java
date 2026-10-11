package com.gaia.backend.app.service;

import com.gaia.backend.app.dto.MemberCreateRequest;
import com.gaia.backend.app.dto.MemberUpdateRequest;
import com.gaia.backend.app.dto.MemberViewDTO;
import com.gaia.domain.member.Member;
import com.gaia.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会员管理后台应用服务。
 *
 * <p>直接编排 gaia-domain 的 {@link MemberRepository}，不再走 Dubbo。
 * 与活动管理后台的形态完全一致：Controller → AppService → Domain。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAdminAppService {

    private final MemberRepository memberRepository;

    public List<MemberViewDTO> listMembers(Integer status, String level) {
        return memberRepository.findAll().stream()
                .filter(m -> status == null || status.equals(m.getStatus()))
                .filter(m -> level == null || level.equalsIgnoreCase(m.getLevel()))
                .map(MemberAdminAppService::toView)
                .collect(Collectors.toList());
    }

    public MemberViewDTO getMember(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 必填");
        }
        Member m = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("会员不存在 userId=" + userId));
        return toView(m);
    }

    public Long createMember(MemberCreateRequest req) {
        if (req == null || req.getUserId() == null) {
            throw new IllegalArgumentException("userId 必填");
        }
        if (memberRepository.findByUserId(req.getUserId()).isPresent()) {
            throw new IllegalStateException("会员已存在 userId=" + req.getUserId());
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        Member entity = Member.builder()
                .userId(req.getUserId())
                .level(req.getLevel() == null ? "NORMAL" : req.getLevel())
                .status(req.getStatus() == null ? 1 : req.getStatus())
                .joinAt(req.getJoinAt() == null ? now : req.getJoinAt())
                .expireAt(req.getExpireAt())
                .build();
        Member saved = memberRepository.save(entity);
        log.info("创建会员 userId={} level={}", saved.getUserId(), saved.getLevel());
        return saved.getUserId();
    }

    public void updateMember(Long userId, MemberUpdateRequest req) {
        if (userId == null || req == null) {
            throw new IllegalArgumentException("userId 与 req 必填");
        }
        Member m = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("会员不存在 userId=" + userId));
        if (req.getLevel() != null) {
            m.setLevel(req.getLevel());
        }
        if (req.getStatus() != null) {
            m.setStatus(req.getStatus());
        }
        if (req.getExpireAt() != null) {
            m.setExpireAt(req.getExpireAt());
        }
        memberRepository.save(m);
        log.info("更新会员 userId={}", userId);
    }

    public void deleteMember(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 必填");
        }
        memberRepository.deleteByUserId(userId);
        log.info("删除会员 userId={}", userId);
    }

    /**
     * 领域 Member → 后台视图 DTO。
     */
    static MemberViewDTO toView(Member m) {
        return MemberViewDTO.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .level(m.getLevel())
                .status(m.getStatus())
                .joinAt(m.getJoinAt())
                .expireAt(m.getExpireAt())
                .createTime(m.getCreateTime())
                .updateTime(m.getUpdateTime())
                .build();
    }
}
