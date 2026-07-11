# 折扣优惠与免配送费优惠功能 - 端到端验证计划

## Summary

所有代码实现已完成（Service 层 + 3 个前端页面），编译成功，服务器运行中。本计划聚焦于完成剩余的端到端验证：订单计算流程、互斥逻辑、免配送费叠加、CRUD 操作及前端页面验证，并修复发现的问题。

## Current State Analysis

### 已完成（代码实现 + 部分测试）

| 模块 | 文件 | 状态 |
|------|------|------|
| MerchantService | `src/main/java/com/qiukai/service/MerchantService.java` | ✅ validatePromo 校验 discount(0-99)/freefee(>0) |
| MenuService | `src/main/java/com/qiukai/service/MenuService.java` | ✅ getMerchantPromos 方法 |
| OrderService | `src/main/java/com/qiukai/service/OrderService.java` | ✅ computePromos 互斥+叠加逻辑 |
| merchant.html | `src/main/java/com/qiukai/view/merchant.html` | ✅ 类型选择器+条件表单 |
| shop.html | `src/main/java/com/qiukai/view/shop.html` | ✅ buildPromoText 三类型支持 |
| checkout.html | `src/main/java/com/qiukai/view/checkout.html` | ✅ calc()+renderPromoRules+renderAmount |

### 已通过的 API 测试
- ✅ 编译成功
- ✅ 服务器启动（端口 8080，后台 job 运行中）
- ✅ 商家登录成功（Token 获取）
- ✅ discount 优惠创建（ratio=80, minSpend=30）
- ✅ freefee 优惠创建（minSpend=50）
- ✅ ratio=0 免单创建成功
- ✅ ratio=100 校验正确拒绝
- ✅ merchant detail API 返回 promos 列表正确

### 当前数据库优惠数据（merchantId=13）
- id=14: discount, ratio=80, minSpend=30, priority=5, status=0（启用，满30打8折）
- id=15: freefee, minSpend=50, priority=3, status=0（启用，满50免配送费）
- id=16: discount, ratio=0, priority=1, status=0（启用，免单）
- id=12/13: fullcut, status=0（测试满减数据）
- id=10: type="满减"(中文旧数据), status=1（停用）
- PromoRules: 1 条（满30减5）

### 待验证项
1. ⬜ 订单计算流程（加购物车→创建订单→验证折扣/免配送费计算）
2. ⬜ 互斥逻辑（满减 vs 折扣取大者）
3. ⬜ 免配送费叠加验证
4. ⬜ CRUD 操作（编辑/删除/停用）
5. ⬜ 前端页面验证（merchant.html / shop.html / checkout.html）

## Proposed Changes

### Step 1: 准备测试用户并获取菜品列表

**目标**：获取一个普通用户 Token 和 merchantId=13 的菜品列表，为订单测试做准备。

**操作**：
1. 通过 `POST /api/user/register` 注册测试用户（或使用已有用户 `POST /api/user/login`）
2. 调用 `GET /api/dishes?merchantId=13` 获取菜品列表，记录菜品 ID 和价格
3. 调用 `GET /api/merchant/13` 确认当前 promos 和 promoRules 数据

**预期**：获得用户 Token + 菜品列表 + 配送费基数

### Step 2: 订单计算流程测试 - 折扣优惠

**目标**：验证折扣优惠计算准确（打8折：ratio=80 → 用户付80%，优惠20%）

**前置**：停用 id=16（免单 promo），保留 id=14（ratio=80, minSpend=30）启用

**操作**：
1. 商家 Token 调用 `PUT /api/merchant/promos/16/status` 停用免单 promo
2. 用户 Token 调用 `POST /api/cart` 加入菜品，小计 ≈ 40 元（满足 minSpend=30）
3. 调用 `GET /api/cart?merchantId=13` 确认购物车
4. 调用 `POST /api/orders` 创建订单（body: `{"merchantId":13}`）
5. 验证返回的 order 对象：
   - `subtotal` = 菜品小计
   - `discount` = subtotal × 20 / 100（BigDecimal HALF_UP 2位）
   - `deliveryFee` = 商家配送费（不满足50，不免配送费）
   - `totalAmount` = subtotal - discount + deliveryFee

**预期**：折扣计算正确，满减(5元) < 折扣(8元@40)，取折扣

### Step 3: 互斥逻辑验证 - 满减 vs 折扣

**目标**：验证满减与折扣互斥时取优惠更大者

**场景 A（折扣 > 满减）**：subtotal=40
- 满减：满30减5 → 5元
- 折扣：40×20%=8元
- 预期：discount=8（折扣胜出）

