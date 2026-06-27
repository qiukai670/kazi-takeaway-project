# 个人中心"我的订单"商家Logo替换 + 订单详情模态框

## 任务摘要

在个人中心 `profile.html` 的"我的订单"模块中，将订单列表项当前显示的随机商品图片（`picsum.photos/seed/order{id}`）替换为对应商家的品牌 logo；同时新建一个订单详情模态框（取代当前仅 toast 提示且有 bug 的 `viewOrderDetail`），详情页头部同样展示商家 logo。Logo 通过后端 JOIN merchant 表获取，前端沿用收藏夹模块的 `onerror` 兜底加载机制，保持尺寸/比例/布局与原设计一致。

## 当前状态分析（已 Phase 1 探索确认）

### 已完成
- **`Order.java`**（L20）：已新增 `private String merchantLogo;` 字段（JOIN 投影字段，非持久化）。

### 待修改

1. **`src/main/resources/mapper/OrderMapper.xml`**
   - `resultMap`（L5-23）：已映射 `username`，但**未映射 `merchantLogo`**。
   - `allColumnsWithUser` 片段（L30-34）：仅含 `u.username AS username`，**无 merchant logo**。
   - `selectById`（L43-47）、`selectAll`（L67-74）：已 LEFT JOIN user，**未 JOIN merchant**。
   - `selectByUserId`（L53-58）：仍是单表查询（用户侧订单列表用），**既无 user 也无 merchant JOIN** → 用户列表拿不到 `merchantLogo`。

2. **`src/main/java/com/qiukai/view/profile.html`**
   - `renderOrders()`（L1072-1141）：
     - L1095 `const imgSeed = 'order' + oid;`
     - L1119 `<img src="https://picsum.photos/seed/${imgSeed}/120/120" alt="订单图" class="w-16 h-16 rounded-2xl object-cover flex-shrink-0">` ← 需替换为商家 logo
   - `viewOrderDetail(id)`（L1186-1201）：仅 `showToast`，且**有 bug**：直接访问 `detail.orderNo`/`detail.totalAmount`，但接口返回结构是 `{order:{...}, items:[...]}`（`OrderDetailVO`），应为 `detail.order.*`。需重写为模态框。

### 可复用基础设施（已确认）
- 模态框：`openModal(id)`/`closeModal(id)` 在 L787-794；`.modal-mask`/`.modal-box` CSS 在 L206-231；已有模态框 `avatarModal`(L602)/`addressModal`(L626)/`bankModal`(L675)/`confirmModal`(L738) 可参照。
- 收藏夹 logo 加载模式（L1451/L1470）：`const logo = f.logo || 'https://picsum.photos/seed/logo${id}/100/100'` + `<img src="${logo}" ... onerror="this.onerror=null;this.src='https://picsum.photos/seed/logofb${id}/100/100'">`。
- `API.order.detail(id)`（api.js L206）：返回 `OrderDetailVO {order, items}`。
- `API.util.escapeHtml`/`API.util.money`/`API.util.formatTime`、`statusText(s)`/`statusClass(s)`（L861-882）均可用。
- `Merchant.java` L16 有 `logo` 字段；`schema.sql` merchant 表有 `logo VARCHAR(512)`，种子数据如 `https://picsum.photos/seed/logo_burger/200/200`。
- `OrderItem` 字段：`id, orderId, dishId, dishName, dishImage, unitPrice, qty, optionsText, subtotal`。
- `getOrderDetail`（OrderService L239-246）调用 `selectById` → 改 JOIN 后 `order.merchantLogo` 自动填充，无需改 Service/Controller。

## 实施步骤

### 步骤 1：修改 `OrderMapper.xml`（后端填充 merchantLogo）

**1a. resultMap 增加映射**（L22 后追加一行）：
```xml
<result property="username" column="username"/>
<result property="merchantLogo" column="merchant_logo"/>
```

**1b. 重命名片段 `allColumnsWithUser` → `allColumnsRich`，追加 merchant logo 列**：
```xml
<sql id="allColumnsRich">
    o.id, o.order_no, o.user_id, o.merchant_id, o.merchant_name, o.subtotal, o.discount, o.delivery_fee,
    o.total_amount, o.status, o.note, o.pay_method, o.address_snapshot, o.pay_time, o.create_time, o.update_time,
    u.username AS username, m.logo AS merchant_logo
</sql>
```
> 原 `allColumnsWithUser` 片段删除（被 `allColumnsRich` 取代）。`allColumns`（单表版）保留不动，供 `selectByOrderNo`/`selectByMerchantId` 等不需要富字段的查询使用。

