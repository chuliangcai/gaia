package com.gaia.api.sign;

import java.io.Serializable;
import java.util.List;

/**
 * 签到 RPC 接口。
 *
 * <p>仅定义契约，不包含任何实现。所有 HTTP 入口必须通过本接口
 * 调用服务，禁止直接访问 Provider 实现类。</p>
 */
public interface SignService {

    String GROUP = "sign";
    String VERSION = "1.0.0";

    /**
     * 用户签到。
     *
     * @param userId 用户 ID（必填）
     * @return 签到结果，包含当日奖励与累计天数
     */
    SignResultDTO sign(Long userId);

    /**
     * 查询用户签到日历。
     *
     * @param userId 用户 ID
     * @param yearMonth yyyyMM 格式年月，例如 202601
     * @return 当月签到详情
     */
    SignCalendarDTO calendar(Long userId, String yearMonth);

    /**
     * 连续签到天数。
     */
    Integer streak(Long userId);

    // ================== 业务对象 ==================

    /**
     * 签到结果。
     */
    class SignResultDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 是否首次签到成功 */
        private boolean success;
        /** 累计连续签到天数 */
        private int streakDays;
        /** 当次奖励积分 */
        private long rewardPoints;
        /** 签到日期 yyyy-MM-dd */
        private String signDate;

        public SignResultDTO() {
        }

        public SignResultDTO(boolean success, int streakDays, long rewardPoints, String signDate) {
            this.success = success;
            this.streakDays = streakDays;
            this.rewardPoints = rewardPoints;
            this.signDate = signDate;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getStreakDays() {
            return streakDays;
        }

        public void setStreakDays(int streakDays) {
            this.streakDays = streakDays;
        }

        public long getRewardPoints() {
            return rewardPoints;
        }

        public void setRewardPoints(long rewardPoints) {
            this.rewardPoints = rewardPoints;
        }

        public String getSignDate() {
            return signDate;
        }

        public void setSignDate(String signDate) {
            this.signDate = signDate;
        }
    }

    /**
     * 签到日历。
     */
    class SignCalendarDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 年月 yyyyMM */
        private String yearMonth;
        /** 已签到的日期集合（yyyy-MM-dd） */
        private List<String> signedDates;
        /** 当月总天数 */
        private int daysInMonth;

        public SignCalendarDTO() {
        }

        public SignCalendarDTO(String yearMonth, List<String> signedDates, int daysInMonth) {
            this.yearMonth = yearMonth;
            this.signedDates = signedDates;
            this.daysInMonth = daysInMonth;
        }

        public String getYearMonth() {
            return yearMonth;
        }

        public void setYearMonth(String yearMonth) {
            this.yearMonth = yearMonth;
        }

        public List<String> getSignedDates() {
            return signedDates;
        }

        public void setSignedDates(List<String> signedDates) {
            this.signedDates = signedDates;
        }

        public int getDaysInMonth() {
            return daysInMonth;
        }

        public void setDaysInMonth(int daysInMonth) {
            this.daysInMonth = daysInMonth;
        }
    }
}
