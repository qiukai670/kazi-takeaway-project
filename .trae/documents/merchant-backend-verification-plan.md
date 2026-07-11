# 商家后台管理系统 — 编译验证与端到端测试计划

## Summary

上一会话已完成商家后台管理系统的全部代码实现（MerchantService.java、MerchantController.java、merchant.html、api.js merchant对象）。本计划聚焦于**最后一步**：编译验证、重启服务器、端到端测试所有商家后台功能。

## Current State Analysis

### 已完成（经本次探索验证）

| 文件 | 状态 | 大小 | 说明 |
|------|------|------|------|
| `src/main/java/com/qiukai/service/MerchantService.java` | ✅ 完整 | 14KB | 店铺/菜品/订单/优惠全方法 + 归属权校验 |
| `src/main/java/com/qiukai/controller/MerchantController.java` | ✅ 完整 | 5.7KB | 25个REST端点 `/api/merchant/*` |
| `src/main/java/com/qiukai/view/merchant.html` | ✅ 完整 | 63KB | 4模块(store/menu/orders/promos) + 6关键函数 |
| `src/main/java/com/qiukai/view/api.js` | ✅ 完整 | 17KB | merchant对象25方法 + return导出 |
| `target/classes/.../MerchantService.class` | ✅ 已编译 | 15KB | 上次成功编译产物 |
| `target/classes/.../MerchantController.class` | ✅ 已编译 | 8KB | 上次成功编译产物 |
| `target/classes/.../merchant.html` | ✅ 已同步 | 63KB | 静态资源已复制 |

### 待解决

1. **服务器未运行** — `netstat` 显示本地 8080 端口无 LISTENING（仅有一条到远程:8080 的出站连接）
2. **后台 Maven 任务失败** — job `job-b623f4454e5b4917b6f8d15f8c594d06` 因沙箱限制写入 `D:\Maven\...\resolver-status.properties` 失败，错误：`TRAE Sandbox Error: hit restricted`
3. **编译产物可能过期** — 虽然 .class 文件存在，但需确认对应最新源码
4. **端到端测试未执行** — 25个端点 + 前端页面均未测试

### 关键约束（来自上次会话经验）

- `mvn` 命令必须用 `dangerouslyDisableSandbox: true`（需写入 Maven 仓库 + 绑定网络端口）
- Windows curl JSON 转义问题 → 必须用 `@file.json` 方式发送请求体
- 服务器后台启动后通过日志确认 "Started KaziTakeawayProjectApplication"

## Proposed Changes

### 第1步：清理旧进程

```powershell
# 查找并终止残留的 Java 进程（避免端口冲突）
tasklist | findstr java
# 如有残留，按需 taskkill /PID <pid> /F
```

**Why**: 确保端口 8080 可用，避免新旧进程冲突。

### 第2步：重新编译（确保源码与class一致）

```powershell
mvn clean compile -DskipTests
```

**参数**: `dangerouslyDisableSandbox: true`（需写入 `D:\Maven\` 仓库）

**Why**: 虽然现有 .class 文件可能已是最新，但 `clean` 可确保不存在过期产物，验证源码无编译错误。

**验证标准**: 日志出现 `BUILD SUCCESS`，`target/classes/com/qiukai/service/MerchantService.class` 和 `target/classes/com/qiukai/controller/MerchantController.class` 存在。

### 第3步：启动 Spring Boot 服务器

```powershell
mvn spring-boot:run
```

**参数**: `dangerouslyDisableSandbox: true` + `run_in_background: true`

**Why**: 服务器需绑定 8080 端口 + 访问 MySQL + 加载 Maven 依赖，全部需沙箱外权限。

**验证标准**: 后台日志出现 `Started KaziTakeawayProjectApplication in X seconds`，无异常堆栈。

### 第4步：端到端 API 测试

使用 Windows curl + `@file.json` 方式（避免 JSON 转义问题）。所有测试脚本放在临时目录。

#### 4.1 商家登录获取 Token

```powershell
# 创建登录JSON
'{\"account\":\"<商家手机号或用户名>\",\"password\":\"<密码>\",\"rememberMe\":true}' | Out-File -Encoding utf8 login.json
curl -s -c cookies.txt -b cookies.txt -X POST http://localhost:8080/api/user/merchant/login -H "Content-Type: application/json" -d "@login.json"
```

**验证**: 返回 `{"code":200,...}`，`cookies.txt` 包含 `kazi_token`。

> 注：测试账号需使用数据库已存在的商家账号。如不存在，先用商家注册接口创建：`POST /api/user/merchant/register`（phone + username + password + merchantName + registrantName）。

#### 4.2 店铺信息模块

```powershell
# 获取店铺信息
curl -s -b cookies.txt http://localhost:8080/api/merchant/info
# 验证: code=200, 返回Merchant对象, 包含name/logo/category等

# 更新店铺信息（尝试修改isRecommended和badge，应被忽略）
'{"name":"测试店铺","category":"快餐","deliveryTime":30}' | Out-File -Encoding utf8 update.json
curl -s -b cookies.txt -X PUT http://localhost:8080/api/merchant/info -H "Content-Type: application/json" -d "@update.json"
# 验证: code=200, 店铺名更新成功; isRecommended和badge字段未被修改
```

#### 4.3 菜单管理模块

```powershell
# 菜品列表
curl -s -b cookies.txt http://localhost:8080/api/merchant/dishes
# 验证: 返回该商家所有菜品

# 切换售罄状态（取一个菜品ID）
curl -s -b cookies.txt -X PUT http://localhost:8080/api/merchant/dishes/<dishId>/sold-out
# 验证: code=200, "售罄状态已切换"
```

#### 4.4 订单管理模块

```powershell
# 订单列表（按状态筛选）
curl -s -b cookies.txt "http://localhost:8080/api/merchant/orders?status=PENDING_CONFIRM"
# 验证: 返回该商家待确认订单