**1c. `selectById` 双 JOIN**（用户侧详情 + 管理员详情都用它）：
```xml
<select id="selectById" resultMap="orderResultMap">
    SELECT <include refid="allColumnsRich"/>
    FROM orders o
    LEFT JOIN user u ON o.user_id = u.id
    LEFT JOIN merchant m ON o.merchant_id = m.id
    WHERE o.id = #{id}
</select>
```

**1d. `selectAll` 双 JOIN**（管理员列表）：
```xml
<select id="selectAll" resultMap="orderResultMap">
    SELECT <include refid="allColumnsRich"/>
    FROM orders o
    LEFT JOIN user u ON o.user_id = u.id
    LEFT JOIN merchant m ON o.merchant_id = m.id
    <where>
        <if test="status != null and status != ''">AND o.status = #{status}</if>
    </where>
    ORDER BY o.create_time DESC
</select>
```

**1e. `selectByUserId` 改用 `allColumnsRich` + 双 JOIN**（用户侧订单列表，关键：让前端列表拿到 `merchantLogo`）：
```xml
<select id="selectByUserId" resultMap="orderResultMap">
    SELECT <include refid="allColumnsRich"/>
    FROM orders o
    LEFT JOIN user u ON o.user_id = u.id
    LEFT JOIN merchant m ON o.merchant_id = m.id
    WHERE o.user_id = #{userId}
    <if test="status != null and status != ''">AND o.status = #{status}</if>
    ORDER BY o.create_time DESC
</select>
```
> 用 LEFT JOIN 保证即使 user/merchant 软删也能查出订单，logo 为 null 时前端 onerror 兜底。

### 步骤 2：修改 `profile.html` `renderOrders()`（列表图替换为商家 logo）

**2a.** 删除 L1095 `const imgSeed = 'order' + oid;`，改为：
```js
const logo = o.merchantLogo || `https://picsum.photos/seed/logo${oid}/120/120`;
```

**2b.** 替换 L1119 的 `<img>` 标签（保持原 class `w-16 h-16 rounded-2xl object-cover flex-shrink-0` 完全不变，仅换 src/alt/加 onerror 兜底）：
```html
<img src="${logo}" alt="${shop} logo" class="w-16 h-16 rounded-2xl object-cover flex-shrink-0" onerror="this.onerror=null;this.src='https://picsum.photos/seed/logofb${oid}/120/120'">
```
> 尺寸/比例/布局与原设计 100% 一致；onerror 兜底沿用收藏夹模式，保证任何屏幕/设备都能正常显示。

### 步骤 3：新建订单详情模态框 + 重写 `viewOrderDetail()`

**3a. 在 `confirmModal` 之后、`toastContainer` 之前（约 L750）新增模态框 markup**：
```html
<!-- ===== 订单详情模态框 ===== -->
<div class="modal-mask" id="orderDetailModal">
  <div class="modal-box" style="max-width: 520px;">
    <div id="orderDetailContent"></div>
    <div class="flex gap-3 mt-6 justify-end">
      <button class="btn-outline text-sm py-2.5 px-5" onclick="closeModal('orderDetailModal')">关闭</button>
    </div>
  </div>
