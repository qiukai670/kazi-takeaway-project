# 商家后台管理系统实施计划

## Context

用户要求构建独立的商家后台管理系统，包括：商家注册改用真实手机号、商家登录支持手机号/用户名双登录、商家后台管理页面(merchant.html)、店铺管理(禁止自标注好评和编辑徽章)、优惠策略管理、菜单管理(含售罄状态)、订单处理(管理员确认后流转至商家，商家接单/派送)。

**探索发现**：大量基础设施已存在：
- 订单流程已完整实现：`PENDING_PAY → PAID →(admin assign)→ PENDING_CONFIRM →(merchant confirm)→ CONFIRMED →(merchant dispatch)→ DELIVERING → COMPLETED`
- `OrderService.merchantConfirmOrder/merchantDispatchOrder` 已有归属校验
- `UserContext.isMerchant()` 已存在
- `MerchantMapper.selectByUserId` 已存在
- `DishMapper.updateSoldOut` 已存在
- `OrderMapper.selectByMerchantId` 已存在
- `PromoRuleMapper/MerchantPromoMapper` 已有完整CRUD
- `shop.html` 售罄显示已实现（灰色+禁用按钮+"售罄"标签）
- `MerchantRegisterDTO` 已有phone字段（@NotBlank + @Pattern验证）
- `login.html` 已发送phone、商家登录标签为"用户名/手机号"、登录后重定向到merchant.html
- `UserService.merchantLogin` 已支持双登录（先查phone再查username）
- `MerchantDTO` 包含 badge 和 isRecommended 字段（商家不可编辑）

**核心待建**：UserService.merchantRegister phone修复 + MerchantController/Service + merchant.html + api.js商家方法

## 实施步骤

### 第1步：修复 UserService.merchantRegister — 使用真实手机号

**文件**: [UserService.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/service/UserService.java) L132-147

将假"999"手机号生成逻辑替换为：
- 检查 `dto.getPhone()` 唯一性：`userMapper.selectByPhone(dto.getPhone()) != null` → 抛出"该手机号已注册"
- 直接 `user.setPhone(dto.getPhone())`

### 第2步：创建 MerchantService — 商家专属业务逻辑

**新文件**: `src/main/java/com/qiukai/service/MerchantService.java`

核心模式：通过 `UserContext.getCurrentUserId()` + `merchantMapper.selectByUserId()` 获取当前商家，所有操作校验归属权。

**方法清单**：
```
// 店铺信息
getMyMerchant()                    → merchantMapper.selectByUserId(currentUserId)
updateMyMerchant(MerchantDTO dto)  → 更新name/logo/cover/category/priceLevel/deliveryTime/minOrder/deliveryFee/tags/status（禁止更新isRecommended和badge）

// 菜品管理（复用DishDTO，所有操作校验dish.merchantId == myMerchantId）
listMyDishes()                     → dishMapper.selectAllByMerchantId(myMerchantId)
addMyDish(DishDTO dto)             → dishMapper.insert（设置merchantId为myMerchantId）
updateMyDish(Long id, DishDTO dto) → 校验归属 → dishMapper.update
deleteMyDish(Long id)              → 校验归属 → 删除规格选项 → dishMapper.deleteById
toggleMyDishShelf(Long id)         → 校验归属 → 切换onShelf
toggleMyDishPopular(Long id)       → 校验归属 → 切换isPopular
toggleMyDishSoldOut(Long id)       → 校验归属 → dishMapper.updateSoldOut

// 订单管理（复用OrderService已有方法，传入myMerchantId）
listMyOrders(String status)        → orderMapper.selectByMerchantId(myMerchantId, status)
getMyOrderDetail(Long id)          → 校验归属 → 返回OrderDetailVO
confirmMyOrder(Long id)            → orderService.merchantConfirmOrder(id, myMerchantId)
dispatchMyOrder(Long id)           → orderService.merchantDispatchOrder(id, myMerchantId)

// 优惠管理 - MerchantPromo（校验promo.merchantId == myMerchantId）
listMyPromos()                     → merchantPromoMapper.selectByMerchantId(myMerchantId)
addMyPromo(MerchantPromo promo)    → 设置merchantId → merchantPromoMapper.insert
updateMyPromo(Long id, MerchantPromo promo) → 校验归属 → merchantPromoMapper.update
toggleMyPromoStatus(Long id)       → 校验归属 → merchantPromoMapper.updateStatus
deleteMyPromo(Long id)             → 校验归属 → merchantPromoMapper.deleteById

// 满减规则 - PromoRule（校验rule.merchantId == myMerchantId）
listMyPromoRules()                 → promoRuleMapper.selectByMerchantId(myMerchantId)
addMyPromoRule(PromoRule rule)     → 设置merchantId → promoRuleMapper.insert
updateMyPromoRule(Long id, PromoRule rule) → 校验归属 → promoRuleMapper.update
toggleMyPromoRuleStatus(Long id)   → 校验归属 → promoRuleMapper.updateStatus
deleteMyPromoRule(Long id)         → 校验归属 → promoRuleMapper.deleteById
```

