-- 添加好评商家字段
ALTER TABLE `merchant` ADD COLUMN `is_recommended` TINYINT NOT NULL DEFAULT 0 COMMENT '好评商家 0否 1是' AFTER `status`;
CREATE INDEX `idx_is_recommended` ON `merchant`(`is_recommended`);

-- 操作日志表
CREATE TABLE IF NOT EXISTS `merchant_op_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id`    BIGINT       NOT NULL COMMENT '商家ID',
  `merchant_name`  VARCHAR(64)  DEFAULT NULL COMMENT '商家名称(冗余)',
  `operator_id`    BIGINT       NOT NULL COMMENT '操作人ID',
  `operator_name`  VARCHAR(64)  DEFAULT NULL COMMENT '操作人名称',
  `action`         VARCHAR(32)  NOT NULL COMMENT '操作类型 toggle_recommended/batch_toggle_recommended',
  `old_value`      VARCHAR(16)  DEFAULT NULL COMMENT '修改前值',
  `new_value`      VARCHAR(16)  DEFAULT NULL COMMENT '修改后值',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家操作日志表';

-- 设置部分现有商家为好评商家（用于初始展示）
UPDATE `merchant` SET `is_recommended` = 1 WHERE `badge` = '品牌' AND `status` = 0;
