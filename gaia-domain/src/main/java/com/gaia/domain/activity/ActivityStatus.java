package com.gaia.domain.activity;

/**
 * 活动状态枚举。
 */
public enum ActivityStatus {

    NOT_STARTED(0, "未开始"),
    ONGOING(1, "进行中"),
    WAITING_DRAW(2, "待开奖"),
    DRAWN(3, "已开奖");

    private final int code;
    private final String desc;

    ActivityStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int code() {
        return code;
    }

    public String desc() {
        return desc;
    }
}