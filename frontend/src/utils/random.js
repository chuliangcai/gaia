import { PRIZES } from '../config/prizes.js';

/**
 * 按权重抽取一个奖品。
 * 前端演示用——真实场景应由后端下发中奖结果（gaia-api/LotteryService）。
 */
export function pickPrizeByWeight() {
  const total = PRIZES.reduce((sum, p) => sum + p.weight, 0);
  let target = Math.random() * total;
  for (let i = 0; i < PRIZES.length; i++) {
    target -= PRIZES[i].weight;
    if (target <= 0) return PRIZES[i];
  }
  return PRIZES[PRIZES.length - 1];
}