**权限校验**：每个方法开头调用 `requireMerchant()`（检查 UserContext.isMerchant()）

### 第3步：创建 MerchantController — 商家专属API端点

**新文件**: `src/main/java/com/qiukai/controller/MerchantController.java`

**端点清单**（全部 `/api/merchant/*`，由 MerchantService 处理）：
```
// 店铺信息
GET   /api/merchant/info           → getMyMerchant
PUT   /api/merchant/info           → updateMyMerchant

// 菜品管理
GET   /api/merchant/dishes         → listMyDishes
POST  /api/merchant/dishes         → addMyDish
PUT   /api/merchant/dishes/{id}    → updateMyDish
DELETE /api/merchant/dishes/{id}   → deleteMyDish
PUT   /api/merchant/dishes/{id}/shelf     → toggleMyDishShelf
PUT   /api/merchant/dishes/{id}/popular   → toggleMyDishPopular
PUT   /api/merchant/dishes/{id}/sold-out  → toggleMyDishSoldOut

// 订单管理
GET   /api/merchant/orders         → listMyOrders (可选status参数)
GET   /api/merchant/orders/{id}    → getMyOrderDetail
PUT   /api/merchant/orders/{id}/confirm   → confirmMyOrder
PUT   /api/merchant/orders/{id}/dispatch  → dispatchMyOrder

// 优惠管理 - MerchantPromo
GET   /api/merchant/promos         → listMyPromos
POST  /api/merchant/promos         → addMyPromo
PUT   /api/merchant/promos/{id}    → updateMyPromo
PUT   /api/merchant/promos/{id}/status   → toggleMyPromoStatus
DELETE /api/merchant/promos/{id}   → deleteMyPromo

// 满减规则 - PromoRule
GET   /api/merchant/promo-rules    → listMyPromoRules
POST  /api/merchant/promo-rules    → addMyPromoRule
PUT   /api/merchant/promo-rules/{id}     → updateMyPromoRule
PUT   /api/merchant/promo-rules/{id}/status → toggleMyPromoRuleStatus
DELETE /api/merchant/promo-rules/{id}   → deleteMyPromoRule
```

### 第4步：更新 api.js — 添加商家API方法

**文件**: [api.js](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/api.js)

在 `auth` 对象后新增 `merchant` 对象：
```javascript
const merchant = {
  getInfo()                           → GET /api/merchant/info
  updateInfo(body)                    → PUT /api/merchant/info
  listDishes()                        → GET /api/merchant/dishes
  addDish(body)                       → POST /api/merchant/dishes
  updateDish(id, body)                → PUT /api/merchant/dishes/{id}
  deleteDish(id)                      → DELETE /api/merchant/dishes/{id}
  toggleDishShelf(id)                 → PUT /api/merchant/dishes/{id}/shelf
  toggleDishPopular(id)               → PUT /api/merchant/dishes/{id}/popular
  toggleDishSoldOut(id)               → PUT /api/merchant/dishes/{id}/sold-out
  listOrders(status)                  → GET /api/merchant/orders
  getOrderDetail(id)                  → GET /api/merchant/orders/{id}
  confirmOrder(id)                    → PUT /api/merchant/orders/{id}/confirm
  dispatchOrder(id)                   → PUT /api/merchant/orders/{id}/dispatch
  listPromos()                        → GET /api/merchant/promos
  addPromo(body)                      → POST /api/merchant/promos
  updatePromo(id, body)               → PUT /api/merchant/promos/{id}
  togglePromoStatus(id)               → PUT /api/merchant/promos/{id}/status
  deletePromo(id)                     → DELETE /api/merchant/promos/{id}
  listPromoRules()                    → GET /api/merchant/promo-rules
  addPromoRule(body)                  → POST /api/merchant/promo-rules
  updatePromoRule(id, body)           → PUT /api/merchant/promo-rules/{id}
  togglePromoRuleStatus(id)           → PUT /api/merchant/promo-rules/{id}/status
  deletePromoRule(id)                 → DELETE /api/merchant/promo-rules/{id}
};
```

