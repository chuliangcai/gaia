package com.gaia.domain.activity;

/**
 * 活动明细动作类型。
 */
public final class ActionType {

    private ActionType() {
    }

    /** 用户执行了任务 */
    public static final String TASK_EXECUTED = "TASK_EXECUTED";

    /** 夺宝码发放（与 TASK_EXECUTED 同事务，作为结果明细） */
    public static final String CODE_ISSUED = "CODE_ISSUED";

    /** 活动开奖执行 */
    public static final String DRAW_EXECUTED = "DRAW_EXECUTED";

    /** 用户中奖（每条记录一个中奖码） */
    public static final String DRAW_WON = "DRAW_WON";
}