# 接单（需有PENDING_CONFIRM状态订单）
curl -s -b cookies.txt -X PUT http://localhost:8080/api/merchant/orders/<orderId>/confirm
# 验证: code=200, "已接单", 订单状态变为CONFIRMED

# 派送
curl -s -b cookies.txt -X PUT http://localhost:8080/api/merchant/orders/<orderId>/dispatch
# 验证: code=200, "已开始派送", 订单状态变为DELIVERING
```

#### 4.5 优惠管理模块

```powershell
# 优惠活动列表
curl -s -b cookies.txt http://localhost:8080/api/merchant/promos

# 新增满减规则
'{"threshold":30.00,"discount":5.00,"status":0}' | Out-File -Encoding utf8 rule.json
curl -s -b cookies.txt -X POST http://localhost:8080/api/merchant/promo-rules -H "Content-Type: application/json" -d "@rule.json"
# 验证: code=200, "满减规则添加成功"

# 满减规则列表
curl -s -b cookies.txt http://localhost:8080/api/merchant/promo-rules
# 验证: 返回刚创建的规则

# 切换规则状态
curl -s -b cookies.txt -X PUT http://localhost:8080/api/merchant/promo-rules/<ruleId>/status
# 验证: code=200
```

#### 4.6 权限校验测试

```powershell
# 用管理员cookie访问商家接口 → 应返回403或业务错误
# 用商家A的cookie操作商家B的菜品 → 应返回"无权操作"
curl -s -b cookies.txt -X PUT http://localhost:8080/api/merchant/dishes/<其他商家菜品ID>/sold-out
# 验证: 返回403 "无权操作此菜品"
```

### 第5步：前端页面验证

```powershell
# 验证merchant.html可访问
curl -s -o NUL -w "%{http_code}" http://localhost:8080/merchant.html
# 验证: 返回200
```

**浏览器手动验证**（无法自动化，需用户确认）：
1. 访问 `http://localhost:8080/merchant.html` → 自动跳转登录页（未登录时）
2. 商家登录 → 跳转到 merchant.html
3. 4个模块切换正常，数据加载成功
4. 店铺信息编辑表单无"好评商家"和"徽章"字段
5. 菜品售罄切换 → 访问 shop.html 验证灰色+禁用按钮+"售罄"标签
6. 订单接单/派送按钮可点击
7. 优惠活动/满减规则 CRUD 正常

### 第6步：售罄显示联动验证

```powershell
# 1. 商家后台将某菜品设为售罄
curl -s -b cookies.txt -X PUT http://localhost:8080/api/merchant/dishes/<dishId>/sold-out

# 2. 用户侧获取菜品列表，验证soldOut=1
curl -s http://localhost:8080/api/dishes?merchantId=<merchantId>
# 验证: 对应菜品 soldOut=1

# 3. 浏览器访问 shop.html?merchantId=<merchantId> 验证：
#    - 菜品卡片灰色样式
#    - "加入购物车"按钮被禁用
#    - 显示"售罄"标签
```

## Assumptions & Decisions

1. **测试账号**：使用数据库已存在的商家账号；如无，通过注册接口创建。需在执行时确认账号凭据。
2. **沙箱禁用**：所有 `mvn` 和 `curl`（如需网络）命令使用 `dangerouslyDisableSandbox: true`，因 Maven 需写入本地仓库、服务器需绑定端口。
3. **JSON 发送方式**：Windows curl 不支持内联 JSON 转义，统一用 `@file.json` 方式。
4. **编译策略**：使用 `mvn clean compile -DskipTests` 确保产物干净；跳过测试加速编译。
5. **服务器运行模式**：后台运行 `mvn spring-boot:run`，通过读取日志确认启动成功。
6. **不改代码**：本计划仅验证已实现代码，不修改任何源文件（除非测试发现Bug需修复）。

## Verification Steps

执行完成后，以下全部为 ✅ 则任务完成：

- [ ] `mvn clean compile` 输出 `BUILD SUCCESS`
- [ ] 服务器日志显示 `Started KaziTakeawayProjectApplication`
- [ ] `netstat` 显示本地 8080 端口 LISTENING
- [ ] 商家登录返回 `code:200` + 有效 token
- [ ] `GET /api/merchant/info` 返回店铺信息
- [ ] `PUT /api/merchant/info` 更新成功，isRecommended/badge 未被修改
- [ ] `GET /api/merchant/dishes` 返回菜品列表
- [ ] `PUT /api/merchant/dishes/{id}/sold-out` 售罄切换成功
- [ ] `GET /api/merchant/orders` 返回订单列表
- [ ] `PUT /api/merchant/orders/{id}/confirm` 接单成功（PENDING_CONFIRM→CONFIRMED）
- [ ] `PUT /api/merchant/orders/{id}/dispatch` 派送成功（CONFIRMED→DELIVERING）
- [ ] `POST /api/merchant/promo-rules` 满减规则创建成功
- [ ] 跨商家操作返回 403 "无权操作"
- [ ] `merchant.html` HTTP 200 可访问
- [ ] shop.html 售罄菜品显示灰色+禁用按钮+"售罄"标签

## 风险与回退

- **编译失败**：检查源码语法错误，修复后重新编译
- **服务器启动失败**：检查端口占用、数据库连接、Bean注入问题
- **API 测试失败**：检查 UserContext/拦截器配置、SQL映射、事务回滚
- **权限校验失效**：检查 `getMyMerchantId()` 逻辑、`UserContext.isMerchant()` 实现
