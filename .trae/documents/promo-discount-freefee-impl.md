# 折扣优惠与免配送费优惠 — 实施计划

## 概述

为商家后台优惠管理系统新增「折扣优惠（discount）」和「免配送费优惠（freefee）」两种类型，并在用户端（shop.html 展示、checkout.html 结算计算）实时应用。数据库迁移、实体类、Mapper XML 已在上一阶段完成，本计划覆盖剩余的 Service 层与前端改造。

## 当前状态分析（已完成）

* **数据库**：`merchant_promo` 表已 ALTER 添加 6 列（`name, discount_ratio, min_spend, priority, start_time, end_time`），`schema.sql` 已同步。

* **MerchantPromo.java**：已包含全部 6 个新字段 + BigDecimal import。

* **MerchantPromoMapper.xml**：resultMap/allColumns/insert/update 均已覆盖新列，`selectByMerchantId` 已加 `ORDER BY priority DESC, id ASC`。

* **MerchantDetailVO**：已有 `promos` 字段（List<MerchantPromo>），`MenuService.getMerchantDetail` 已返回 promos。

## 当前状态分析（待修改）

* **MerchantService.java L215-242**：`addMyPromo`/`updateMyPromo` 仍强制 `type='fullcut'`，拒绝其他类型。

* **OrderService.java L69-76, L127-139**：`createOrder` 调用 `computeDiscount()` 仅处理满减规则（PromoRule），不识别 MerchantPromo 的 discount/freefee。

* **MenuService.java**：无独立的 `getMerchantPromos` 方法（仅 `getMerchantDetail` 内部调用）。

* **merchant.html L302-409, L870-913**：优惠类型选择器被替换为静态「满减」展示，表单仅含 description 字段，`savePromoBtn` 强制 `type:'fullcut'`。

* **shop.html L290-297**：`buildPromoText` 仅处理 description 与 fullcut，不识别 discount/freefee。

* **checkout.html L543, L582-611, L862-894, L904-914**：`calc()` 仅算满减，`loadMerchant` 未取 promos，`renderPromoRules`/`renderAmount` 仅展示满减。

## 设计决策（已与用户确认）

1. **折扣比例含义**：`discountRatio` = 打折率。`80` = 打8折（用户付80%，优惠20%）；`0` = 免单（全额优惠）；`100` = 无优惠 → **拒绝**。有效范围 `[0, 99]`。

   * 优惠金额公式：`discount = subtotal × (100 - ratio) / 100`（BigDecimal, HALF\_UP, 2位小数）。
2. **叠加规则**：

   * **免配送费**：独立叠加。满足 `minSpend` 门槛即减免配送费。

   * **满减 vs 折扣**：互斥。取优惠金额更大者；金额相等时 `priority` 高者胜出（priority 已在查询时 DESC 排序，遍历取 `>=` 即可实现平局取先出现的高优先级项）。
3. **活动有效期**：`startTime`/`endTime` 均可选；都填时需 `start < end`；当前时间不在区间内则不参与计算。
4. **状态控制**：`status=0` 启用，`status=1` 停用（沿用现有约定）。

## 实施步骤

### 步骤 1：MerchantService.java — 移除 fullcut 限制 + 新增校验

**文件**：`src/main/java/com/qiukai/service/MerchantService.java`

**1a. 新增** **`validatePromo(MerchantPromo)`** **私有方法**（放在优惠管理区块内）：

* 校验 type ∈ {fullcut, discount, freefee}，否则抛 `BusinessException("不支持的优惠类型")`。

* **fullcut**：`description` 必填（非空）。

* **discount**：`name` 必填；`discountRatio` 必填且 `0 <= ratio <= 99`；`minSpend` 选填，若填需 `>= 0`。

* **freefee**：`name` 必填；`minSpend` 必填且 `> 0`。

* **通用**：若 `startTime` 与 `endTime` 都填，需 `startTime.isBefore(endTime)`，否则抛 `BusinessException("生效时间需早于失效时间")`。

