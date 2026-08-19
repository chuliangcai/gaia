-- ==========================================
-- Gaia 服务进程表结构（MySQL 8.0）
-- ==========================================

CREATE DATABASE IF NOT EXISTS `gaia` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `gaia`;

DROP TABLE IF EXISTS `user_sign_record`;
CREATE TABLE `user_sign_record` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT          NOT NULL COMMENT '用户ID',
    `sign_date`     VARCHAR(16)     NOT NULL COMMENT '签到日期 yyyy-MM-dd',
    `year_month`    VARCHAR(8)      NOT NULL COMMENT '年月 yyyyMM',
    `reward_points` BIGINT          NOT NULL DEFAULT 0 COMMENT '当次奖励积分',
    `streak_days`   INT             NOT NULL DEFAULT 0 COMMENT '连续天数',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `sign_date`),
    KEY `idx_user_ym` (`user_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户签到记录';
