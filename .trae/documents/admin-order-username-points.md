# 管理端订单显示用户名 + 订单完成自动加积分

## Context（背景）

当前管理后台订单列表的"用户"列只显示 `用户#${userId}`，管理员无法直接识别下单用户真实身份。同时系统虽已有 `user.points` 字段，但全工程无任何积分累加逻辑——用户完成订单后积分始终不变，会员积分体系名存实亡。

本次改动实现两个独立功能：
1. **管理端订单列表显示下单用户真实用户名**（替代 `用户#ID`）。
2. **订单状态流转到「已完成」时自动按实付总额累加积分**（1元=1积分，向下取整），让积分体系真正运转。

两功能互不耦合，可并行实施。无需 DB 迁移、无需新表、无需新配置。

## 用户已确认的决策

| 决策点 | 选择 |
|---|---|
| 积分基数 | `totalAmount`（实付总额 = subtotal − discount + deliveryFee，含配送费、已扣折扣） |
| 积分比例与取整 | 1元 = 1积分，向下取整（如 ¥23.70 → 23） |
| 订单详情 | 仅列表显示用户名，不新建详情模态框 |

## 功能 1：管理端订单列表显示真实用户名

### 1.1 Order 实体新增字段
文件：`src/main/java/com/qiukai/entity/Order.java`
- 在 L16 `userId` 之后新增 `private String username;`
- 纯 JOIN 投影字段（类似已有的 `merchantName`），不入库。`insert` 语句（OrderMapper.xml L29-34）是显式列名不含 username，不会误写库。Lombok `@Data` 自动生成 getter/setter，Jackson 序列化为 JSON `username`，前端 `o.username` 可读。

### 1.2 OrderMapper.xml 改造
文件：`src/main/resources/mapper/OrderMapper.xml`

**(a) resultMap**（L5-22）末尾、L21 `updateTime` 之后追加：
```xml
<result property="username" column="username"/>
```
不查询 username 的语句留 null，安全。

**(b) 新增 SQL 片段**（放在 L27 `allColumns` 之后）：
```xml
<sql id="allColumnsWithUser">
    o.id, o.order_no, o.user_id, o.merchant_id, o.merchant_name, o.subtotal, o.discount, o.delivery_fee,
    o.total_amount, o.status, o.note, o.pay_method, o.address_snapshot, o.pay_time, o.create_time, o.update_time,
    u.username AS username
</sql>
```
必须加 `o.` 前缀——`user` 表也有 `id`/`create_time`/`update_time`，JOIN 后列名歧义会报错。保留原 `allColumns` 供其他三条单表查询使用，互不污染。

**(c) 改 `selectById`（L36-38）和 `selectAll`（L58-64）** 为 LEFT JOIN：
```xml
<select id="selectById" resultMap="orderResultMap">
    SELECT <include refid="allColumnsWithUser"/>
    FROM orders o LEFT JOIN user u ON o.user_id = u.id
    WHERE o.id = #{id}
</select>

<select id="selectAll" resultMap="orderResultMap">
    SELECT <include refid="allColumnsWithUser"/>
    FROM orders o LEFT JOIN user u ON o.user_id = u.id
    <where>
        <if test="status != null and status != ''">AND o.status = #{status}</if>
    </where>
    ORDER BY o.create_time DESC
</select>
```
- `selectById` 被 adminGetOrderDetail 与用户侧 getOrderAndCheck 共用，JOIN 后用户侧也顺带拿到 username（未使用，无害）。LEFT JOIN 保证用户被删时不丢订单行。
- `selectByOrderNo`/`selectByUserId`/`selectByMerchantId` 保持原 `allColumns` 不动（用户/商家端列表不需要用户名）。
- `OrderMapper.java` 方法签名不变，`OrderService.listAllOrders`/`adminGetOrderDetail` 无需改动。

### 1.3 admin.html 前端
文件：`src/main/java/com/qiukai/view/admin.html`

- **L1595**（`tbody.innerHTML` 拼接，HTML 上下文，必须转义）：
  - 旧：`用户#${o.userId}`
  - 新：`${escapeHtml(o.username || ('用户#' + o.userId))}`
- **三个确认弹窗 L1620 / L1641 / L1662**（`showConfirm` 的 msg 经 `textContent` 赋值，L775，非 innerHTML，无需转义）：
  - 旧：`订单 ${o.orderNo}（用户#${o.userId}）...`
  - 新：`订单 ${o.orderNo}（${o.username || ('用户#' + o.userId)}）...`
- 表头 L381「用户」列已存在，无需改。
- 用户名 null（如用户被删）回退显示 `用户#${userId}`。