* `priority` 若 null 默认 0；`status` 若 null 默认 0。

**1b. 改造** **`addMyPromo`（L215-226）**：

* 移除 `if (!"fullcut".equals(promo.getType()))` 校验。

* 调用 `validatePromo(promo)`。

* 设置 `merchantId`，不再强制 `setType("fullcut")`。

* 设置 `priority`/`status` 默认值。

**1c. 改造** **`updateMyPromo`（L228-242）**：

* 移除 fullcut 校验。

* **禁止修改 type**：忽略 `promo.getType()`（保持 existing.getType()）。

* 对 existing 应用新字段前，先组装一个临时对象做校验：将 existing 的不变字段 + 入参的新字段合并后调用 `validatePromo`。

* 追加字段赋值：`name, discountRatio, minSpend, priority, startTime, endTime`（仅当入参非 null 时覆盖）。

* `description` 赋值时需判空（discount/freefee 可能无 description）。

### 步骤 2：MenuService.java — 新增 getMerchantPromos

**文件**：`src/main/java/com/qiukai/service/MenuService.java`

在 `getPromoRules`（L127-129）下方新增：

```java
public List<MerchantPromo> getMerchantPromos(Long merchantId) {
    return merchantPromoMapper.selectByMerchantId(merchantId);
}
```

供 OrderService 调用，避免 OrderService 直接依赖 merchantPromoMapper。

### 步骤 3：OrderService.java — 重写优惠计算

**文件**：`src/main/java/com/qiukai/service/OrderService.java`

**3a. 新增** **`PromoCalcResult`** **内部类**（类内部，私有静态）：

```java
private static class PromoCalcResult {
    BigDecimal discount = BigDecimal.ZERO;      // 优惠金额（满减或折扣，互斥取大）
    BigDecimal deliveryFee;                      // 实际配送费（可能被免配送费置0）
    String discountType = null;                  // 命中的优惠类型标记："fullcut" / "discount" / null
    boolean freeDelivery = false;                // 是否命中免配送费
}
```

**3b. 新增** **`isPromoActive(MerchantPromo, LocalDateTime)`** **私有方法**：

* `status == 0`（启用）。

* 时间校验：`now` 在 `[startTime, endTime]` 内（start/end 为 null 表示不限制）。

**3c. 用** **`computePromos(Long merchantId, BigDecimal subtotal, BigDecimal baseDeliveryFee)`** **替换** **`computeDiscount`（L127-139）**：

1. 满减优惠：遍历 `menuService.getPromoRules(merchantId)`，取满足 `subtotal >= threshold` 且 `status==0` 的最大 `discount` → `fullcutDiscount`。
2. 折扣优惠：遍历 `menuService.getMerchantPromos(merchantId)` 中 `type=='discount'` 且 `isPromoActive` 的活动；若 `minSpend` 为 null 或 `subtotal >= minSpend`，计算 `subtotal × (100 - ratio) / 100`（HALF\_UP, 2位）作为优惠金额；取最大者 → `discountPromoDiscount`（同时记录该活动的 priority 供平局比较）。
3. 互斥决策：比较 `fullcutDiscount` 与 `discountPromoDiscount`：

   * 若 `discountPromoDiscount > fullcutDiscount`：取折扣，`discountType='discount'`。

   * 若 `fullcutDiscount > discountPromoDiscount`：取满减，`discountType='fullcut'`。

   * 若相等：取 priority 高者（因 Mapper 已按 priority DESC 排序，遍历折扣时首个最大值即为高优先级；满减无 priority 概念，视为 0；比较 `discountPromoPriority >= 0` 即取折扣，否则满减。简化：相等时若存在折扣活动且其 priority > 0 取折扣，否则取满减）。
4. 免配送费：遍历 `type=='freefee'` 且 `isPromoActive` 的活动；若 `minSpend` 为 null 或 `subtotal >= minSpend`，则 `deliveryFee = 0`，`freeDelivery = true`（取首个命中即可）。
5. 返回 `PromoCalcResult`。

