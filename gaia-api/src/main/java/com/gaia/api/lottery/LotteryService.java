package com.gaia.api.lottery;

/**
 * 抽奖模块 RPC 接口（二期预留）。
 *
 * <p>当前仅暴露健康检查契约，用于校验 Gateway → Server 链路通畅，
 * 业务实现将在下一期填充。</p>
 */
public interface LotteryService {

    /**
     * 健康检查。
     */
    String ping();
}
