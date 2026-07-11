-- ======================================================================
-- 咔滋外卖 数据库建表脚本
-- 适用 MySQL 5.7.26，字符集 utf8mb4，引擎 InnoDB
-- 表结构遵循第三范式，所有表均添加必要索引
-- ======================================================================

DROP DATABASE IF EXISTS `kazi_takeaway_project`;
CREATE DATABASE `kazi_takeaway_project` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `kazi_takeaway_project`;

-- ----------------------------------------------------------------------
-- 1. 用户表（含普通用户与管理员，通过 role 区分）
--    role: 0=普通用户 1=管理员
--    gender: 0=保密 1=男 2=女
--    status: 0=正常 1=禁用
-- ----------------------------------------------------------------------
CREATE TABLE `user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone`         VARCHAR(11)  NOT NULL COMMENT '手机号',
  `username`      VARCHAR(32)  NOT NULL COMMENT '用户名',
  `nickname`      VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
  `password`      VARCHAR(128) NOT NULL COMMENT '密码(PBKDF2:迭代次数:盐:哈希)',
  `avatar`        VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `gender`        TINYINT      NOT NULL DEFAULT 0 COMMENT '性别 0保密 1男 2女',
  `role`          TINYINT      NOT NULL DEFAULT 0 COMMENT '角色 0用户 1管理员',
  `member_level`  VARCHAR(8)   DEFAULT NULL COMMENT '会员等级',
  `points`        INT          NOT NULL DEFAULT 0 COMMENT '积分',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0正常 1禁用',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------------------------------------------------
-- 2. 登录令牌表（支撑30天自动登录，token 为安全随机串）
-- ----------------------------------------------------------------------
CREATE TABLE `user_token` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `token`        VARCHAR(64)  NOT NULL COMMENT '登录令牌',
  `expire_time`  DATETIME     NOT NULL COMMENT '过期时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录令牌表';

