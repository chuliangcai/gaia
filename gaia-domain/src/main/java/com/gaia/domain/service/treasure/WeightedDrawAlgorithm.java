package com.gaia.domain.service.treasure;

import com.gaia.domain.draw.DrawCandidate;
import com.gaia.domain.draw.DrawWinner;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 加权随机不放回抽奖算法（纯领域服务）。
 *
 * <p>每个候选有 weight（夺宝码数量），按权重累计不放回抽取
 * {@code winnerCount} 名中奖者。</p>
 *
 * <p>算法：</p>
 * <ol>
 *   <li>构建候选权重累计和数组</li>
 *   <li>每轮随机数 {@code r in [0, totalWeight)}，二分查找累计和找命中位置</li>
 *   <li>命中候选的权重 -1（即用掉一个码），若减到 0 则从候选池剔除</li>
 *   <li>重复直到抽够 winnerCount 名或候选池空</li>
 * </ol>
 */
public class WeightedDrawAlgorithm {

    private final SecureRandom random = new SecureRandom();

    /**
     * 执行加权随机抽奖。
     *
     * @param candidates  候选列表（userId, weight）
     * @param winnerCount 期望中奖人数
     * @return 中奖者列表（已按中奖顺序排序）
     */
    public List<DrawWinner> draw(List<DrawCandidate> candidates, int winnerCount) {
        if (candidates == null || candidates.isEmpty() || winnerCount <= 0) {
            return List.of();
        }
        // 防御：过滤掉 weight<=0 的候选
        List<DrawCandidate> pool = new ArrayList<>();
        for (DrawCandidate c : candidates) {
            if (c != null && c.getUserId() != null && c.getWeight() != null && c.getWeight() > 0) {
                pool.add(c);
            }
        }
        if (pool.isEmpty()) {
            return List.of();
        }

        List<DrawWinner> winners = new ArrayList<>();
        Set<Long> seenUser = new HashSet<>();
        int rank = 1;
        int maxIterations = (int) Math.min(Integer.MAX_VALUE,
                totalWeight(pool) * 2L + (long) winnerCount * 2L);
        int iter = 0;

        while (winners.size() < winnerCount && iter < maxIterations) {
            iter++;
            long totalWeight = totalWeight(pool);
            if (totalWeight <= 0) {
                break;
            }
            long hit = nextLong(totalWeight);
            int idx = binarySearchCumWeight(pool, hit);
            DrawCandidate picked = pool.get(idx);
            // 同用户只能中一次（保证名单公平且简单）
            if (seenUser.contains(picked.getUserId())) {
                picked.setWeight(picked.getWeight() - 1);
                if (picked.getWeight() <= 0) {
                    pool.remove(idx);
                }
                continue;
            }
            winners.add(DrawWinner.builder()
                    .userId(picked.getUserId())
                    .code(null) // code 由发奖层从该用户的夺宝码集合挑选一个填入
                    .rank(rank++)
                    .weight(picked.getWeight())
                    .build());
            seenUser.add(picked.getUserId());
            picked.setWeight(picked.getWeight() - 1);
            if (picked.getWeight() <= 0) {
                pool.remove(idx);
            }
        }
        return winners;
    }

    private long totalWeight(List<DrawCandidate> pool) {
        long sum = 0L;
        for (DrawCandidate c : pool) {
            sum += c.getWeight();
        }
        return sum;
    }

    /**
     * 二分查找累计权重：找到第一个 cumulativeWeight > hit 的下标。
     */
    private int binarySearchCumWeight(List<DrawCandidate> pool, long hit) {
        long cumulative = 0L;
        for (int i = 0; i < pool.size(); i++) {
            cumulative += pool.get(i).getWeight();
            if (cumulative > hit) {
                return i;
            }
        }
        return pool.size() - 1;
    }

    private long nextLong(long bound) {
        // SecureRandom.nextLong(bound) 在 bound 接近 Long.MAX_VALUE 时会有偏置，
        // 但我们 bound = totalWeight(<= 码总数)，远小于 2^62，安全。
        return random.nextLong(bound);
    }
}