# 外卖订单评价系统 实施计划

## Context（背景）

当前应用的评价能力极简：`review` 表只有单一 `rating` + `content`，每订单一条（`UNIQUE uk_order_id`），不支持菜品分级、图片、配送评分、编辑/删除。`profile.html` 的"我的评价"面板已存在但展示单薄，且因 `selectByUserId` 未 JOIN 订单表导致 `merchantName` 恒为空。订单卡片上已完成的订单没有"评价"入口。

本计划实现一个完整的订单评价系统：独立评价页（菜品逐条评分 + 图片上传 + 商家服务/配送评分 + 草稿自动保存）、订单卡片"评价"按钮、个人中心"我的评价"富展示与 24 小时内编辑/删除。**数据统计（原需求第 7 项）经确认暂不实现**，专注核心评价功能（1-6 项）。

## 范围

- **纳入**：独立 `review.html` 评价页、订单卡片评价入口、"我的评价"富展示与编辑/删除、菜品图片上传（每道菜最多 3 张）、草稿 30 秒自动保存、表单校验、加载/空/错误状态、响应式。
- **排除**：评价行为统计、完成率、平均时长、关键词提取（第 7 项）。

## 后端改动

### 1. 数据库迁移（手动执行，MySQL 5.7 不支持 `ADD COLUMN IF NOT EXISTS`，非幂等）

新建 `src/main/resources/migrate_review_v2.sql`（实施时用 mysql 客户端对 `kazi_takeaway_project` 库执行一次）：
- `ALTER TABLE review ADD COLUMN delivery_rating TINYINT DEFAULT NULL AFTER rating, ADD COLUMN update_time DATETIME DEFAULT NULL AFTER create_time;`
- `CREATE TABLE review_item`（`id, review_id, dish_id, dish_name, dish_image, rating TINYINT NOT NULL DEFAULT 5, content VARCHAR(500), images VARCHAR(1000) 逗号分隔, create_time`，`KEY idx_review_id`）。无 FK，与 `order_item` 约定一致。
- 同步更新 `schema.sql` 第 279-290 行的 `review` 建表语句（加两列）并追加 `review_item` 建表，供全新安装一致。

`update_time` 不设 `ON UPDATE`，仅编辑时由 `update` 语句写 `NOW()`，24h 窗口以 `create_time` 计算。

### 2. 实体
- 扩展 [Review.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/entity/Review.java)：加 `deliveryRating(Integer)`、`updateTime(LocalDateTime)`、瞬态 `merchantName`、`orderNo`、`items(List<ReviewItem>)`。
- 新建 [ReviewItem.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/entity/ReviewItem.java)：`id, reviewId, dishId, dishName, dishImage, rating, content, images(String csv), createTime, imageList(List<String> 瞬态)`。

### 3. DTO
- 扩展 [ReviewDTO.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/dto/ReviewDTO.java)：保留 `orderId(@NotNull)`、`rating(@NotNull 1-5，整体服务评分，必填)`、`content(@Size 500)`；新增 `deliveryRating(@Min1 @Max5，可选)`、`@Valid List<ReviewItemDTO> items(可选)`。
- 新建 [ReviewItemDTO.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/dto/ReviewItemDTO.java)：`dishId, dishName, dishImage, rating(@Min1 @Max5 可选，null→存5), content(@Size 500), images(@Size(max=3) List<String>)`。

### 4. Mapper
- 扩展 [ReviewMapper.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/mapper/ReviewMapper.java)：加 `selectById`、`update`、`delete`。
- 重写 [ReviewMapper.xml](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/resources/mapper/ReviewMapper.xml)：
  - `reviewResultMap` 增配 `delivery_rating/update_time/merchant_name/order_no`（`items` 不在此映射，由 service 批量装配）。
  - `insert` 显式列 `(user_id, order_id, merchant_id, rating, delivery_rating, content)`——瞬态字段永不写入。
  - `selectByUserId`/`selectById` 改为 `INNER JOIN orders o ON o.id=r.order_id` 取 `o.merchant_name, o.order_no`（订单快照，比 JOIN merchant 更稳）。
  - 新 `update`（`SET rating, delivery_rating, content, update_time=NOW() WHERE id=#`，不动 `order_id`，`uk_order_id` 安全）。
  - 新 `delete`。`selectByOrderId`（去重检查用）保持独立不受影响。