**3d. 修改** **`createOrder`（L69-76）**：

```java
PromoCalcResult promo = computePromos(dto.getMerchantId(), subtotal, merchant.getDeliveryFee());
BigDecimal discount = promo.discount;
BigDecimal deliveryFee = promo.deliveryFee;
BigDecimal totalAmount = subtotal.subtract(discount).add(deliveryFee);
```

其余逻辑（order.setDiscount/setDeliveryFee 等）不变。

### 步骤 4：merchant.html — 恢复类型选择 + 条件表单

**文件**：`src/main/java/com/qiukai/view/merchant.html`

**4a. HTML 改造**：

* L306：描述改为「管理满减、折扣、免配送费优惠活动与满减规则，吸引用户下单」。

* L320 表头：改为 `<th>类型</th><th>名称/描述</th><th>详情</th><th>优先级</th><th>有效期</th><th>状态</th><th class="text-right">操作</th>`。

* L395-401 优惠类型静态展示：替换为 `<select id="promoType">` 含三个 option（fullcut 满减 / discount 折扣 / freefee 免配送费）。

* L402 description 字段下方新增条件字段（初始 hidden，由 JS 切换）：

  * `#promoName`（活动名称，discount/freefee 必填，fullcut 隐藏）

  * `#promoDiscountRatio`（折扣率 0-99，仅 discount 显示）

  * `#promoMinSpend`（最低消费/门槛，discount 选填、freefee 必填）

  * `#promoPriority`（优先级，默认 0，全部显示）

  * `#promoStartTime` / `#promoEndTime`（datetime-local，全部显示）

  * `#promoDescription`（满减描述，仅 fullcut 显示）

**4b. JS 改造**：

* 新增 `togglePromoFields(type)`：按 type 显隐上述字段，更新必填提示。

* `openPromoModal(promo)`（L897-902）：设置 `#promoType` 值并触发 `togglePromoFields`；填充 name/ratio/minSpend/priority/startTime/endTime（格式化 datetime-local）。

* `savePromoBtn`（L904-913）：按 type 组装 body（fullcut→{type,description}；discount→{type,name,discountRatio,minSpend?,priority,startTime?,endTime?}；freefee→{type,name,minSpend,priority,startTime?,endTime?}）；前端校验必填项。

* `renderPromos`（L870-880）：表行增加 名称/描述列、详情列（discount 显示「{ratio}折 最低¥{minSpend}」、freefee 显示「满¥{minSpend}免配送费」、fullcut 显示 description）、优先级列、有效期列（格式化 start\~end 或「长期」）。

### 步骤 5：shop.html — buildPromoText 支持新类型

**文件**：`src/main/java/com/qiukai/view/shop.html`

改造 `buildPromoText`（L290-297）：

* `discount`：返回 `${ratio}折优惠`（如 ratio=80 →「8折优惠」，ratio=0 →「免单」），若有 minSpend 追加「(满¥{minSpend}可用)」。

* `freefee`：返回 `满¥{minSpend}免配送费`。

* `fullcut`/其他：保留现有逻辑（description 优先，否则聚合 promoRules）。

### 步骤 6：checkout.html — 结算计算集成

**文件**：`src/main/java/com/qiukai/view/checkout.html`

**6a. 全局状态**（L543 附近）：新增 `let promos = [];`。

**6b. loadMerchant**（L904-914）：追加 `promos = data.promos || [];`。

**6c. calc() 重写**（L582-595）：

```
1. subtotal 来自 cartVO.totalAmount
2. 满减优惠：遍历 promoRules 取满足门槛的最大 discount → fullcutDiscount
3. 折扣优惠：遍历 promos(type=discount, status=0, 时间有效, minSpend满足)
   计算 discount = subtotal * (100 - ratio) / 100，取最大 → discountPromoDiscount
4. 互斥：取 fullcutDiscount 与 discountPromoDiscount 较大者（相等时优先折扣，因 priority 高者通常为折扣活动）
5. 免配送费：遍历 promos(type=freefee, status=0, 时间有效, minSpend满足)，命中则 deliveryFee=0
6. total = max(0, subtotal - discount + deliveryFee)
```