**场景 B（满减 > 折扣）**：调整满减规则或折扣率使满减更大
- 可通过创建满减规则（如满40减10）使满减 > 折扣
- 预期：discount=10（满减胜出）

**场景 C（相等时 priority 决策）**：
- 调整使满减=折扣金额，验证 priority 高者胜出

### Step 4: 免配送费叠加验证

**目标**：验证免配送费独立叠加到折扣/满减之上

**操作**：
1. 确保 id=15（freefee, minSpend=50）启用
2. 用户加入菜品使 subtotal ≥ 50
3. 创建订单
4. 验证：
   - `discount` = 折扣优惠金额（仍生效）
   - `deliveryFee` = 0（免配送费触发）
   - `totalAmount` = subtotal - discount + 0

**预期**：折扣 + 免配送费同时生效

### Step 5: 免单（ratio=0）边界测试

**目标**：验证 ratio=0 时折扣=小计，实付=0

**操作**：
1. 启用 id=16（ratio=0 免单）
2. 停用 id=14（避免干扰）
3. 用户加入菜品，创建订单
4. 验证：
   - `discount` = subtotal（全额免单）
   - `totalAmount` = 0 + deliveryFee（若免配送费也触发则为0）

**预期**：免单正确生效

### Step 6: CRUD 操作验证

**目标**：验证编辑/删除/停用功能

**操作**：
1. **编辑**：`PUT /api/merchant/promos/14` 修改 ratio=85，验证返回成功且 detail 中 ratio 已更新
2. **停用**：`PUT /api/merchant/promos/14/status` 设 status=1，验证订单不再享受该折扣
3. **启用**：`PUT /api/merchant/promos/14/status` 设 status=0，验证恢复
4. **删除**：`DELETE /api/merchant/promos/{id}` 删除一个测试 promo，验证 detail 中不再出现

### Step 7: 前端页面验证

**目标**：验证三个前端页面正确渲染和交互

**merchant.html 验证**：
- 优惠管理页面显示三种类型选择器
- 切换类型时条件表单正确显示/隐藏（fullcut→描述, discount→名称+折扣率+最低消费, freefee→名称+门槛）
- 新建/编辑弹窗字段填充正确
- 列表表格显示类型、名称/描述、详情、优先级、有效期、状态、操作

**shop.html 验证**：
- 商家详情页优惠规则模块正确显示折扣/免配送费/满减文案
- 折扣显示为"X折优惠"，ratio=0 显示"免单"
- 免配送费显示为"满¥X免配送费"

**checkout.html 验证**：
- 结算页优惠活动条正确显示所有类型规则
- 满足条件的规则高亮（active），未满足的灰色
- 金额计算正确：优惠金额、配送费（免配送费时显示0或"免费"标识）、实付金额
- 响应式布局在不同屏幕尺寸下正常

### Step 8: 清理测试数据

**操作**：
1. 删除测试创建的临时 promos
2. 恢复 promo 状态到合理初始值
3. 清理测试用户的购物车和订单（如需要）
4. 汇总测试结果

## Assumptions & Decisions

1. **测试用户**：使用 `POST /api/user/register` 注册测试用户，或复用已有用户账号
2. **互斥规则**：满减与折扣互斥取优惠更大者；金额相等时 priority 高者胜（满减视为 priority=0）
3. **叠加规则**：免配送费独立叠加到满减或折扣之上
4. **折扣率含义**：80=打8折（用户付80%），0=免单，有效范围 [0, 99]
5. **计算精度**：后端 BigDecimal HALF_UP 2位小数；前端 JS 浮点计算 + money() 格式化
6. **服务器**：后台 job 运行中（job-98af805b6f5f4ff7967470f74a453784），端口 8080
7. **测试 Token**：商家 Token 已知（0ff905181a838255835f63741ad526a20df0d92228a6441bd7534c02f65b7901），可能需重新登录获取新 Token

## Verification Steps

1. 每个测试场景记录 API 请求和响应
2. 对比预期值与实际值，标记 ✅/❌
3. 发现的问题立即修复并重新验证
4. 最终汇总所有测试结果，确认功能完整可用

## Risk & Mitigation

- **Token 过期**：若 API 返回 401，重新登录获取新 Token
- **端口占用**：若服务器停止，重新启动 `mvn spring-boot:run`
- **测试数据干扰**：id=16（免单）无 minSpend 限制，会影响所有订单计算，测试时按需停用/启用
- **PowerShell JSON 转义**：使用 `@file.json` 方式发送请求体，文件用 `Out-File -Encoding ascii` 写入
- **终端中文乱码**：API 返回的中文在终端可能显示为 ??，不影响数据正确性