- 新建 [ReviewItemMapper.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/mapper/ReviewItemMapper.java) + [ReviewItemMapper.xml](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/resources/mapper/ReviewItemMapper.xml)：`insertBatch(foreach)`、`selectByReviewId`、`selectByReviewIds(IN foreach)`、`deleteByReviewId`。**service 必须守卫空集合**（空 `foreach` 会语法错）。

### 5. Service（[UserService.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/service/UserService.java)，注入 `ReviewItemMapper`）
- `addReview(dto)`（保留 `@Transactional`）：原有校验（登录、订单归属、`COMPLETED`、未评价）+ insert review（带 deliveryRating，`useGeneratedKeys` 回填 id）+ 非空时映射 items（`images List→csv`，rating null→5）+ `insertBatch`。**改为返回 `Review`**（含生成 id），便于前端跳转。
- `listMyReviews()`：`selectByUserId` → 收集 reviewIds → `selectByReviewIds` 批量 → 按 reviewId 分组装配 `items`（每条 split csv→imageList）。空列表早返回（避 `IN ()`）。
- `getReview(id)`（新）：`selectById` + 校验归属（不属本人抛 404 不泄漏）+ `selectByReviewId` 装配 items。
- `updateReview(id, dto)`（新，`@Transactional`）：`selectById`+归属+`assertWithin24h` → `update`（写 update_time）→ `deleteByReviewId` 删旧 items → 非空时 `insertBatch` 新 items（删后插，同事务回滚保护）。
- `deleteReview(id)`（新，`@Transactional`）：`selectById`+归属+`assertWithin24h` → 先 `deleteByReviewId`（子）再 `delete`（父）。删除后 `uk_order_id` 释放，允许重新评价（产品决定：可重评）。
- `assertWithin24h(review)`：`review.getCreateTime().plusHours(24)` 与 `LocalDateTime.now()` 比较，超时抛 `BusinessException("评价提交超过24小时，不可操作")`。边界 `now==deadline` 仍可操作。
- csv 工具：`toCsv(List)` 取前 3 条 `String.join(",",...)`；`populateImageList(item)` split `images`→`imageList`。

### 6. Controller（[UserController.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/controller/UserController.java)）
- `POST /api/user/reviews`：返回 `Result<Review>`（原 `Void`，改为返回含 id 的新评价）。
- `GET /api/user/reviews`：不变（service 已富化）。
- 新 `GET /api/user/reviews/{id}` → `getReview`。
- 新 `PUT /api/user/reviews/{id}` → `updateReview`，`@Valid ReviewDTO`。
- 新 `DELETE /api/user/reviews/{id}` → `deleteReview`。

## 前端改动

### 7. api.js（[api.js](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/api.js)）
`API.user` 下新增：`getReview(id)`→GET、`updateReview(id,body)`→PUT、`deleteReview(id)`→DELETE（路径 `/api/user/reviews/{id}`）。`addReview(body)` 与 `listMyReviews()` 已存在。