</div>
```

**3b. 重写 `viewOrderDetail(id)`（L1186-1201）**：
- 调 `API.order.detail(id)`，正确解构 `detail.order` 与 `detail.items`（修掉原 bug）。
- 头部：商家 logo（取 `o.merchantLogo`，onerror 兜底）+ 商家名 + 状态标签 + 订单号/下单时间。
- 菜品明细：每项 `dishImage` 缩略图 + `dishName` + `optionsText` + `¥unitPrice × qty` + 小计。
- 金额明细：商品小计 / 优惠减免 / 配送费 / 实付金额（突出橙色）。
- 订单信息：收货地址快照 / 支付方式（BALANCE→余额 等轻量映射）/ 支付时间（有则显示）/ 订单留言。
- 所有用户/商家提供的字符串经 `API.util.escapeHtml` 转义防 XSS；`openModal('orderDetailModal')` 打开。
- 支付方式映射用内联小函数 `payMethodText(m)`：`{BALANCE:'余额支付', WECHAT:'微信支付', ALIPAY:'支付宝', CARD:'银行卡'}`，未命中回退原值。

完整重写代码：
```js
function payMethodText(m) {
  return { BALANCE: '余额支付', WECHAT: '微信支付', ALIPAY: '支付宝', CARD: '银行卡' }[m] || m || '-';
}
async function viewOrderDetail(id) {
  showToast('正在加载订单详情...');
  try {
    const detail = await API.order.detail(id);
    const o = detail.order || {};
    const items = detail.items || [];
    const logo = o.merchantLogo || `https://picsum.photos/seed/logo${id}/120/120`;
    const shop = API.util.escapeHtml(o.merchantName || '未知商家');
    const orderNo = API.util.escapeHtml(o.orderNo || id);
    const amount = API.util.money(o.totalAmount);
    const subtotal = API.util.money(o.subtotal);
    const discount = API.util.money(o.discount);
    const deliveryFee = API.util.money(o.deliveryFee);
    const time = API.util.formatTime(o.createTime);
    const payTime = API.util.formatTime(o.payTime);
    const st = o.status;
    const note = o.note ? API.util.escapeHtml(o.note) : '无';
    const payMethod = API.util.escapeHtml(payMethodText(o.payMethod));
    const addr = o.addressSnapshot ? API.util.escapeHtml(o.addressSnapshot) : '无';

    const itemsHtml = items.length ? items.map(it => {
      const name = API.util.escapeHtml(it.dishName || '菜品');
      const img = it.dishImage || `https://picsum.photos/seed/dish${it.dishId}/80/80`;
      const opt = it.optionsText ? API.util.escapeHtml(it.optionsText) : '';
      const qty = it.qty || 1;
      const price = API.util.money(it.unitPrice);
      const sub = API.util.money(it.subtotal);
      return `
      <div class="flex items-center gap-3 py-3 border-b" style="border-color: var(--beige);">
        <img src="${img}" alt="${name}" class="w-12 h-12 rounded-xl object-cover flex-shrink-0" onerror="this.onerror=null;this.src='https://picsum.photos/seed/dishfb${it.dishId}/80/80'">
        <div class="flex-1 min-w-0">
          <p class="font-semibold text-sm truncate" style="color: var(--coffee);">${name}</p>
          ${opt ? `<p class="text-xs truncate" style="color: var(--brown-mid);">${opt}</p>` : ''}
        </div>
        <div class="text-right flex-shrink-0">
          <p class="text-xs" style="color: var(--brown-mid);">¥${price} × ${qty}</p>
          <p class="font-semibold text-sm" style="color: var(--coffee);">¥${sub}</p>
        </div>
      </div>`;
    }).join('') : `<p class="text-sm py-4 text-center" style="color: var(--brown-mid);">暂无菜品明细</p>`;

    document.getElementById('orderDetailContent').innerHTML = `
      <div class="flex items-center gap-4 mb-5 pb-5 border-b" style="border-color: var(--beige);">
        <img src="${logo}" alt="${shop} logo" class="w-16 h-16 rounded-2xl object-cover flex-shrink-0" onerror="this.onerror=null;this.src='https://picsum.photos/seed/logofb${id}/120/120'">
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1 flex-wrap">
            <h3 class="font-cn-display text-xl truncate" style="color: var(--coffee);">${shop}</h3>
            <span class="status-tag ${statusClass(st)} flex-shrink-0">${statusText(st)}</span>
          </div>
          <p class="text-xs" style="color: var(--brown-mid);">订单号：${orderNo} · 下单时间：${time}</p>
        </div>
      </div>
      <div class="mb-5">
        <p class="text-xs font-bold uppercase tracking-wider mb-2" style="color: var(--brown-mid);">菜品明细</p>
        ${itemsHtml}
      </div>
      <div class="space-y-2 mb-5 text-sm">
        <p class="text-xs font-bold uppercase tracking-wider mb-2" style="color: var(--brown-mid);">金额明细</p>
        <div class="flex justify-between" style="color: var(--brown-mid);"><span>商品小计</span><span>¥${subtotal}</span></div>
        <div class="flex justify-between" style="color: var(--brown-mid);"><span>优惠减免</span><span>-¥${discount}</span></div>
        <div class="flex justify-between" style="color: var(--brown-mid);"><span>配送费</span><span>¥${deliveryFee}</span></div>
        <div class="flex justify-between font-semibold pt-2 border-t" style="border-color: var(--beige); color: var(--coffee);">
          <span>实付金额</span><span class="font-display text-xl" style="color: var(--orange);">¥${amount}</span>
        </div>
      </div>
      <div class="space-y-2 text-sm">
        <p class="text-xs font-bold uppercase tracking-wider mb-2" style="color: var(--brown-mid);">订单信息</p>
        <div class="flex justify-between gap-3" style="color: var(--brown-mid);"><span class="flex-shrink-0">收货地址</span><span class="text-right" style="color: var(--coffee);">${addr}</span></div>
        <div class="flex justify-between gap-3" style="color: var(--brown-mid);"><span class="flex-shrink-0">支付方式</span><span style="color: var(--coffee);">${payMethod}</span></div>
        ${payTime ? `<div class="flex justify-between gap-3" style="color: var(--brown-mid);"><span class="flex-shrink-0">支付时间</span><span style="color: var(--coffee);">${payTime}</span></div>` : ''}
        <div class="flex justify-between gap-3" style="color: var(--brown-mid);"><span class="flex-shrink-0">订单留言</span><span class="text-right" style="color: var(--coffee);">${note}</span></div>
      </div>`;
    openModal('orderDetailModal');
  } catch (e) {
    showToast(e.message || '加载详情失败', 'error');
  }
}
```

### 步骤 4：编译 + 重启 + 端到端验证

1. `mvn compile`（按 pom.xml L71-79 资源配置，`src/main/java/**/*.html` 会同步到 `target/classes/com/qiukai/view/`）。
2. 后台重启 Spring Boot（`mvn spring-boot:run`）。
3. 用测试用户 `13888888888 / test1234` 登录：
   - `GET /api/orders`（带 Cookie `kazi_token`）→ 校验每个订单 JSON 含 `merchantLogo` 字段且非空。
   - `GET /api/orders/{id}` → 校验 `order.merchantLogo` 非空、`items` 数组完整。
4. 浏览器打开 `profile.html` → 我的订单：
   - 列表每项左侧图片为对应商家 logo（如 `logo_burger`），不再是 `seed/order{id}`。
   - 点击"查看详情" → 弹出模态框：头部商家 logo + 名称 + 状态；菜品明细；金额明细；订单信息。ESC/点遮罩/关闭按钮均可关闭。
5. 回归：管理员后台 `admin.html` 订单列表仍正常（双 JOIN 不影响 username 显示）。

## 假设与决策

- **Logo 数据来源**：JOIN merchant 表（用户已确认），无需 DB 迁移、无需新增接口。
- **订单详情呈现**：新建模态框（用户已确认），取代当前 toast。
- **列表图片尺寸**：保持原 `w-16 h-16 rounded-2xl object-cover flex-shrink-0` 不变，仅换数据源 + 加 onerror 兜底，确保尺寸/比例/布局与原设计一致。
- **统一图片加载机制**：主 `src` + `onerror` 兜底到 picsum，沿用收藏夹模块既有模式，跨设备/屏幕一致。
- **LEFT JOIN**：保证 user/merchant 缺失时订单仍可查，logo 为 null 走前端兜底。
- **`allColumns`（单表）保留**：`selectByOrderNo`、`selectByMerchantId` 等不需富字段的查询继续用它，避免无谓 JOIN。
- **XSS 防护**：模态框 `innerHTML` 模板中所有外部字符串均经 `escapeHtml`；`textContent` 场景无需转义。
- **不改 Service/Controller/VO**：`getOrderDetail` 已用 `selectById`，改 JOIN 后 logo 自动随 order 返回。

## 验证清单

- [ ] `OrderMapper.xml` resultMap 含 `merchantLogo` 映射
- [ ] `allColumnsRich` 含 `m.logo AS merchant_logo`
- [ ] `selectById`/`selectAll`/`selectByUserId` 均双 JOIN（user + merchant）
- [ ] `renderOrders()` 列表 img 用 `o.merchantLogo` + onerror 兜底，class 不变
- [ ] 新增 `orderDetailModal` markup
- [ ] `viewOrderDetail()` 正确解构 `detail.order`/`detail.items`，渲染模态框
- [ ] `mvn compile` 通过
- [ ] 接口 `/api/orders` 与 `/api/orders/{id}` 返回 `merchantLogo`
- [ ] 页面列表 + 详情模态框均显示商家 logo
