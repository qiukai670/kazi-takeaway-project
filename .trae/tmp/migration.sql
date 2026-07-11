ALTER TABLE merchant_promo
  ADD COLUMN name VARCHAR(64) DEFAULT NULL COMMENT '活动名称' AFTER description,
  ADD COLUMN discount_ratio INT DEFAULT NULL COMMENT '折扣率0-99(80=打8折)' AFTER name,
  ADD COLUMN min_spend DECIMAL(8,2) DEFAULT NULL COMMENT '最低消费门槛' AFTER discount_ratio,
  ADD COLUMN priority INT NOT NULL DEFAULT 0 COMMENT '优先级(大优先)' AFTER min_spend,
  ADD COLUMN start_time DATETIME DEFAULT NULL COMMENT '生效时间' AFTER priority,
  ADD COLUMN end_time DATETIME DEFAULT NULL COMMENT '失效时间' AFTER start_time;