-- ----------------------------------------------------------------------
-- 3. 商家表
--    badge: 品牌品质优选新店，NULL 表示无
--    price_level: ¥/¥¥/¥¥¥
-- ----------------------------------------------------------------------
CREATE TABLE `merchant` (
  `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT         DEFAULT NULL COMMENT '关联用户ID(role=2)',
  `name`          VARCHAR(64)    NOT NULL COMMENT '商家名称',
  `logo`          VARCHAR(512)   DEFAULT NULL COMMENT 'Logo',
  `cover`         VARCHAR(512)   DEFAULT NULL COMMENT '封面图',
  `category`      VARCHAR(32)    NOT NULL COMMENT '分类',
  `price_level`   VARCHAR(8)     DEFAULT '¥¥' COMMENT '价格档位',
  `rating`        DECIMAL(2,1)   NOT NULL DEFAULT 4.8 COMMENT '评分',
  `sales`         INT            NOT NULL DEFAULT 0 COMMENT '月售量',
  `delivery_time` INT            NOT NULL DEFAULT 30 COMMENT '配送时长(分钟)',
  `distance`      DECIMAL(3,1)   NOT NULL DEFAULT 1.0 COMMENT '距离(km)',
  `min_order`     DECIMAL(8,2)   NOT NULL DEFAULT 20.00 COMMENT '起送价',
  `delivery_fee`  DECIMAL(8,2)   NOT NULL DEFAULT 3.00 COMMENT '配送费',
  `badge`         VARCHAR(16)    DEFAULT NULL COMMENT '徽章',
  `tags`          VARCHAR(255)   DEFAULT NULL COMMENT '特色标签(逗号分隔)',
  `status`           TINYINT        NOT NULL DEFAULT 0 COMMENT '状态 0营业 1休息',
  `is_recommended`   TINYINT        NOT NULL DEFAULT 0 COMMENT '好评商家 0否 1是',
  `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_sales` (`sales`),
  KEY `idx_is_recommended` (`is_recommended`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

-- ----------------------------------------------------------------------
-- 4. 商家优惠表（满减/折扣/免配送费/新客立减）
--    type: discount 折扣 fullcut 满减 freefee 免配送费 newuser 新客立减
-- ----------------------------------------------------------------------
CREATE TABLE `merchant_promo` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id`    BIGINT       NOT NULL COMMENT '商家ID',
  `type`           VARCHAR(16)  NOT NULL COMMENT '优惠类型',
  `description`    VARCHAR(128) DEFAULT NULL COMMENT '优惠描述',
  `name`           VARCHAR(64)  DEFAULT NULL COMMENT '活动名称',
  `discount_ratio` INT          DEFAULT NULL COMMENT '折扣率0-99(80=打8折)',
  `min_spend`      DECIMAL(8,2) DEFAULT NULL COMMENT '最低消费门槛',
  `priority`       INT          NOT NULL DEFAULT 0 COMMENT '优先级(大优先)',
  `start_time`     DATETIME     DEFAULT NULL COMMENT '生效时间',
  `end_time`       DATETIME     DEFAULT NULL COMMENT '失效时间',
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0启用 1停用',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家优惠表';

-- ----------------------------------------------------------------------
-- 5. 满减规则表（一档一行，支撑满减计算）
-- ----------------------------------------------------------------------
CREATE TABLE `promo_rule` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id`  BIGINT       NOT NULL COMMENT '商家ID',
  `threshold`    DECIMAL(8,2) NOT NULL COMMENT '满减门槛',
  `discount`     DECIMAL(8,2) NOT NULL COMMENT '优惠金额',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0启用 1停用',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减规则表';

-- ----------------------------------------------------------------------
-- 6. 菜品表
--    on_shelf: 0上架 1下架
-- ----------------------------------------------------------------------
CREATE TABLE `dish` (
  `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id`  BIGINT         NOT NULL COMMENT '所属商家ID',
  `name`         VARCHAR(64)    NOT NULL COMMENT '菜品名称',
  `price`        DECIMAL(8,2)   NOT NULL COMMENT '现价',
  `old_price`    DECIMAL(8,2)   DEFAULT NULL COMMENT '原价',
  `image`        VARCHAR(512)   DEFAULT NULL COMMENT '图片',
  `description`  VARCHAR(255)   DEFAULT NULL COMMENT '描述',
  `category`     VARCHAR(32)    DEFAULT NULL COMMENT '子分类(招牌推荐等)',
  `sales`        INT            NOT NULL DEFAULT 0 COMMENT '月销量',
  `is_discount`  TINYINT        NOT NULL DEFAULT 0 COMMENT '是否折扣 0否 1是',
  `is_new`       TINYINT        NOT NULL DEFAULT 0 COMMENT '是否新品 0否 1是',
  `is_popular`   TINYINT        NOT NULL DEFAULT 0 COMMENT '是否人气菜品 0否 1是',
  `on_shelf`     TINYINT      NOT NULL DEFAULT 0 COMMENT '上下架 0上架 1下架',
  `sold_out`     TINYINT      NOT NULL DEFAULT 0 COMMENT '售罄 0否 1是',
  `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_sales` (`sales`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

-- ----------------------------------------------------------------------
-- 7. 菜品规格组表（如辣度、酱料、加料）
--    option_type: single 单选 multi 多选
-- ----------------------------------------------------------------------
CREATE TABLE `dish_option` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id`      BIGINT       NOT NULL COMMENT '菜品ID',
  `name`         VARCHAR(32)  NOT NULL COMMENT '规格组名称',
  `option_type`  VARCHAR(8)   NOT NULL DEFAULT 'single' COMMENT 'single单选 multi多选',
  `sort`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品规格组表';

-- ----------------------------------------------------------------------
-- 8. 规格选项值表（如微辣、中辣，可加价）
-- ----------------------------------------------------------------------
CREATE TABLE `dish_option_choice` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `option_id`    BIGINT       NOT NULL COMMENT '规格组ID',
  `name`         VARCHAR(32)  NOT NULL COMMENT '选项名称',
  `price_add`    DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '加价金额',
  `sort`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_option_id` (`option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格选项值表';

-- ----------------------------------------------------------------------
-- 9. 购物车表（按用户+商家维度，记录所选菜品及规格快照）
-- ----------------------------------------------------------------------
CREATE TABLE `cart` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
  `merchant_id`     BIGINT       NOT NULL COMMENT '商家ID',
  `dish_id`         BIGINT       NOT NULL COMMENT '菜品ID',
  `dish_name`       VARCHAR(64)  NOT NULL COMMENT '菜品名称',
  `dish_image`      VARCHAR(512) DEFAULT NULL COMMENT '菜品图片',
  `base_price`      DECIMAL(8,2) NOT NULL COMMENT '菜品基础价',
  `unit_price`      DECIMAL(8,2) NOT NULL COMMENT '实际单价(含加价)',
  `qty`             INT          NOT NULL DEFAULT 1 COMMENT '数量',
  `options_text`    VARCHAR(255) DEFAULT NULL COMMENT '规格文本(加辣、双倍芝士)',
  `options_snapshot` VARCHAR(512) DEFAULT NULL COMMENT '规格快照JSON',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_merchant` (`user_id`, `merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ----------------------------------------------------------------------
-- 10. 收货地址表
--     tag: home 家 company 公司 school 学校
-- ----------------------------------------------------------------------
CREATE TABLE `address` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `name`         VARCHAR(32)  NOT NULL COMMENT '收件人',
  `phone`        VARCHAR(11)  NOT NULL COMMENT '联系电话',
  `region`       VARCHAR(128) NOT NULL COMMENT '所在地区',
  `detail`       VARCHAR(255) NOT NULL COMMENT '详细地址',
  `tag`          VARCHAR(8)   DEFAULT 'home' COMMENT '标签',
  `is_default`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0否 1是',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ----------------------------------------------------------------------
-- 11. 银行卡表（card_type: debit 储蓄卡 credit 信用卡）
--     注意：生产环境不应存储 CVV，此处为个人项目演示用途
-- ----------------------------------------------------------------------
CREATE TABLE `bank_card` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `holder`       VARCHAR(32)  NOT NULL COMMENT '持卡人姓名',
  `card_number`  VARCHAR(32)  NOT NULL COMMENT '卡号',
  `bank_name`    VARCHAR(32)  NOT NULL COMMENT '所属银行',
  `card_type`    VARCHAR(8)   NOT NULL DEFAULT 'debit' COMMENT 'debit储蓄卡 credit信用卡',
  `expire_date`  VARCHAR(8)   DEFAULT NULL COMMENT '有效期 MM/YY',
  `cvv`          VARCHAR(4)   DEFAULT NULL COMMENT 'CVV(演示用途)',
  `is_default`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0否 1是',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行卡表';

-- ----------------------------------------------------------------------
-- 12. 商家收藏表
-- ----------------------------------------------------------------------
CREATE TABLE `favorite` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
  `merchant_id`  BIGINT   NOT NULL COMMENT '商家ID',
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_merchant` (`user_id`, `merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家收藏表';

-- ----------------------------------------------------------------------
-- 13. 订单表
--     status: PENDING_PAY 待支付 PAID 已支付 PENDING_CONFIRM 待确认
--             CONFIRMED 已确认 DELIVERING 配送中 COMPLETED 已完成 CANCELLED 已取消
--     pay_method: wechat alipay bank
-- ----------------------------------------------------------------------
CREATE TABLE `orders` (
  `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`          VARCHAR(32)    NOT NULL COMMENT '订单号',
  `user_id`           BIGINT         NOT NULL COMMENT '用户ID',
  `merchant_id`       BIGINT         NOT NULL COMMENT '商家ID',
  `merchant_name`     VARCHAR(64)    NOT NULL COMMENT '商家名称快照',
  `subtotal`          DECIMAL(10,2)  NOT NULL COMMENT '菜品小计',
  `discount`          DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '满减优惠',
  `delivery_fee`      DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '配送费',
  `total_amount`      DECIMAL(10,2)  NOT NULL COMMENT '实付金额',
  `status`            VARCHAR(20)    NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态',
  `note`              VARCHAR(200)   DEFAULT NULL COMMENT '订单留言',
  `pay_method`        VARCHAR(8)     DEFAULT NULL COMMENT '支付方式',
  `address_snapshot`  VARCHAR(255)   DEFAULT NULL COMMENT '收货地址快照',
  `pay_time`          DATETIME       DEFAULT NULL COMMENT '支付时间',
  `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------------------------------------------------
-- 14. 订单明细表
-- ----------------------------------------------------------------------
CREATE TABLE `order_item` (
  `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`      BIGINT         NOT NULL COMMENT '订单ID',
  `dish_id`       BIGINT         NOT NULL COMMENT '菜品ID',
  `dish_name`     VARCHAR(64)    NOT NULL COMMENT '菜品名称',
  `dish_image`    VARCHAR(512)   DEFAULT NULL COMMENT '菜品图片',
  `unit_price`    DECIMAL(8,2)   NOT NULL COMMENT '单价',
  `qty`           INT            NOT NULL COMMENT '数量',
  `options_text`  VARCHAR(255)   DEFAULT NULL COMMENT '规格文本',
  `subtotal`      DECIMAL(10,2)  NOT NULL COMMENT '小计',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ----------------------------------------------------------------------
-- 15. 用户评价表
-- ----------------------------------------------------------------------
CREATE TABLE `review` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
  `order_id`        BIGINT       NOT NULL COMMENT '订单ID',
  `merchant_id`     BIGINT       NOT NULL COMMENT '商家ID',
  `rating`          TINYINT      NOT NULL DEFAULT 5 COMMENT '整体服务评分1-5',
  `delivery_rating` TINYINT      DEFAULT NULL COMMENT '配送速度评分1-5',
  `content`         VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     DEFAULT NULL COMMENT '更新时间(编辑时写)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户评价表';

-- ----------------------------------------------------------------------
-- 16. 评价菜品明细表（每道菜的独立评分/文字/实拍图）
-- ----------------------------------------------------------------------
CREATE TABLE `review_item` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `review_id`    BIGINT        NOT NULL COMMENT '评价ID',
  `dish_id`      BIGINT        DEFAULT NULL COMMENT '菜品ID',
  `dish_name`    VARCHAR(64)   NOT NULL COMMENT '菜品名称',
  `dish_image`   VARCHAR(512)  DEFAULT NULL COMMENT '菜品图片',
  `rating`       TINYINT       NOT NULL DEFAULT 5 COMMENT '菜品评分1-5',
  `content`      VARCHAR(500)  DEFAULT NULL COMMENT '菜品评价内容',
  `images`       VARCHAR(1000) DEFAULT NULL COMMENT '实拍图片路径(逗号分隔)',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_id` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价菜品明细表';

-- ----------------------------------------------------------------------
-- 17. 商家操作日志表（记录管理员对商家的状态变更操作）
-- ----------------------------------------------------------------------
CREATE TABLE `merchant_op_log` (
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


-- ======================================================================
-- 种子数据
-- ======================================================================

-- 管理员账号 admin / admin123；测试用户 13888888888 / test1234
INSERT INTO `user` (`phone`,`username`,`nickname`,`password`,`avatar`,`gender`,`role`,`member_level`,`points`) VALUES
('10000000000','admin','系统管理员','pbkdf2:10000:fa4687eef68ab184434005201a0c5975:99321b71073ea4705dbb48780457676aef9ef4dcb3636744bae982fd42af04c9',NULL,1,1,NULL,0),
('13888888888','qiukai_2024','秋凯','pbkdf2:10000:ac39e4119c770de25fc4f38897300067:6609c1aed394b2cf1163cbb994610ca414e83790f65e4bb4e9e1c6c9a15de8ed','https://picsum.photos/seed/avatar_qiukai/200/200',0,0,'V3',1380);

-- 商家（与前端 mock 对齐，取核心 6 家）
INSERT INTO `merchant` (`name`,`logo`,`cover`,`category`,`price_level`,`rating`,`sales`,`delivery_time`,`distance`,`min_order`,`delivery_fee`,`badge`,`tags`) VALUES
('汉堡研究所','https://picsum.photos/seed/logo_burger/200/200','https://picsum.photos/seed/cover_burger/800/400','汉堡快餐','¥¥',4.9,9999,25,1.2,20.00,3.00,'品牌','现做汉堡,准时必达'),
('酥脆炸鸡铺','https://picsum.photos/seed/logo_chicken/200/200','https://picsum.photos/seed/cover_chicken/800/400','炸鸡小吃','¥¥',4.8,8800,30,1.5,15.00,3.00,'品质优选','现炸出锅'),
('蜀地麻辣烫','https://picsum.photos/seed/logo_mala/200/200','https://picsum.photos/seed/cover_mala/800/400','麻辣烫','¥¥',4.7,7600,35,2.0,18.00,2.00,NULL,'骨汤熬制'),
('一兰拉面屋','https://picsum.photos/seed/logo_ramen/200/200','https://picsum.photos/seed/cover_ramen/800/400','日式料理','¥¥¥',4.9,6500,40,2.8,25.00,5.00,'品牌','正宗日式'),
('茶颜悦色','https://picsum.photos/seed/logo_tea/200/200','https://picsum.photos/seed/cover_tea/800/400','奶茶果汁','¥',4.8,13000,20,0.8,12.00,2.00,'品牌','鲜茶现萃'),
('绿野轻食','https://picsum.photos/seed/logo_salad/200/200','https://picsum.photos/seed/cover_salad/800/400','沙拉轻食','¥¥',4.6,3200,30,1.8,22.00,4.00,NULL,'健康减脂');

-- 商家优惠
INSERT INTO `merchant_promo` (`merchant_id`,`type`,`description`) VALUES
(1,'fullcut','满30减8，满50减15，满80减25'),
(1,'discount','部分商品限时折扣'),
(2,'fullcut','满25减6，满45减12'),
(3,'fullcut','满20减5，满40减10'),
(3,'freefee','免配送费'),
(4,'fullcut','满40减8'),
(5,'fullcut','满15减3，满30减8'),
(5,'newuser','新客立减5元'),
(6,'fullcut','满30减6');

-- 满减规则（与优惠描述对齐）
INSERT INTO `promo_rule` (`merchant_id`,`threshold`,`discount`) VALUES
(1,30.00,8.00),(1,50.00,15.00),(1,80.00,25.00),
(2,25.00,6.00),(2,45.00,12.00),
(3,20.00,5.00),(3,40.00,10.00),
(4,40.00,8.00),
(5,15.00,3.00),(5,30.00,8.00),
(6,30.00,6.00);

-- 菜品（汉堡研究所 id=1 的菜品，含折扣与规格）
INSERT INTO `dish` (`merchant_id`,`name`,`price`,`old_price`,`image`,`description`,`category`,`sales`,`is_discount`,`is_new`,`on_shelf`) VALUES
(1,'双层芝士堡',28.00,35.00,'https://picsum.photos/seed/dish_burger1/400/300','双层安格斯牛肉饼，搭配车达芝士与秘制酱料','招牌推荐',5800,1,0,0),
(1,'培根牛肉堡',32.00,NULL,'https://picsum.photos/seed/dish_burger2/400/300','厚切培根配澳洲牛肉饼','汉堡系列',3200,0,0,0),
(1,'香辣鸡腿堡',22.00,26.00,'https://picsum.photos/seed/dish_burger3/400/300','现炸鸡腿肉，秘制辣酱','汉堡系列',4500,1,0,0),
(1,'黄金薯条',12.00,NULL,'https://picsum.photos/seed/dish_fries/400/300','现炸薯条，外酥里嫩','小吃系列',6800,0,0,0),
(1,'香辣鸡翅',18.00,NULL,'https://picsum.photos/seed/dish_wings/400/300','三只装，秘制腌料','小吃系列',3900,0,0,0),
(1,'冰可乐',8.00,NULL,'https://picsum.photos/seed/dish_coke/400/300','冰镇可口可乐','饮品系列',7200,0,0,0),
(1,'芒果圣代',15.00,18.00,'https://picsum.photos/seed/dish_sundae/400/300','新鲜芒果果肉配香草冰淇淋','甜品系列',2100,1,1,0),
(1,'至尊牛肉堡',38.00,NULL,'https://picsum.photos/seed/dish_burger4/400/300','三层牛肉饼，双层芝士','招牌推荐',1500,0,1,0);

-- 菜品规格组（双层芝士堡 dish_id=1）
INSERT INTO `dish_option` (`dish_id`,`name`,`option_type`,`sort`) VALUES
(1,'辣度','single',1),(1,'加料','multi',2),(1,'份量','single',3);
-- 规格选项值
INSERT INTO `dish_option_choice` (`option_id`,`name`,`price_add`,`sort`) VALUES
(1,'不辣',0.00,1),(1,'微辣',0.00,2),(1,'中辣',0.00,3),(1,'特辣',1.00,4),
(2,'双倍芝士',5.00,1),(2,'加培根',6.00,2),(2,'加蛋',3.00,3),
(3,'标准份',0.00,1),(3,'加大份',8.00,2);

-- 菜品规格组（冰可乐 dish_id=6）
INSERT INTO `dish_option` (`dish_id`,`name`,`option_type`,`sort`) VALUES
(6,'冰度','single',1),(6,'甜度','single',2);
INSERT INTO `dish_option_choice` (`option_id`,`name`,`price_add`,`sort`) VALUES
(4,'正常冰',0.00,1),(4,'少冰',0.00,2),(4,'去冰',0.00,3),(4,'多冰',0.00,4),
(5,'全糖',0.00,1),(5,'七分糖',0.00,2),(5,'半糖',0.00,3),(5,'无糖',0.00,4);

-- 测试用户收货地址
INSERT INTO `address` (`user_id`,`name`,`phone`,`region`,`detail`,`tag`,`is_default`) VALUES
(2,'秋凯','13888888888','北京市朝阳区','建国路 88 号咔滋大厦 12 层 1208 室','company',1),
(2,'秋凯','13888888888','北京市海淀区','中关村大街 1 号 3 单元 502','home',0);

-- 测试用户银行卡
INSERT INTO `bank_card` (`user_id`,`holder`,`card_number`,`bank_name`,`card_type`,`expire_date`,`cvv`,`is_default`) VALUES
(2,'秋凯','6222021234567890123','工商银行','debit','08/28','123',1),
(2,'秋凯','6225887654321098','招商银行','credit','11/27','456',0);

-- 测试用户收藏
INSERT INTO `favorite` (`user_id`,`merchant_id`) VALUES
(2,1),(2,3),(2,5);

-- 测试订单（已完成，用于评价）
INSERT INTO `orders` (`order_no`,`user_id`,`merchant_id`,`merchant_name`,`subtotal`,`discount`,`delivery_fee`,`total_amount`,`status`,`pay_method`,`address_snapshot`,`pay_time`) VALUES
('KZ20260620001',2,1,'汉堡研究所',68.00,8.00,3.00,63.00,'COMPLETED','wechat','北京市朝阳区建国路 88 号咔滋大厦 12 层 1208 室','2026-06-20 12:30:00');
INSERT INTO `order_item` (`order_id`,`dish_id`,`dish_name`,`dish_image`,`unit_price`,`qty`,`options_text`,`subtotal`) VALUES
(1,1,'双层芝士堡','https://picsum.photos/seed/dish_burger1/400/300',28.00,2,'中辣、双倍芝士',56.00),
(1,6,'冰可乐','https://picsum.photos/seed/dish_coke/400/300',8.00,1,'少冰、半糖',8.00),
(1,4,'黄金薯条','https://picsum.photos/seed/dish_fries/400/300',12.00,1,NULL,12.00);

-- 测试评价
INSERT INTO `review` (`user_id`,`order_id`,`merchant_id`,`rating`,`content`) VALUES
(2,1,1,5,'汉堡很赞，芝士拉丝超满足，配送也很快！');