### 第5步：创建 merchant.html — 商家后台管理页面

**新文件**: `src/main/java/com/qiukai/view/merchant.html`

**设计参考**: [admin.html](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/admin.html)（Tailwind CSS + 自定义CSS变量 cream/coffee/orange 主题）

**页面结构**：
- 顶部导航栏（毛玻璃效果，显示商家名称、退出按钮）
- 左侧侧边栏：4个模块导航
  1. 店铺信息 — 编辑店铺名称、Logo、封面、分类、配送信息（不显示好评商家和徽章编辑）
  2. 菜单管理 — 菜品列表、新增/编辑/删除、上下架/人气/售罄切换
  3. 订单管理 — 订单列表（按状态筛选）、订单详情、接单/派送操作
  4. 优惠管理 — 优惠活动列表+满减规则管理（启用/停用/编辑/删除）
- 右侧内容区：数据表格 + 模态框表单

**关键交互**：
- 页面加载时检查登录状态，非商家(role≠2)重定向到login.html
- 数据表格使用admin.html相同的样式（data-table, status-pill, action-btn, toggle开关）
- 模态框用于编辑店铺信息、新增/编辑菜品、新增/编辑优惠规则
- 订单状态显示：待确认(橙)→已确认(蓝)→配送中(紫)→已完成(绿)

### 第6步：编译验证

```bash
mvn compile
```

### 第7步：端到端测试

1. **商家注册**：使用真实手机号注册 → 验证user表phone为真实号码
2. **商家登录**：手机号登录 + 用户名登录均成功
3. **商家后台**：登录后跳转merchant.html
4. **店铺信息**：查看/编辑（验证无好评商家和徽章字段）
5. **菜单管理**：新增/编辑/删除菜品，切换售罄状态 → shop.html验证售罄显示
6. **订单管理**：管理员分配订单后 → 商家后台可见 → 接单 → 派送
7. **优惠管理**：新增满减规则 → 用户下单验证折扣计算

## 复用的现有代码

| 组件 | 文件路径 | 说明 |
|------|----------|------|
| OrderService.merchantConfirmOrder | service/OrderService.java L298 | 商家接单（待确认→已确认） |
| OrderService.merchantDispatchOrder | service/OrderService.java L317 | 商家派送（已确认→配送中） |
| MerchantMapper.selectByUserId | mapper/MerchantMapper.java L18 | 按userId查商家 |
| DishMapper.updateSoldOut | mapper/DishMapper.java L39 | 切换售罄 |
| OrderMapper.selectByMerchantId | mapper/OrderMapper.java L25 | 商家订单列表 |
| PromoRuleMapper (全部CRUD) | mapper/PromoRuleMapper.java | 满减规则增删改查 |
| MerchantPromoMapper (全部CRUD) | mapper/MerchantPromoMapper.java | 优惠活动增删改查 |
| UserContext.isMerchant | interceptor/UserContext.java L35 | 商家身份判断 |
| shop.html售罄显示 | view/shop.html L448-471 | 已实现，无需修改 |
| OrderService.computeDiscount | service/OrderService.java L127 | 下单时满减计算 |
| AdminService.toggleSoldOut | service/AdminService.java L295 | 参考实现模式 |

## 不需要修改的部分

- **shop.html** — 售罄显示已完整实现（灰色样式+禁用按钮+"售罄"标签）
- **OrderService** — 订单流程已完整（admin assign → merchant confirm → merchant dispatch）
- **AdminController** — admin只有assign权限，无confirm/dispatch，符合要求
- **login.html** — 商家注册已有phone字段，登录已支持双登录，已重定向到merchant.html
- **UserService.merchantLogin** — 已支持phone OR username双登录
