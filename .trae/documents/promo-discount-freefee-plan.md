# 折扣优惠与免配送费优惠功能实现计划

## Context

当前商家后台优惠管理仅支持满减类型（上一会话中限制了非 fullcut 类型）。用户要求新增两种**实际参与订单计算**的优惠类型：折扣优惠（按打折率减免）和免配送费优惠（满足门槛免配送费）。两种新类型需包含活动名称、优先级、有效期等配置，并在 shop.html 展示和 checkout.html 结算时实时计算。

**已确认的设计决策：**
- 折扣比例 = 打折率（80=打8折，用户付80%，优惠20%；0=免单；100=无优惠，拒绝）
- 叠加规则：免配送费独立叠加；满减与折扣互斥，取优惠金额更大者，金额相等时 priority 高者胜出

## 实施步骤

### 1. 数据库迁移

手动执行 ALTER TABLE（无 Flyway），并更新 `src/main/resources/schema.sql` L86-95 的 CREATE TABLE：

```sql
ALTER TABLE merchant_promo
  ADD COLUMN name           VARCHAR(64)  DEFAULT NULL COMMENT '活动名称'        AFTER description,
  ADD COLUMN discount_ratio INT          DEFAULT NULL COMMENT '折扣率0-99(80=打8折)' AFTER name,
  ADD COLUMN min_spend      DECIMAL(8,2) DEFAULT NULL COMMENT '最低消费门槛'    AFTER discount_ratio,
  ADD COLUMN priority       INT          NOT NULL DEFAULT 0 COMMENT '优先级(大优先)' AFTER min_spend,
  ADD COLUMN start_time     DATETIME     DEFAULT NULL COMMENT '生效时间'        AFTER priority,
  ADD COLUMN end_time       DATETIME     DEFAULT NULL COMMENT '失效时间'        AFTER start_time;
```

### 2. 实体变更 — `src/main/java/com/qiukai/entity/MerchantPromo.java`

新增 6 个字段：`name`(String)、`discountRatio`(Integer)、`minSpend`(BigDecimal)、`priority`(Integer)、`startTime`(LocalDateTime)、`endTime`(LocalDateTime)。

### 3. Mapper XML — `src/main/resources/mapper/MerchantPromoMapper.xml`

- resultMap：追加 6 个 `<result>` 映射
- allColumns：追加 `name, discount_ratio, min_spend, priority, start_time, end_time`
- selectByMerchantId：追加 `ORDER BY priority DESC, id ASC`（让优先级高的排在前面，免配送费取第一个时即为最高优先级）
- insert：追加 6 个字段
- update：`<set>` 中追加 6 个 `<if>` 动态字段

### 4. Service 层

#### 4a. `src/main/java/com/qiukai/service/MerchantService.java` L215-242

- **移除** fullcut-only 限制（删除 `if (!"fullcut".equals(...))` 校验）
- **新增** `validatePromo(MerchantPromo)` 私有方法，按类型校验：
  - fullcut：description 必填
  - discount：name 必填，discountRatio 必填且 0-99，minSpend 选填但 >=0
  - freefee：name 必填，minSpend 必填且 >0
  - 所有类型：若 startTime/endTime 都填则 start < end
- `updateMyPromo`：禁止修改 type（防止字段不一致），追加新字段赋值

#### 4b. `src/main/java/com/qiukai/service/MenuService.java`

新增 `getMerchantPromos(Long merchantId)` 方法，委托 `merchantPromoMapper.selectByMerchantId`，供 OrderService 调用。

#### 4c. `src/main/java/com/qiukai/service/OrderService.java` L69-76, L127-139（核心）

将 `computeDiscount()` 替换为 `computePromos()`，返回 `PromoCalcResult{discount, deliveryFee, discountType, freeDelivery}`：

1. **满减**：遍历 PromoRule，取满足门槛的最大优惠（现有逻辑）
2. **折扣**：遍历 MerchantPromo type=discount，校验生效状态+minSpend，计算 `discount = subtotal * (100 - ratio) / 100`（BigDecimal，HALF_UP，保留2位），取优惠最大者
3. **互斥**：比较满减 vs 折扣，取金额更大者；相等时 priority 高者胜（满减隐式 priority=0）
4. **免配送费**：遍历 MerchantPromo type=freefee，校验生效状态+minSpend>=门槛，满足则 deliveryFee=0
5. `totalAmount = subtotal - discount + deliveryFee`

