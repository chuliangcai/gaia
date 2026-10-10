package com.gaia.app.service.impl;

import com.gaia.api.member.MemberQueryService;
import com.gaia.api.member.MemberSummaryDTO;
import com.gaia.common.constants.DubboConstants;
import com.gaia.domain.member.Member;
import com.gaia.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会员查询 Dubbo Provider。
 *
 * <p>被 gateway 的 AuthController 调用，用于支撑"切换用户"面板。</p>
 */
@Slf4j
@DubboService(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
@RequiredArgsConstructor
public class MemberQueryServiceImpl implements MemberQueryService {

    /** dev 演示用户：保证切换面板非空，便于本地首次启动就能用 */
    private static final List<long[]> DEV_DEMO_USER_IDS = List.of(
            new long[]{10001L},
            new long[]{10002L},
            new long[]{10003L}
    );

    private final MemberRepository memberRepository;

    @Override
    public List<MemberSummaryDTO> listActiveMembers() {
        List<Long> userIds = memberRepository.listActiveUserIds();
        if (userIds.isEmpty()) {
            log.info("gaia_member 无有效记录，自动 seed {} 个 dev 演示用户", DEV_DEMO_USER_IDS.size());
            seedDevDemo();
            userIds = memberRepository.listActiveUserIds();
        }
        return userIds.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private MemberSummaryDTO toSummary(Long userId) {
        Member m = memberRepository.findByUserId(userId).orElse(null);
        return MemberSummaryDTO.builder()
                .userId(userId)
                .level(m == null ? null : m.getLevel())
                .status(m == null ? null : m.getStatus())
                .build();
    }

    private void seedDevDemo() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        for (long[] entry : DEV_DEMO_USER_IDS) {
            Long userId = entry[0];
            // 仅当不存在时插入，避免与已灌入数据冲突
            if (memberRepository.findByUserId(userId).isPresent()) {
                continue;
            }
            String level = userId % 2 == 1 ? "VIP" : "NORMAL";
            memberRepository.save(Member.builder()
                    .userId(userId)
                    .level(level)
                    .status(1)
                    .joinAt(now)
                    .build());
        }
    }
}