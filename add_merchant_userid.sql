-- 商家表添加 user_id 列，关联用户账号
ALTER TABLE `merchant` ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '关联用户ID(role=2)' AFTER `id`;
CREATE INDEX `idx_user_id` ON `merchant`(`user_id`);