**6d. renderPromoRules 重写**（L598-611）：展示所有类型活动：

* fullcut 规则：现有「满X减Y」标签。

* discount 活动：「{ratio}折」标签，满足 minSpend 高亮。

* freefee 活动：「满X免配送费」标签，满足门槛高亮。

**6e. renderAmount 改造**（L862-894）：

* 优惠金额标签动态化：discount 命中时显示「折扣优惠」/「满减优惠」/「优惠」。

* 配送费行：若 freeDelivery 命中，显示删除线 + 「免配送费」标签。

* 省钱提示：`discount` 金额包含配送费减免时一并展示。

**6f. HTML 改造**（L296-318）：

* L299「满减优惠」标题改为「优惠活动」。

* L301 `#promoRules` 容器保持。

* L311「满减优惠」金额标签 id 化（`#discountLabel`）便于动态切换文案。

* 配送费行加 id（`#deliveryRow`）便于显示删除线/免配送费标签。

### 步骤 7：编译 + 数据库验证 + 端到端测试

**7a. 编译**：`mvn -q compile`（需 `dangerouslyDisableSandbox: true`）。
**7b. 重启服务**：停止旧后台进程，重新 `mvn spring-boot:run`。
**7c. 数据库验证**：确认 merchant\_promo 表 6 列存在（已在上阶段完成，此处仅复核）。
**7d. 端到端测试**（使用测试账号 testmerchant001 / Test\@1234）：

1. 商家后台创建 discount 活动（ratio=80, minSpend=30）→ 保存成功 → 列表显示。
2. 商家后台创建 freefee 活动（minSpend=50）→ 保存成功 → 列表显示。
3. 校验 discount ratio=100 被拒绝；ratio=0 允许。
4. shop.html 查看商家页 → 优惠文案正确显示。
5. checkout.html 加购满 30 元 → 折扣优惠生效（subtotal×20%）。
6. checkout.html 加购满 50 元 → 折扣 + 免配送费同时生效。
7. 加购满 30 且有满减规则（满30减5）→ 比较折扣6元 vs 满减5元 → 取折扣6元。
8. 编辑/删除活动 → 列表更新正确。
9. 停用活动 → 结算不再应用。

## 假设与边界处理

| 场景                              | 处理方式                                     |
| ------------------------------- | ---------------------------------------- |
| discountRatio=0                 | 免单，discount=subtotal，total=0+deliveryFee |
| discountRatio=100               | 校验拒绝（非有效优惠）                              |
| discountRatio=null（discount 类型） | 校验拒绝（必填）                                 |
| minSpend=null（discount）         | 允许，无门槛                                   |
| minSpend=null（freefee）          | 校验拒绝（必填，>0）                              |
| startTime/endTime 仅填一个          | 允许（单边约束）                                 |
| 活动未到生效时间 / 已过期                  | 不参与计算                                    |
| 活动停用（status=1）                  | 不参与计算                                    |
| 满减与折扣优惠金额相等                     | 取折扣（priority 通常更高）；若折扣 priority=0 则取满减   |
| subtotal=0（空购物车）                | discount=0，不触发任何优惠                       |

## 验证清单

* [ ] MerchantService.validatePromo 三类型校验通过

* [ ] OrderService.computePromos 满减+折扣互斥正确

* [ ] OrderService.computePromos 免配送费独立叠加

* [ ] merchant.html 类型选择器切换字段显隐正确

* [ ] merchant.html 三类型 CRUD 数据持久化

* [ ] shop.html 三类型优惠文案展示

* [ ] checkout.html 结算金额计算准确

* [ ] checkout.html 免配送费减免显示

* [ ] 编译无错误，服务正常启动

* [ ] 端到端测试 9 项全部通过

