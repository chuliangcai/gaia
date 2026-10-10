package com.gaia.domain.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动领域实体。
 *
 * <p>状态机：</p>
 * <pre>
 *   0 NOT_STARTED ──(start_time 到达)──▶ 1 ONGOING
 *   1 ONGOING ──(end_time 到达)──▶ 2 WAITING_DRAW
 *   2 WAITING_DRAW ──(开奖完成)──▶ 3 DRAWN
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 活动业务 code（wire 对外） */
    private String activityCode;

    /** 活动名称 */
    private String name;

    /** 奖品描述 */
    private String prizeDesc;

    /** 奖品图片 */
    private String prizeImgUrl;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 状态：0=未开始,1=进行中,2=待开奖,3=已开奖 */
    private Integer status;

    /** 中奖人数 */
    private Integer winnerCount;

    /** 实际开奖时间 */
    private LocalDateTime drawAt;

    /** 乐观锁版本 */
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isNotStarted() {
        return status != null && status == ActivityStatus.NOT_STARTED.code();
    }

    public boolean isOngoing() {
        return status != null && status == ActivityStatus.ONGOING.code();
    }

    public boolean isWaitingDraw() {
        return status != null && status == ActivityStatus.WAITING_DRAW.code();
    }

    public boolean isDrawn() {
        return status != null && status == ActivityStatus.DRAWN.code();
    }

    /**
     * 状态推进：0→1
     */
    public void markOngoing() {
        this.status = ActivityStatus.ONGOING.code();
    }

    /**
     * 状态推进：1→2
     */
    public void markWaitingDraw() {
        this.status = ActivityStatus.WAITING_DRAW.code();
    }

    /**
     * 状态推进：2→3
     */
    public void markDrawn() {
        this.status = ActivityStatus.DRAWN.code();
        this.drawAt = LocalDateTime.now();
    }
}