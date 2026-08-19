package com.gaia.common.constants;

/**
 * Redis Key 统一常量。
 *
 * <p>所有 Key 命名采用 {@code gaia:模块:业务:id} 形式，集中维护避免散落。</p>
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 默认 namespace */
    public static final String NAMESPACE = "gaia";

    // ====================== 签到模块 ======================

    /** 用户月度签到 BitMap Key，{userId} 为用户 ID */
    public static final String SIGN_BITMAP_KEY = NAMESPACE + ":sign:bitmap:{userId}:{ym}";

    /** 用户连续签到天数缓存 Key（避免每次 BITCOUNT 后再 BITPOS） */
    public static final String SIGN_STREAK_KEY = NAMESPACE + ":sign:streak:{userId}";

    /** 签到分布式锁 Key，防重入 */
    public static final String SIGN_LOCK_KEY = NAMESPACE + ":sign:lock:{userId}:{date}";

    /** 补签分布式锁 Key */
    public static final String SIGN_REMAKE_LOCK_KEY = NAMESPACE + ":sign:remake:lock:{userId}:{date}";

    // ====================== 抽奖模块（预留） ======================

    public static final String LOTTERY_DRAW_LOCK_KEY = NAMESPACE + ":lottery:lock:{userId}:{activityId}";

    public static final String LOTTERY_STOCK_KEY = NAMESPACE + ":lottery:stock:{activityId}";
}