## 功能 2：订单转 COMPLETED 时自动加积分

### 2.1 UserMapper.java 新增方法
文件：`src/main/java/com/qiukai/mapper/UserMapper.java`
- L32 `updatePassword` 之后新增：
```java
/** 累加积分（原子自增） */
int addPoints(@Param("id") Long id, @Param("delta") int delta);
```

### 2.2 UserMapper.xml 新增 update
文件：`src/main/resources/mapper/UserMapper.xml`
- L61 `updatePassword` 之后新增：
```xml
<update id="addPoints">
    UPDATE user SET points = points + #{delta} WHERE id = #{id}
</update>
```
- DB 级原子自增（`points = points + ?`），无 read-modify-write，无并发竞态。`user.points` 列 `NOT NULL DEFAULT 0`（schema.sql L27），自增安全。

### 2.3 OrderService 改 confirmReceipt
文件：`src/main/java/com/qiukai/service/OrderService.java`

- 类顶部新增常量：`private static final int POINTS_PER_YUAN = 1;`（自文档化「1元=1积分」，未来调比例只改一处）
- L39 `addressMapper` 之后注入：`@Autowired private UserMapper userMapper;`（已 `import com.qiukai.mapper.*`，无需新增 import；`BigDecimal` 已 import L14）
- `confirmReceipt`（L194-202）在 `orderMapper.updateStatus(orderId, OrderStatus.COMPLETED);` 之后追加：
```java
BigDecimal totalAmount = order.getTotalAmount();
if (totalAmount != null) {
    int points = totalAmount.intValue() * POINTS_PER_YUAN; // 正数 intValue() 即向下取整
    if (points > 0) {
        userMapper.addPoints(userId, points);
    }
}
```
- 用方法内已有的 `userId`（L196 `requireLogin()` 返回值，且 `getOrderAndCheck` 已校验 `order.userId == userId`）作为积分接收人，正确。
- `BigDecimal.intValue()` 对正数等价于 `floor`（如 23.70 → 23，52.00 → 52）。

### 事务与幂等性
- `confirmReceipt` 已 `@Transactional`（L194），`addPoints` 与 `updateStatus` 同事务；`addPoints` 抛异常则整事务回滚（含状态更新），数据一致。
- 幂等由状态守卫保证：仅 `DELIVERING` 可确认收货，成功后变 `COMPLETED`，重复调用直接抛「当前订单状态不可确认收货」，不会二次加分。无需额外幂等键。
- 历史 COMPLETED 订单不补发积分（向前生效，符合预期）。

## 关键文件清单
- `src/main/java/com/qiukai/entity/Order.java` — 功能1：新增 username 字段
- `src/main/resources/mapper/OrderMapper.xml` — 功能1：resultMap、新片段、selectById/selectAll 改 JOIN
- `src/main/java/com/qiukai/view/admin.html` — 功能1：renderOrders L1595 + 三个确认弹窗 L1620/L1641/L1662
- `src/main/java/com/qiukai/mapper/UserMapper.java` — 功能2：addPoints 接口
- `src/main/resources/mapper/UserMapper.xml` — 功能2：addPoints SQL
- `src/main/java/com/qiukai/service/OrderService.java` — 功能2：注入 UserMapper、常量、confirmReceipt 加积分

## 验证方案

**前置**：pom.xml L71-79 把 `src/main/java/**/*.html` 配为资源，改完执行 `mvn compile` 自动同步视图到 `target/classes/com/qiukai/view/`。先停掉旧后台进程（端口 8080）再 `mvn spring-boot:run` 重启。

**功能 1（用户名）**：
1. 管理员登录（admin / admin123），进入订单管理列表。
2. 预期「用户」列显示真实 `username`（如 `qiukai_2024`），而非 `用户#2`。
3. 点击「审核订单/确认接单/开始派送」三个弹窗，提示文案显示用户名。
4. 边界：LEFT JOIN 用户被删时回退 `用户#${userId}`。

**功能 2（积分）**：
1. 普通用户登录（13888888888 / test1234），记录当前 `points`（profile 页或 `/api/user/info`）。
2. 下单 → 支付（→PAID）→ 管理员审核（→PENDING_CONFIRM）→ 确认接单（→CONFIRMED）→ 开始派送（→DELIVERING）→ 用户确认收货（→COMPLETED）。
3. 预期 `points` 增加 `floor(totalAmount)`。如实付 ¥52.00 → +52；¥23.70 → +23。
4. 幂等性：再次调用确认收货接口，应返回「当前订单状态不可确认收货」且积分不再增加。
5. 零/负金额守卫：`points <= 0` 跳过 addPoints（理论上正常订单不会触发）。
