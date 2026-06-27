-- ======================================================================
-- 评价系统 v2 迁移脚本（手动执行一次）
-- 目标库：kazi_takeaway_project
-- MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS，故非幂等，仅执行一次
-- ======================================================================
USE `kazi_takeaway_project`;

-- 1. review 表新增配送评分、更新时间
ALTER TABLE `review`
  ADD COLUMN `delivery_rating` TINYINT DEFAULT NULL COMMENT '配送速度评分1-5' AFTER `rating`,
  ADD COLUMN `update_time`     DATETIME DEFAULT NULL COMMENT '更新时间(编辑时写)' AFTER `create_time`;

-- 2. 评价菜品明细表（无外键，与 order_item 约定一致）
CREATE TABLE `review_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_id`    BIGINT       NOT NULL COMMENT '评价ID',
  `dish_id`      BIGINT       DEFAULT NULL COMMENT '菜品ID',
  `dish_name`    VARCHAR(64)  NOT NULL COMMENT '菜品名称',
  `dish_image`   VARCHAR(512) DEFAULT NULL COMMENT '菜品图片',
  `rating`       TINYINT      NOT NULL DEFAULT 5 COMMENT '菜品评分1-5',
  `content`      VARCHAR(500) DEFAULT NULL COMMENT '菜品评价内容',
  `images`       VARCHAR(1000) DEFAULT NULL COMMENT '实拍图片路径(逗号分隔)',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_id` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价菜品明细表';