新增 `isPromoActive(promo, now)` 私有方法：status==0 且在有效期内。

### 5. 前端 merchant.html

#### 5a. 优惠模态框（L387-411）
- 恢复 `<select id="promoType">`，选项：满减/折扣优惠/免配送费
- 新增条件字段：活动名称、折扣率（仅discount）、最低消费/门槛、优先级、有效期（datetime-local）
- 保留优惠描述字段（仅fullcut）
- JS `togglePromoFields(type)` 按类型显隐字段

#### 5b. openPromoModal（L897-902）
- 填充所有新字段，编辑时 `promoType.disabled = true`
- 新增 `formatDateTimeLocal(dt)` 辅助函数（兼容 LocalDateTime 数组序列化）

#### 5c. savePromoBtn（L904-913）
- 按类型构建 body，前端校验（name/ratio/minSpend）

#### 5d. renderPromos（L866-883）+ 表头（L320）
- 表头追加：名称、详情、优先级、有效期 列
- 详情列按类型差异化：fullcut 显示 description，discount 显示"打X折(满¥Y)"，freefee 显示"满¥X免配送费"

#### 5e. 模块描述（L306）
- 改为"管理满减/折扣/免配送费优惠活动与满减规则"

### 6. 前端 shop.html — `buildPromoText()` (L290-297)

按类型生成展示文案：
- discount：`打${ratio}折` 或 `满¥${minSpend}打${ratio}折`（ratio=0 显示"免单"）
- freefee：`满¥${minSpend}免配送费`

### 7. 前端 checkout.html

#### 7a. 加载数据（L904-914）
- `loadMerchant()` 中追加 `promos = data.promos || []`

#### 7b. calc() 重写（L582-595）
- 与后端 `computePromos()` 完全一致的算法
- 新增 `parsePromoTime(t)` 和 `isPromoActive(p)` 辅助函数
- 返回 `{subtotal, discount, deliveryFee, total, discountType, freeDelivery}`

#### 7c. renderPromoRules（L598-611）
- 展示满减 + 折扣 + 免配送费 所有活动标签，满足条件的高亮

#### 7d. renderAmount（L862-894）
- 优惠标签动态：discountType 决定显示"折扣优惠"/"满减优惠"
- 免配送费时配送费显示删除线 + "免"标签
- 省钱提示汇总折扣+免配送费

#### 7e. HTML 调整（L296-318）
- "满减优惠"标题改为"优惠活动"
- discountLabel 加 id
- 配送费 span 加 id="deliveryFeeContainer" 供动态渲染

## 边界情况

| 场景 | 处理 |
|------|------|
| ratio=0（免单） | 允许，discount=subtotal，商品部分实付0 |
| ratio=100（无优惠） | 后端前端均拒绝 |
| 过期/未生效 | isPromoActive() 跳过 |
| 满减==折扣金额 | priority 高者胜，满减隐式 priority=0 |
| 多个折扣优惠 | 取优惠金额最大者 |
| 多个免配送费 | 取优先级最高者（已按 priority DESC 排序） |
| 编辑时改类型 | 禁止，需删除重建 |
| 现有 fullcut 数据 | 新字段 NULL，不影响 |

## 验证方式

1. **编译**：`mvn clean compile -DskipTests`
2. **重启服务器**，执行 ALTER TABLE 迁移
3. **后端 API 测试**（curl）：
   - 商家登录 → 新增 discount 优惠（ratio=80, minSpend=30）→ 200
   - 新增 freefee 优惠（minSpend=25）→ 200
   - 新增 ratio=100 → 500 拒绝
   - 新增 freefee minSpend=0 → 500 拒绝
4. **前端验证**：
   - merchant.html：优惠模态框类型切换、条件字段显隐、表格渲染新列
   - shop.html：商家头部显示折扣/免配送费文案
   - checkout.html：购物车满足门槛时折扣/免配送费正确计算，价格明细正确展示
5. **端到端计算**：创建订单，验证 totalAmount = subtotal - max(满减,折扣) + (免配送费?0:配送费)
