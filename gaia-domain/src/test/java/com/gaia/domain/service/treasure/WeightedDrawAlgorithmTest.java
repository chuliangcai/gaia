package com.gaia.domain.service.treasure;

import com.gaia.domain.draw.DrawCandidate;
import com.gaia.domain.draw.DrawWinner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 加权随机不放回抽奖测试。
 */
class WeightedDrawAlgorithmTest {

    private final WeightedDrawAlgorithm algorithm = new WeightedDrawAlgorithm();

    @Test
    void testEmptyCandidates_returnsEmpty() {
        List<DrawWinner> winners = algorithm.draw(List.of(), 3);
        assertTrue(winners.isEmpty());
    }

    @Test
    void testNullCandidates_returnsEmpty() {
        List<DrawWinner> winners = algorithm.draw(null, 3);
        assertTrue(winners.isEmpty());
    }

    @Test
    void testZeroWinnerCount_returnsEmpty() {
        List<DrawCandidate> cands = List.of(
                DrawCandidate.builder().userId(1L).weight(5).build());
        List<DrawWinner> winners = algorithm.draw(cands, 0);
        assertTrue(winners.isEmpty());
    }

    @Test
    void testSingleCandidate_alwaysWins() {
        List<DrawCandidate> cands = List.of(
                DrawCandidate.builder().userId(42L).weight(1).build());
        List<DrawWinner> winners = algorithm.draw(cands, 1);
        assertEquals(1, winners.size());
        assertEquals(42L, winners.get(0).getUserId());
        assertEquals(1, winners.get(0).getRank());
    }

    @Test
    void testRankAssignment_isMonotonic() {
        List<DrawCandidate> cands = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            cands.add(DrawCandidate.builder().userId(i).weight(1).build());
        }
        List<DrawWinner> winners = algorithm.draw(cands, 3);
        assertEquals(3, winners.size());
        for (int i = 0; i < winners.size(); i++) {
            assertEquals(i + 1, winners.get(i).getRank());
        }
    }

    @Test
    void testWinnerCountGreaterThanCandidates_truncatedToCandidates() {
        // 每个候选只允许中一次，且只有 2 个候选
        List<DrawCandidate> cands = List.of(
                DrawCandidate.builder().userId(1L).weight(10).build(),
                DrawCandidate.builder().userId(2L).weight(10).build());
        List<DrawWinner> winners = algorithm.draw(cands, 5);
        // 最多 2 个不同的 userId
        assertTrue(winners.size() <= 2);
    }

    @Test
    void testSingleUser_HeavyWeight_mustBePicked() {
        List<DrawCandidate> cands = List.of(
                DrawCandidate.builder().userId(1L).weight(1000).build(),
                DrawCandidate.builder().userId(2L).weight(1).build());
        // 用户 1 的权重是用户 2 的 1000 倍，应该必中
        for (int i = 0; i < 100; i++) {
            List<DrawWinner> winners = algorithm.draw(cands, 1);
            assertEquals(1, winners.size());
            assertEquals(1L, winners.get(0).getUserId());
        }
    }

    @Test
    void testMultipleDraws_DoesNotProduceNegativeWeights() {
        List<DrawCandidate> cands = List.of(
                DrawCandidate.builder().userId(1L).weight(5).build(),
                DrawCandidate.builder().userId(2L).weight(5).build());
        for (int trial = 0; trial < 100; trial++) {
            List<DrawWinner> winners = algorithm.draw(cands, 2);
            for (DrawWinner w : winners) {
                assertNotNull(w.getUserId());
                assertNotNull(w.getRank());
                assertFalse(w.getWeight() != null && w.getWeight() < 0, "weight must not be negative");
            }
        }
    }
}