### 8. 新建 review.html（独立评价页，[review.html](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/review.html)）
遵循 `shop.html?id=` 模式，复用相同 CSS 变量/字体/`.btn-primary`/`.star` 风格。
- **URL**：`review.html?orderId=X`（新建）或 `review.html?reviewId=X`（编辑）。`API.util.getParam` 解析。
- **加载**：编辑模式→`API.user.getReview(reviewId)`；新建→`API.order.detail(orderId)` 取 `order+items`。加载中骨架、错误重试、空 items（仅商家评价）。
- **顶部**：订单号、商家名、下单时间（`API.util.formatTime`）。
- **菜品区**：遍历订单 items，每道菜展示图片+名称+数量+选项；交互式星级评分（点击+滑动）；文字输入；图片上传（最多 3 张，复用 `API.util.uploadImage`，缩略图预览+删除）。编辑模式回填已有评分/文字/图片。
- **商家区**：整体服务评分（必填）星级、配送速度评分星级、文字输入（带简易表情选择行，点击追加 emoji）。
- **底部固定栏**：提交按钮（服务评分未填时 disabled）。新建调 `addReview`，编辑调 `updateReview`。
- **草稿**：localStorage key `review_draft_${orderId}`；输入即记 + 每 30 秒自动保存 + 失焦保存；加载时回填（编辑模式优先用接口数据）；提交成功后清除。
- **校验**：服务评分为空→toast 提示并阻止提交。
- **成功**：模态展示"评价提交成功"，两按钮："返回订单列表"（→`profile.html`）、"查看我的评价"（→`profile.html?tab=comments`）。
- **响应式**：移动端底部固定栏、桌面居中卡片；星级控件支持触摸滑动（pointer/touch 事件）。
- **可复用星级组件**：`createStarRating(container, {value, onChange})`，5 个 `★` span，click 定值、hover 预览、pointermove/touchmove 滑动定值，`aria-label` 可达性。

### 9. profile.html（[profile.html](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/profile.html)）
- **`renderOrders()`（1100-1102 行 COMPLETED 分支）**：由 `reviews` 构建 `reviewedOrderIds` Set。已评价→显示"查看评价"按钮（`switchTab('comments')`，若 24h 内额外显示"修改评价"→`review.html?reviewId=`）；未评价→显示"评价"按钮→`review.html?orderId=`。保留"再次购买""查看详情"。
- **`renderReviews()`（1506-1531 行）重写**：富展示每条评价——订单信息（商家名、订单号、评价时间）、商家评分（服务 ★ + 配送 ★ + 文字）、菜品明细列表（菜名、★、文字、图片缩略图）、编辑/删除按钮（仅 `createTime+24h > now`）。`merchantName` 现由后端返回，原 bug 自愈。删除走 `showConfirm` + `API.user.deleteReview`，成功后从 `reviews` 移除并重渲。
- **`?tab=` 深链**：`init()` 读取 `API.util.getParam('tab')`，存在则 `switchTab(tab)`，支持"查看我的评价"直达。
- 复用 `showConfirm`/`showToast`/`switchTab` 等既有工具。

## 实施顺序

1. 后端：迁移 SQL → 实体/DTO → mapper 接口+XML → service → controller。
2. 执行 `migrate_review_v2.sql` 到 MySQL；重启 Spring Boot。
3. 前端：api.js 方法 → review.html → profile.html 改造。
4. 复制 view 文件到 `target/classes/com/qiukai/view/`。
5. 端到端验证。

## 验证

- **迁移**：mysql 客户端执行迁移脚本，`DESC review`/`SHOW TABLES LIKE 'review_item'` 确认。
- **重启**：`mvn spring-boot:run`，确认 8080 启动无 mapper 报错。
- **新建评价**：登录测试账号 `13888888888 / test1234` → profile 订单 → COMPLETED 订单点"评价" → review.html 填菜品评分/图片/服务/配送/文字 → 提交 → 成功模态 → "查看我的评价"看到新评价（含菜品明细、图片）。
- **编辑**：24h 内点"修改评价" → 改内容/评分 → 保存 → 列表更新。
- **删除**：24h 内删除 → 确认 → 列表移除；该订单"评价"按钮重新出现（可重评）。
- **24h 窗口**：代码审查 `assertWithin24h` 逻辑（无旧数据难实测）。
- **图片上传**：每道菜上传 3 张，缩略图显示；提交后"我的评价"展示图片。
- **草稿**：输入内容 → 刷新页面 → 草稿回填；提交成功后草稿清空。
- **校验**：未填服务评分提交 → toast 阻止。
- **状态**：断网/错误时友好提示；空评价列表显示空状态。
- **响应式**：浏览器移动视口下星级可触摸滑动、底部栏正常。
