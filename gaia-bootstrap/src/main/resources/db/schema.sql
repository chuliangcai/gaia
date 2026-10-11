-- ==========================================
-- Gaia 服务全部库表结构（MySQL 8.0）
--   - 单库 `gaia`：会员夺宝相关表
--
-- 全部使用 CREATE ... IF NOT EXISTS，无 DROP，启动幂等。
-- 用法: mysql -h127.0.0.1 -uroot -p12345 < schema.sql
-- ==========================================

-- ----------------------------------------------------------
-- 库: gaia (会员夺宝)
-- ----------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `gaia` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `gaia`;

-- 活动主表
CREATE TABLE IF NOT EXISTS `gaia_activity` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_code`   VARCHAR(64)     NOT NULL COMMENT '活动业务 code (wire 上对外)',
    `biz_code`        VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '玩法 code（@BizProduct.code）',
    `name`            VARCHAR(128)    NOT NULL COMMENT '活动名称',
    `prize_desc`      VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '奖品描述',
    `prize_img_url`   VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '奖品图片 URL',
    `start_time`      DATETIME        NOT NULL COMMENT '活动开始时间',
    `end_time`        DATETIME        NOT NULL COMMENT '活动结束时间',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '0=未开始,1=进行中,2=待开奖,3=已开奖',
    `winner_count`    INT             NOT NULL DEFAULT 1 COMMENT '中奖人数',
    `draw_at`         DATETIME        DEFAULT NULL COMMENT '实际开奖时间',
    `version`         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁',
    `configurations`  TEXT            DEFAULT NULL COMMENT '能力配置 JSON；key=configClass FQN，value=该类配置对象',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_code` (`activity_code`),
    KEY `idx_biz_code` (`biz_code`),
    KEY `idx_status_end` (`status`, `end_time`),
    KEY `idx_status_start` (`status`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动主表';

-- 老库兼容：gaia_activity 若缺 configurations 列则补上（幂等；新装库 CREATE TABLE 已包含该列，本分支不会执行）
SET @col := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gaia_activity' AND COLUMN_NAME = 'configurations');
SET @sql := IF(@col = 0,
               CONCAT('ALTER TABLE gaia_activity ADD COLUMN configurations TEXT DEFAULT NULL COMMENT ',
                      CHAR(39), '能力配置 JSON；key=configClass FQN，value=该类配置对象', CHAR(39),
                      ' AFTER version'),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 活动明细表 (事件流 + 幂等键)
CREATE TABLE IF NOT EXISTS `gaia_activity_record` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT          NOT NULL COMMENT '用户 ID',
    `activity_id`     BIGINT          NOT NULL COMMENT '活动 ID',
    `action_type`     VARCHAR(32)     NOT NULL COMMENT '动作类型 (TASK_EXECUTED/DRAW_EXECUTED/DRAW_WON/...)',
    `action_code`     VARCHAR(128)    NOT NULL COMMENT '幂等键',
    `biz_id`          BIGINT          DEFAULT NULL COMMENT '业务 ID (taskId/activityId 等)',
    `payload`         VARCHAR(2048)   DEFAULT NULL COMMENT '入参 JSON',
    `result`          VARCHAR(2048)   DEFAULT NULL COMMENT '结果 JSON',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_action_code` (`action_code`),
    KEY `idx_user_act_type` (`user_id`, `activity_id`, `action_type`),
    KEY `idx_act_type` (`activity_id`, `action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动明细 (事件流 + 幂等)';

-- 会员表
CREATE TABLE IF NOT EXISTS `gaia_member` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT          NOT NULL COMMENT '用户 ID (与登录用户同义)',
    `level`           VARCHAR(32)     NOT NULL DEFAULT 'NORMAL' COMMENT '会员等级',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=失效,1=有效',
    `join_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expire_at`       DATETIME        DEFAULT NULL COMMENT '会员过期时间',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- 会员权益表 (含夺宝码账本)
CREATE TABLE IF NOT EXISTS `gaia_member_benefit` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT          NOT NULL COMMENT '用户 ID',
    `benefit_type`    VARCHAR(32)     NOT NULL COMMENT '类型: LOTTERY_CODE/PRIZE',
    `benefit_value`   VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '值 (夺宝码或奖品描述)',
    `activity_id`     BIGINT          DEFAULT NULL COMMENT '所属活动',
    `source_id`       BIGINT          DEFAULT NULL COMMENT '来源明细 gaia_activity_record.id',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=失效,1=有效',
    `granted_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expire_at`       DATETIME        DEFAULT NULL,
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 生成列保证 LOTTERY_CODE 活动内唯一, PRIZE 不受影响
    `uk_lottery_code` VARCHAR(96) GENERATED ALWAYS AS
        (CASE WHEN benefit_type = 'LOTTERY_CODE' THEN CONCAT(activity_id, ':', benefit_value) END) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lottery_code` (`uk_lottery_code`),
    KEY `idx_user_type` (`user_id`, `benefit_type`),
    KEY `idx_user_act` (`user_id`, `activity_id`),
    KEY `idx_act_type` (`activity_id`, `benefit_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员权益表 (含夺宝码/奖品账本)';
