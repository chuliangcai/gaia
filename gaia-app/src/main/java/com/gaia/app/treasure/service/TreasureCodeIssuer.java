package com.gaia.app.treasure.service;

import com.gaia.domain.service.treasure.TreasureCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 夺宝码生成服务（纯生成，无锁 / 无落库 / 无缓存）。
 *
 * <p>本服务只负责按需生成 {@code count} 个候选夺宝码，<strong>不</strong>写权益账本；
 * 账本写入由 {@link com.gaia.domain.ability.task.TaskExecuteAbility#issueReward}
 * 在拿到扩展点返回的 {@code List<RewardIssueResult>} 后统一完成。</p>
 *
 * <p>生成阶段不会触发唯一约束（{@link TreasureCodeGenerator#generateOne()} 不查库），
 * 冲突由落库层捕获并重试。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TreasureCodeIssuer {

    private final TreasureCodeGenerator generator;

    /**
     * 生成 {@code count} 个夺宝码候选（{@code count <= 0} 时返回空列表）。
     *
     * @return 与 {@code count} 等长的候选码列表（不去重，不查库）
     */
    public List<String> issue(int count) {
        if (count <= 0) {
            return List.of();
        }
        log.debug("TreasureCodeIssuer.issue count={}", count);
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(generator.generateOne());
        }
        return out;
    }
}