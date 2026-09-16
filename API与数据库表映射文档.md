# API 与数据库表映射文档

> 超市管理系统 (SupermarketManage) — 所有 API 端点与数据库表的完整映射关系  
> 按**表**分类，列出每个 API 对该表的读/写操作  
> 生成日期: 2026-06-26

---

## 目录

1. [user_info — 用户账户表](#1-user_info--用户账户表)
2. [sms_code — 短信验证码表](#2-sms_code--短信验证码表)
3. [admin_register_code — 管理员注册校验码表](#3-admin_register_code--管理员注册校验码表)
4. [user_feedback — 用户反馈表](#4-user_feedback--用户反馈表)
5. [product_category — 商品分类表](#5-product_category--商品分类表)
6. [product_base — 商品基础档案表](#6-product_base--商品基础档案表)
7. [purchase_record — 进货入库记录表](#7-purchase_record--进货入库记录表)
8. [product_batch_detail — 商品批次明细表（库存核心）](#8-product_batch_detail--商品批次明细表库存核心)
9. [shelf_info — 货架信息表](#9-shelf_info--货架信息表)
10. [shelf_capacity_tracking — 货架每层容量追踪表](#10-shelf_capacity_tracking--货架每层容量追踪表)
11. [sale_order — 销售订单主表](#11-sale_order--销售订单主表)
12. [sale_order_item — 销售订单详情表](#12-sale_order_item--销售订单详情表)
13. [sale_refund — 销售退货记录表](#13-sale_refund--销售退货记录表)
14. [loss_record — 商品损耗记录表](#14-loss_record--商品损耗记录表)
15. [promotion_image — 宣传图片表](#15-promotion_image--宣传图片表)
16. [无表操作 — 文件上传/静态资源](#16-无表操作--文件上传静态资源)

---

## 表关系概览

```
user_info ──┬── sms_code
            ├── user_feedback
            ├── sale_order ─── sale_order_item ──┬── product_batch_detail ──┬── purchase_record
            │                    │                │                          │
            │                    │                └── sale_refund ───────────┴── loss_record
            │                    │
            └── sale_refund      └── product_base ─── product_category
                                     │
product_batch_detail ────────────────┘
       │
       └── shelf_info ─── shelf_capacity_tracking

promotion_image (独立表)
```

---

## 1. user_info — 用户账户表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `POST /api/auth/login` | POST | 按账号（手机号或用户名）查询用户，验证密码 |
| 🔍读 | `POST /api/auth/register-user` | POST | 检查手机号/用户名是否已存在 |
| 🔍读 | `POST /api/auth/register-admin` | POST | 检查手机号/用户名是否已存在 |
| 🔍读 | `POST /api/auth/forget-password` | POST | 按手机号查询用户是否存在 |
| 🔍读 | `POST /api/auth/change-password` | POST | 查询当前用户信息，验证旧密码 |
| 🔍读 | `POST /api/auth/cancel-account` | POST | 查询当前用户，查询手机号最大后缀用于注销 |
| 🔍读 | `GET /api/order/page` | GET | 查询订单时联表查询 username，填充订单创建人名称 |
| 🔍读 | `GET /api/order/{id}` | GET | 查询订单详情时联表查询 username |
| 🔍读 | `GET /api/refund/page` | GET | 查询退货记录时联表查询 operator_id → username |
| ✏️写 | `POST /api/auth/register-user` | POST | INSERT 新用户记录（user_type=1） |
| ✏️写 | `POST /api/auth/register-admin` | POST | INSERT 新管理员记录（user_type=0） |
| ✏️写 | `POST /api/auth/forget-password` | POST | UPDATE 密码（BCrypt加密） |
| ✏️写 | `POST /api/auth/change-password` | POST | UPDATE 密码 |
| ✏️写 | `POST /api/auth/cancel-account` | POST | UPDATE 手机号追加后缀 `-N` 实现注销复用 |

---

## 2. sms_code — 短信验证码表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `POST /api/auth/view-sms-code` | POST | 查询指定手机号最新未使用且未过期的验证码（开发调试用） |
| 🔍读 | `POST /api/auth/register-user` | POST | 验证短信验证码是否有效 |
| 🔍读 | `POST /api/auth/register-admin` | POST | 验证短信验证码是否有效 |
| 🔍读 | `POST /api/auth/forget-password` | POST | 验证短信验证码是否有效 |
| ✏️写 | `POST /api/auth/send-sms` | POST | INSERT 新验证码（6位随机数，5分钟有效）；先将该手机号旧未使用验证码标记为已使用（UPDATE is_used=1） |
| ✏️写 | `POST /api/auth/register-user` | POST | UPDATE is_used=1（标记验证码已使用） |
| ✏️写 | `POST /api/auth/register-admin` | POST | UPDATE is_used=1（标记验证码已使用） |
| ✏️写 | `POST /api/auth/forget-password` | POST | UPDATE is_used=1（标记验证码已使用） |

> **Mapper 自定义 SQL：**
> - `SmsCodeMapper.invalidateOldCodes(phone)` — `UPDATE sms_code SET is_used=1 WHERE phone=? AND is_used=0 AND expire_time > NOW()`
> - `SmsCodeMapper.selectLatestValidByPhone(phone)` — `SELECT * FROM sms_code WHERE phone=? AND is_used=0 AND expire_time > NOW() ORDER BY create_time DESC LIMIT 1`

---

## 3. admin_register_code — 管理员注册校验码表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `POST /api/auth/register-admin` | POST | 校验管理员注册码是否有效（is_valid=1 且 used_times < total_usable_times） |
| ✏️写 | `POST /api/auth/register-admin` | POST | UPDATE used_times+1；若用完则 UPDATE is_valid=0 |

> **Mapper 自定义 SQL：**
> - `AdminRegisterCodeMapper.selectValidByCode(code)` — `SELECT * FROM admin_register_code WHERE code=? AND is_valid=1 AND used_times < total_usable_times`

---

## 4. user_feedback — 用户反馈表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/feedback/page` | GET | 分页查询反馈列表（支持按 userId、status 筛选） |
| 🔍读 | `POST /api/feedback/reply` | POST | 先查询反馈是否存在（getById），再执行回复更新 |
| ✏️写 | `POST /api/feedback` | POST | INSERT 新反馈记录（status=0 未处理） |
| ✏️写 | `POST /api/feedback/reply` | POST | UPDATE reply 内容和 status=1（已回复） |

---

## 5. product_category — 商品分类表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/product-category/page` | GET | 分页查询分类列表（支持 keyword 模糊搜索） |
| 🔍读 | `GET /api/product-category/{id}` | GET | 按 ID 查询单个分类 |
| 🔍读 | `POST /api/product-category` | POST | 新增前检查分类名是否已存在（唯一约束） |
| 🔍读 | `PUT /api/product-category` | PUT | 更新前查询分类是否存在 |
| 🔍读 | `PUT /api/product-category/toggle/{id}` | PUT | 切换启用前查询分类是否存在 |
| 🔍读 | `GET /api/product-base/page` | GET | 查询商品时联表查 category_name 填充到返回结果 |
| 🔍读 | `POST /api/product-base` | POST | 新增商品时校验 category_id 是否存在 |
| 🔍读 | `PUT /api/product-base` | PUT | 更新商品时校验 category_id 是否存在 |
| ✏️写 | `POST /api/product-category` | POST | INSERT 新分类 |
| ✏️写 | `PUT /api/product-category` | PUT | UPDATE 分类名称、排序等 |
| ✏️写 | `PUT /api/product-category/toggle/{id}` | PUT | UPDATE is_enabled（0↔1 切换） |

---

## 6. product_base — 商品基础档案表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/product-base/page` | GET | 分页查询商品列表（支持 keyword、categoryId 筛选）；联表查 category_name |
| 🔍读 | `GET /api/product-base/{id}` | GET | 按 ID 查询单个商品 |
| 🔍读 | `GET /api/product-base/list` | GET | 查询所有已启用的商品（is_enabled=1），可选按 categoryId 过滤；收银台选商品时使用 |
| 🔍读 | `GET /api/product-base/stock-detail/{baseProductId}` | GET | 按商品 ID 查询商品基本信息 |
| 🔍读 | `GET /api/purchase/page` | GET | 分页查询进货记录时，逐条查 product_base 获取商品名称填充 |
| 🔍读 | `POST /api/purchase` | POST | 新增进货时校验商品是否存在且已启用 |
| 🔍读 | `GET /api/shelf-on/batches` | GET | 查询待上架批次时查 product_base 获取商品名称 |
| 🔍读 | `GET /api/shelf-on/for-sale` | GET | 收银台查询在售商品时查 product_base 获取商品信息 |
| 🔍读 | `GET /api/shelf-on/list-valid` | GET | 查询有效库存时查 product_base 获取商品信息 |
| 🔍读 | `GET /api/order/{id}` | GET | 查询订单详情时通过 order_item → product_base 获取商品名称和图片 |
| 🔍读 | `GET /api/refund/page` | GET | 查询退货记录时通过 batch_detail → product_base 获取商品名称 |
| 🔍读 | `GET /api/loss/page` | GET | 查询损耗记录时逐条查 product_base 获取商品名称 |
| 🔍读 | `GET /api/dashboard/stats` | GET | 遍历所有已启用商品，统计每种商品的库存状态 |
| 🔍读 | `GET /api/profit/monthly` | GET | 月度利润统计中不直接读此表，但通过 batch_detail 间接关联 |
| ✏️写 | `POST /api/product-base` | POST | INSERT 新商品基础档案 |
| ✏️写 | `PUT /api/product-base` | PUT | UPDATE 商品信息（名称、分类、图片等） |
| ✏️写 | `PUT /api/product-base/toggle/{id}` | PUT | UPDATE is_enabled（0↔1 切换） |

---

## 7. purchase_record — 进货入库记录表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/purchase/page` | GET | 分页查询进货记录（支持按商品、日期范围筛选） |
| 🔍读 | `GET /api/profit/monthly` | GET | 月度利润统计：`SUM(purchase_total_amount)` 按年月汇总进货总成本 |
| ✏️写 | `POST /api/purchase` | POST | INSERT 一条进货记录（含数量、单价、总金额）；同时按 purchase_quantity 生成等量的 product_batch_detail 行 |

> **Mapper 自定义 SQL：**
> - `PurchaseRecordMapper.selectLatestPurchasePrice(baseProductId)` — 查询某商品最近一次进货单价
> - `PurchaseRecordMapper.selectMonthlyPurchaseTotal(year, month)` — 按月汇总进货总金额

---

## 8. product_batch_detail — 商品批次明细表（库存核心）

> 这是系统的**库存核心表**，每件商品独立一条记录，全生命周期跟踪。  
> **状态流转：** `0(在库未上架)` → `1(已上架在售)` → `2(已售出)` → `3(已损耗)`  
> **特殊状态：** `5(待支付锁定/购物车预留)`  
> **退货路径：** `2→1(退货回库)` / `2→3(退货转损耗)`

### 查询类操作

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/product-base/stock-detail/{baseProductId}` | GET | 按商品 ID 查询所有批次明细，展示每批次的库存状态 |
| 🔍读 | `GET /api/shelf-on/batches` | GET | 查询指定状态批次：`selectByProductAndStatus`、`selectByProductAndStatuses`、`selectByBaseProductId`、`countGroupByStatus` |
| 🔍读 | `GET /api/shelf-on/for-sale` | GET | 查询所有 status=1（已上架在售）的批次，供收银台展示 |
| 🔍读 | `GET /api/shelf-on/list-valid` | GET | 查询所有 status=1 的批次（旧版有效库存查询） |
| 🔍读 | `POST /api/order/create` | POST | 查询 status=1 的批次用于锁定；检查库存是否充足 |
| 🔍读 | `POST /api/order/pay` | POST | 查询 status=5（已锁定）的批次，核实归属并标记售出 |
| 🔍读 | `POST /api/order/cancel` | POST | 查询 status=5（已锁定）的批次，解锁回 status=1 |
| 🔍读 | `GET /api/order/{id}` | GET | 通过 order_item → batch_detail 查询已售批次详情和定价 |
| 🔍读 | `POST /api/refund` | POST | 查询订单关联的已售批次（status=2），用于退货校验 |
| 🔍读 | `POST /api/refund/approve` | POST | 查询退货对应的批次，执行回库或转损耗操作 |
| 🔍读 | `GET /api/refund/page` | GET | 通过 batch_detail_id 联查批次信息填充退货记录 |
| 🔍读 | `POST /api/loss` | POST | 查询待损耗批次（status IN 0,1,2），校验状态 |
| 🔍读 | `GET /api/loss/page` | GET | 通过 batch_detail_id 联查批次信息填充损耗记录 |
| 🔍读 | `GET /api/dashboard/stats` | GET | `countByProductAndStatus` 统计各状态数量，`selectByProductAndStatus` 做库存预警；`selectTotalRemaining`（容量追踪）做容量预警 |
| 🔍读 | `GET /api/profit/monthly` | GET | `selectMonthlySoldCostTotal` 汇总已售成本；`selectMonthlyLossCostTotal` 汇总损耗成本 |

### 写入类操作

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| ✏️写 | `POST /api/purchase` | POST | INSERT N 条批次明细（N = purchase_quantity），status=0 |
| ✏️写 | `POST /api/shelf-on/batch-on` | POST | UPDATE：`shelfOn` — 设置 shelf_id/shelf_layer/shelf_display_name/sale_price/discount_price，status 0→1 |
| ✏️写 | `POST /api/shelf-on/batch-off` | POST | UPDATE：`shelfOff` — 清空 shelf_id/shelf_layer/shelf_display_name，status 1→0 |
| ✏️写 | `PUT /api/shelf-on/price` | PUT | UPDATE：`updatePrice` — 修改 sale_price、discount_price |
| ✏️写 | `POST /api/shelf-on/reserve/{batchId}` | POST | UPDATE：`reserveForCart` — 原子操作 status 1→5（加入购物车锁定） |
| ✏️写 | `POST /api/shelf-on/release/{batchId}` | POST | UPDATE：`releaseFromCart` — 原子操作 status 5→1（从购物车释放） |
| ✏️写 | `POST /api/order/create` | POST | UPDATE：`bindToOrderItem` — 关联 order_item_id，标记为已锁定 |
| ✏️写 | `POST /api/order/pay` | POST | UPDATE：`markAsSold` — status→2，关联 belong_order_item_id |
| ✏️写 | `POST /api/order/cancel` | POST | UPDATE：`markAsUnlocked` — status 5→1，清空 belong_order_item_id |
| ✏️写 | `POST /api/refund/approve` | POST | UPDATE：`markAsReturnedToStock`（status 2→1，退货回库）和/或 `markAsLoss`（status 2→3，退货转损耗） |
| ✏️写 | `POST /api/loss` | POST | UPDATE：`markAsLoss` — status→3 |

> **Mapper 自定义 SQL（23个方法）：**
> - `selectByProductAndStatus` / `selectByProductAndStatuses` / `selectByBaseProductId` — 按商品+状态查询
> - `selectEarliestAvailable` — 查询最早可用批次（FIFO）
> - `countByProductAndStatus` / `countGroupByStatus` — 数量统计
> - `selectByShelfAndStatus` / `selectByOrderItemId` / `selectByPurchaseRecordId` — 按货架/订单/进货单查询
> - `batchUpdateStatus` — 批量更新状态（动态 IN 子句）
> - `shelfOn` / `shelfOff` / `updatePrice` / `changeShelf` — 上架/下架/改价/换架
> - `markAsSold` / `markAsLocked` / `markAsUnlocked` / `bindToOrderItem` — 销售状态流转
> - `reserveForCart` / `releaseFromCart` — 购物车锁定/释放
> - `markAsLoss` / `markAsReturnedToStock` — 损耗/退货回库
> - `selectMonthlySoldCostTotal` / `selectMonthlyLossCostTotal` — 按月成本汇总

---

## 9. shelf_info — 货架信息表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/shelf/page` | GET | 分页查询货架列表（支持 shelfCode、shelfArea 筛选） |
| 🔍读 | `GET /api/shelf/{id}` | GET | 按 ID 查询单个货架 |
| 🔍读 | `GET /api/shelf/list` | GET | 查询所有已启用货架（is_enabled=1） |
| 🔍读 | `PUT /api/shelf` | PUT | 更新前查询货架是否存在；验证容量 |
| 🔍读 | `PUT /api/shelf/toggle/{id}` | PUT | 切换启用前查询是否存在 |
| 🔍读 | `GET /api/product-base/stock-detail/{baseProductId}` | GET | 通过批次详情中的 shelf_id 联查货架编号填充 |
| 🔍读 | `GET /api/shelf-on/batches` | GET | 查询待上架批次时查 shelf_info 获取货架名称和容量 |
| 🔍读 | `GET /api/shelf-on/shelf-layers/{shelfId}` | GET | 查询货架是否存在；获取各层容量占用 |
| 🔍读 | `POST /api/shelf-on/batch-on` | POST | 自动分配货架时查询货架容量和层数 |
| 🔍读 | `GET /api/shelf-on/for-sale` | GET | 查询所有已启用货架填充在售商品展示 |
| 🔍读 | `GET /api/order/{id}` | GET | 查询订单详情时通过 batch_detail → shelf_info 获取货架编号 |
| ✏️写 | `POST /api/shelf` | POST | INSERT 新货架；同时按层数生成 shelf_capacity_tracking 行 |
| ✏️写 | `PUT /api/shelf` | PUT | UPDATE 货架信息（编号、区域、层数、容量）；删除旧容量追踪行并重建 |
| ✏️写 | `PUT /api/shelf/toggle/{id}` | PUT | UPDATE is_enabled（0↔1 切换） |

---

## 10. shelf_capacity_tracking — 货架每层容量追踪表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/shelf/capacity/{id}` | GET | 查询某货架各层的容量使用详情（已用/最大/剩余） |
| 🔍读 | `GET /api/shelf-on/shelf-layers/{shelfId}` | GET | 查询某货架各层已用数量 |
| 🔍读 | `POST /api/shelf-on/batch-on` | POST | 自动分配时检查各层剩余容量是否充足 |
| 🔍读 | `POST /api/shelf-on/batch-off` | POST | 下架时查询对应层的容量追踪记录 |
| 🔍读 | `POST /api/loss` | POST | 损耗登记时查询对应层容量追踪，用于扣除已用数 |
| 🔍读 | `POST /api/order/pay` | POST | 支付时查询对应层容量追踪，用于扣除已用数 |
| 🔍读 | `POST /api/refund/approve` | POST | 退货回库时查询对应层容量追踪，用于增加已用数 |
| 🔍读 | `GET /api/dashboard/stats` | GET | `selectTotalRemaining()` 计算总剩余容量，做库存容量预警 |
| ✏️写 | `POST /api/shelf` | POST | INSERT 每层追踪记录（used_count=0, max_capacity=layer_capacity） |
| ✏️写 | `PUT /api/shelf` | PUT | DELETE 旧追踪行 → INSERT 新追踪行（层数或容量变更时） |
| ✏️写 | `POST /api/shelf-on/batch-on` | POST | UPDATE `incrementUsed(shelfId, layer, +delta)` — 增加已用数 |
| ✏️写 | `POST /api/shelf-on/batch-off` | POST | UPDATE `decrementUsed(shelfId, layer, delta)` — 减少已用数 |
| ✏️写 | `POST /api/order/pay` | POST | UPDATE `decrementUsed(shelfId, layer, 1)` — 商品售出后减少已用数 |
| ✏️写 | `POST /api/refund/approve` | POST | UPDATE `incrementUsed(shelfId, layer, backStockQty)` — 退货回库增加已用数 |
| ✏️写 | `POST /api/loss` | POST | UPDATE `decrementUsed(shelfId, layer, 1)` — 商品损耗后减少已用数 |

> **Mapper 自定义 SQL（7个方法）：**
> - `selectByShelfAndLayer` / `selectByShelfId` — 查询容量
> - `incrementUsed(shelfId, layer, delta)` — `UPDATE … SET used_count = used_count + #{delta}`
> - `decrementUsed(shelfId, layer, delta)` — `UPDATE … SET used_count = IF(used_count >= #{delta}, used_count - #{delta}, 0)`（防负值）
> - `updateMaxCapacity` — 更新层最大容量
> - `selectTotalRemaining` — 总剩余容量 `SUM(max_capacity - used_count)`
> - `selectShelfRemaining` — 某货架剩余容量

---

## 11. sale_order — 销售订单主表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/order/page` | GET | 分页查询订单列表（支持 payStatus、日期范围、userId 筛选）；联表 user_info 获取用户名 |
| 🔍读 | `GET /api/order/{id}` | GET | 按 ID 查询单个订单（同时调 getOrderDetail 做详情组装） |
| 🔍读 | `POST /api/order/pay` | POST | 查询订单确认存在且状态正确（payStatus=0） |
| 🔍读 | `POST /api/order/cancel` | POST | 查询订单确认存在且可取消 |
| 🔍读 | `PUT /api/order/pay-status` | PUT | 查询订单是否存在 |
| 🔍读 | `POST /api/refund` | POST | 校验订单是否存在 |
| 🔍读 | `POST /api/refund/approve` | POST | 查询订单用于更新退款状态（updateRefundStatus） |
| 🔍读 | `GET /api/dashboard/stats` | GET | `selectMonthlyPaidTotal` 汇总当月已支付订单金额 |
| 🔍读 | `GET /api/profit/monthly` | GET | `selectMonthlyPaidTotal(year, month)` — `SUM(pay_amount) WHERE pay_status IN (1,2,4)` |
| ✏️写 | `POST /api/order/create` | POST | INSERT 新订单（初始 payStatus=0） |
| ✏️写 | `POST /api/order/pay` | POST | UPDATE payStatus=1（已支付） |
| ✏️写 | `POST /api/order/cancel` | POST | UPDATE payStatus=3（已取消） |
| ✏️写 | `PUT /api/order/pay-status` | PUT | UPDATE payStatus（管理员手动修改，支持 0/1/2/3/4） |
| ✏️写 | `POST /api/refund/approve` | POST | 通过 `updateRefundStatus` 更新订单退款状态（1→4部分退款 或 1→2全部退款） |

> **Mapper 自定义 SQL：**
> - `SaleOrderMapper.selectMonthlyPaidTotal(year, month)` — `SELECT COALESCE(SUM(pay_amount), 0) FROM sale_order WHERE pay_status IN (1,2,4) AND YEAR/MONTH(create_time)`

---

## 12. sale_order_item — 销售订单详情表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/order/{id}` | GET | 查询订单的所有明细行（getOrderItems） |
| 🔍读 | `POST /api/order/pay` | POST | 查询订单明细，获取应支付的商品列表 |
| 🔍读 | `POST /api/order/cancel` | POST | 查询订单明细，获取需解锁的批次列表 |
| 🔍读 | `POST /api/refund` | POST | 校验退货请求中的 order_item_id 是否存在 |
| 🔍读 | `POST /api/refund/approve` | POST | 查询订单明细，更新 refund_quantity |
| 🔍读 | `GET /api/refund/page` | GET | 通过 order_item_id 联查明细信息 |
| ✏️写 | `POST /api/order/create` | POST | INSERT N 条订单明细（N = 购买种类数） |
| ✏️写 | `POST /api/order/pay` | POST | UPDATE batch_detail_id（关联批次明细，用于成本追溯） |
| ✏️写 | `POST /api/refund/approve` | POST | UPDATE refund_quantity += 本次退货数量 |
| ✏️写 | `POST /api/refund/approve` | POST | 通过 `updateRefundStatus` → 当所有 item 的 refund_quantity 达到 buy_quantity 时，订单变更为已退款/部分退款 |

---

## 13. sale_refund — 销售退货记录表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/refund/page` | GET | 分页查询退货记录（支持 orderId、handleStatus、日期范围筛选）；联表 user_info、product_batch_detail、product_base、sale_order_item 填充详情 |
| 🔍读 | `POST /api/refund` | POST | 提交退货前检查同一 order_item 是否已有待审核的退货申请（防重复） |
| 🔍读 | `POST /api/refund/approve` | POST | 查询退货记录确认存在且为待审核状态 |
| 🔍读 | `POST /api/refund/reject` | POST | 查询退货记录确认存在且为待审核状态 |
| 🔍读 | `GET /api/order/{id}` | GET | 查询订单详情时联查该订单的退货记录（pending refund counts, latest refund status） |
| 🔍读 | `GET /api/dashboard/stats` | GET | 汇总退款总金额用于利润计算 |
| 🔍读 | `GET /api/profit/monthly` | GET | `selectMonthlyRefundTotal` — `SUM(refund_total_amount) WHERE refund_handle_status=1`；`selectMonthlyReturnCostTotal` — JOIN batch_detail 计算退货成本；`selectMonthlyReturnLossCostTotal` — JOIN batch_detail 计算退货转损耗成本 |
| ✏️写 | `POST /api/refund` | POST | INSERT 新退货申请（refund_handle_status=0 待审核） |
| ✏️写 | `POST /api/refund/approve` | POST | UPDATE refund_handle_status=1（已同意），填充 back_stock_quantity、to_loss_quantity |
| ✏️写 | `POST /api/refund/reject` | POST | UPDATE refund_handle_status=2（已拒绝） |
| ✏️写 | `POST /api/refund/batch-approve` | POST | 批量调用 approveRefund 逻辑 |
| ✏️写 | `POST /api/refund/batch-reject` | POST | 批量调用 rejectRefund 逻辑 |

> **Mapper 自定义 SQL（4个方法）：**
> - `selectMonthlyRefundTotal(year, month)` — 月退款总额
> - `selectByOrderItemId(orderItemId)` — 按订单明细查退货记录
> - `selectMonthlyReturnCostTotal(year, month)` — **跨表 JOIN** `sale_refund sr JOIN product_batch_detail bd ON sr.batch_detail_id = bd.id` 按月汇总退货成本
> - `selectMonthlyReturnLossCostTotal(year, month)` — **跨表 JOIN**，退货转损耗成本

---

## 14. loss_record — 商品损耗记录表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/loss/page` | GET | 分页查询损耗记录（支持 baseProductId、日期范围筛选）；先查 batch_detail 解析出对应 batchDetailId 列表，再查 loss_record；逐条联查 batch_detail 和 product_base 填充详情 |
| 🔍读 | `GET /api/loss/monthly-stats` | GET | 按月统计损耗金额 |
| 🔍读 | `GET /api/profit/monthly` | GET | `selectMonthlyLossTotal(year, month)` — `SUM(loss_total_amount)` 按月汇总损耗金额 |
| ✏️写 | `POST /api/loss` | POST | INSERT 损耗记录（每条待损耗商品一条记录）；同时更新对应 batch_detail 状态为 3（已损耗） |
| ✏️写 | `POST /api/refund/approve` | POST | 退货审核时若 to_loss_quantity > 0，INSERT 损耗记录（退货转损耗） |

> **Mapper 自定义 SQL：**
> - `LossRecordMapper.selectMonthlyLossTotal(year, month)` — `SELECT COALESCE(SUM(loss_total_amount), 0) FROM loss_record WHERE YEAR/MONTH(create_time)`

---

## 15. promotion_image — 宣传图片表

| 操作 | API 端点 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 🔍读 | `GET /api/promotion/page` | GET | 分页查询宣传图片列表（管理端，含已删除的） |
| 🔍读 | `GET /api/promotion/active` | GET | 查询所有启用中的宣传图片（is_active=1 AND is_deleted=0，公开接口） |
| 🔍读 | `PUT /api/promotion/toggle/{id}` | PUT | 切换启用前查询是否存在 |
| 🔍读 | `DELETE /api/promotion/{id}` | DELETE | 软删除前查询是否存在 |
| ✏️写 | `POST /api/promotion` | POST | INSERT 新宣传图片 |
| ✏️写 | `PUT /api/promotion/toggle/{id}` | PUT | UPDATE is_active（0↔1 切换） |
| ✏️写 | `DELETE /api/promotion/{id}` | DELETE | UPDATE is_deleted=1（软删除，不物理删除） |

---

## 16. 无表操作 — 文件上传/静态资源

以下 API 不操作任何数据库表，仅做文件系统读写：

| API 端点 | HTTP方法 | 说明 |
|----------|----------|------|
| `POST /api/upload/image` | POST | 接收图片文件，写入本地 `picture/` 目录，返回访问 URL。直接使用 `java.nio.file.Files.write()` |
| `GET /api/picture/**` | GET | 静态资源处理器，映射 `picture/` 目录的文件为 HTTP 可访问的图片 URL |

---

## 附录 A：API 端点总览（按控制器分组）

| # | 控制器 | 端点 | 方法 | 涉及的表 |
|---|--------|------|------|----------|
| 1 | AuthController | `/api/auth/send-sms` | POST | sms_code |
| 2 | AuthController | `/api/auth/view-sms-code` | POST | sms_code |
| 3 | AuthController | `/api/auth/register-user` | POST | user_info, sms_code |
| 4 | AuthController | `/api/auth/register-admin` | POST | user_info, sms_code, admin_register_code |
| 5 | AuthController | `/api/auth/login` | POST | user_info |
| 6 | AuthController | `/api/auth/forget-password` | POST | user_info, sms_code |
| 7 | AuthController | `/api/auth/change-password` | POST | user_info |
| 8 | AuthController | `/api/auth/cancel-account` | POST | user_info |
| 9 | ProductCategoryController | `/api/product-category/page` | GET | product_category |
| 10 | ProductCategoryController | `/api/product-category/{id}` | GET | product_category |
| 11 | ProductCategoryController | `/api/product-category` | POST | product_category |
| 12 | ProductCategoryController | `/api/product-category` | PUT | product_category |
| 13 | ProductCategoryController | `/api/product-category/toggle/{id}` | PUT | product_category |
| 14 | ProductBaseController | `/api/product-base/page` | GET | product_base, product_category |
| 15 | ProductBaseController | `/api/product-base/{id}` | GET | product_base |
| 16 | ProductBaseController | `/api/product-base/list` | GET | product_base |
| 17 | ProductBaseController | `/api/product-base/stock-detail/{baseProductId}` | GET | product_base, product_batch_detail, shelf_info |
| 18 | ProductBaseController | `/api/product-base` | POST | product_base, product_category |
| 19 | ProductBaseController | `/api/product-base` | PUT | product_base, product_category |
| 20 | ProductBaseController | `/api/product-base/toggle/{id}` | PUT | product_base |
| 21 | PurchaseController | `/api/purchase/page` | GET | purchase_record, product_base |
| 22 | PurchaseController | `/api/purchase` | POST | purchase_record, product_base, product_batch_detail |
| 23 | ShelfInfoController | `/api/shelf/page` | GET | shelf_info |
| 24 | ShelfInfoController | `/api/shelf/{id}` | GET | shelf_info |
| 25 | ShelfInfoController | `/api/shelf/list` | GET | shelf_info |
| 26 | ShelfInfoController | `/api/shelf` | POST | shelf_info, shelf_capacity_tracking |
| 27 | ShelfInfoController | `/api/shelf` | PUT | shelf_info, shelf_capacity_tracking |
| 28 | ShelfInfoController | `/api/shelf/toggle/{id}` | PUT | shelf_info |
| 29 | ShelfInfoController | `/api/shelf/capacity/{id}` | GET | shelf_capacity_tracking |
| 30 | ShelfOnManageController | `/api/shelf-on/batches` | GET | product_batch_detail, product_base, shelf_info |
| 31 | ShelfOnManageController | `/api/shelf-on/shelf-layers/{shelfId}` | GET | shelf_capacity_tracking, shelf_info |
| 32 | ShelfOnManageController | `/api/shelf-on/batch-on` | POST | product_batch_detail, shelf_info, shelf_capacity_tracking |
| 33 | ShelfOnManageController | `/api/shelf-on/batch-off` | POST | product_batch_detail, shelf_capacity_tracking |
| 34 | ShelfOnManageController | `/api/shelf-on/price` | PUT | product_batch_detail |
| 35 | ShelfOnManageController | `/api/shelf-on/for-sale` | GET | product_batch_detail, product_base, shelf_info |
| 36 | ShelfOnManageController | `/api/shelf-on/reserve/{batchId}` | POST | product_batch_detail |
| 37 | ShelfOnManageController | `/api/shelf-on/release/{batchId}` | POST | product_batch_detail |
| 38 | ShelfOnManageController | `/api/shelf-on/list-valid` | GET | product_batch_detail, product_base |
| 39 | SaleOrderController | `/api/order/page` | GET | sale_order, user_info |
| 40 | SaleOrderController | `/api/order/{id}` | GET | sale_order, sale_order_item, product_batch_detail, product_base, shelf_info, user_info, sale_refund |
| 41 | SaleOrderController | `/api/order/create` | POST | sale_order, sale_order_item, product_batch_detail |
| 42 | SaleOrderController | `/api/order/pay` | POST | sale_order, sale_order_item, product_batch_detail, shelf_capacity_tracking |
| 43 | SaleOrderController | `/api/order/cancel` | POST | sale_order, sale_order_item, product_batch_detail |
| 44 | SaleOrderController | `/api/order/pay-status` | PUT | sale_order |
| 45 | SaleRefundController | `/api/refund/page` | GET | sale_refund, user_info, product_batch_detail, product_base, sale_order_item |
| 46 | SaleRefundController | `/api/refund` | POST | sale_refund, sale_order, sale_order_item |
| 47 | SaleRefundController | `/api/refund/approve` | POST | sale_refund, sale_order, sale_order_item, product_batch_detail, loss_record, shelf_capacity_tracking |
| 48 | SaleRefundController | `/api/refund/reject` | POST | sale_refund |
| 49 | SaleRefundController | `/api/refund/batch-approve` | POST | sale_refund, sale_order, sale_order_item, product_batch_detail, loss_record, shelf_capacity_tracking |
| 50 | SaleRefundController | `/api/refund/batch-reject` | POST | sale_refund |
| 51 | LossRecordController | `/api/loss/page` | GET | loss_record, product_batch_detail, product_base |
| 52 | LossRecordController | `/api/loss` | POST | loss_record, product_batch_detail, shelf_capacity_tracking |
| 53 | LossRecordController | `/api/loss/monthly-stats` | GET | loss_record |
| 54 | DashboardController | `/api/dashboard/stats` | GET | product_batch_detail, product_base, sale_order, sale_refund, shelf_capacity_tracking |
| 55 | ProfitStatController | `/api/profit/monthly` | GET | sale_order, sale_refund, purchase_record, loss_record, product_batch_detail |
| 56 | PromotionImageController | `/api/promotion/page` | GET | promotion_image |
| 57 | PromotionImageController | `/api/promotion/active` | GET | promotion_image |
| 58 | PromotionImageController | `/api/promotion` | POST | promotion_image |
| 59 | PromotionImageController | `/api/promotion/toggle/{id}` | PUT | promotion_image |
| 60 | PromotionImageController | `/api/promotion/{id}` | DELETE | promotion_image |
| 61 | UserFeedbackController | `/api/feedback/page` | GET | user_feedback |
| 62 | UserFeedbackController | `/api/feedback` | POST | user_feedback |
| 63 | UserFeedbackController | `/api/feedback/reply` | POST | user_feedback |
| 64 | FileUploadController | `/api/upload/image` | POST | *(无表，文件系统)* |
| 65 | (StaticResource) | `/api/picture/**` | GET | *(无表，文件系统)* |

---

## 附录 B：数据库表被访问频次统计

| 表名 | 涉及API数(读) | 涉及API数(写) | 总涉及API数 |
|------|:-----------:|:-----------:|:---------:|
| product_batch_detail | 13 | 10 | 23 |
| shelf_capacity_tracking | 8 | 6 | 14 |
| product_base | 15 | 3 | 18 |
| sale_order | 9 | 5 | 14 |
| shelf_info | 11 | 3 | 14 |
| sale_refund | 7 | 4 | 11 |
| user_info | 9 | 5 | 14 |
| sale_order_item | 6 | 3 | 9 |
| loss_record | 3 | 2 | 5 |
| sms_code | 4 | 4 | 8 |
| purchase_record | 2 | 1 | 3 |
| product_category | 9 | 3 | 12 |
| promotion_image | 5 | 3 | 8 |
| user_feedback | 2 | 2 | 4 |
| admin_register_code | 1 | 1 | 2 |
| *(文件系统)* | 0 | 1 | 1 |

---

## 附录 C：前端 API 封装对照

> 前端 Axios 封装位于 `smanage_frontend/src/api/`

| 前端函数 | 对应后端 API |
|----------|-------------|
| `auth.js` | |
| `sendSms(phone)` | `POST /api/auth/send-sms` |
| `viewSmsCode(phone)` | `POST /api/auth/view-sms-code` |
| `registerUser(data)` | `POST /api/auth/register-user` |
| `registerAdmin(data)` | `POST /api/auth/register-admin` |
| `login(data)` | `POST /api/auth/login` |
| `forgetPassword(data)` | `POST /api/auth/forget-password` |
| `changePassword(data)` | `POST /api/auth/change-password` |
| `cancelAccount()` | `POST /api/auth/cancel-account` |
| `product.js` | |
| `getCategoryPage(params)` | `GET /api/product-category/page` |
| `getCategoryById(id)` | `GET /api/product-category/{id}` |
| `addCategory(data)` | `POST /api/product-category` |
| `updateCategory(data)` | `PUT /api/product-category` |
| `toggleCategory(id)` | `PUT /api/product-category/toggle/{id}` |
| `getProductBasePage(params)` | `GET /api/product-base/page` |
| `getProductBaseById(id)` | `GET /api/product-base/{id}` |
| `getProductBaseList(params)` | `GET /api/product-base/list` |
| `addProductBase(data)` | `POST /api/product-base` |
| `updateProductBase(data)` | `PUT /api/product-base` |
| `toggleProductBase(id)` | `PUT /api/product-base/toggle/{id}` |
| `getShelfPage(params)` | `GET /api/shelf/page` |
| `getShelfById(id)` | `GET /api/shelf/{id}` |
| `getShelfList()` | `GET /api/shelf/list` |
| `addShelf(data)` | `POST /api/shelf` |
| `updateShelf(data)` | `PUT /api/shelf` |
| `toggleShelf(id)` | `PUT /api/shelf/toggle/{id}` |
| `getShelfCapacity(id)` | `GET /api/shelf/capacity/{id}` |
| `getShelfOnBatches(params)` | `GET /api/shelf-on/batches` |
| `getShelfLayers(shelfId)` | `GET /api/shelf-on/shelf-layers/{shelfId}` |
| `batchShelfOn(data)` | `POST /api/shelf-on/batch-on` |
| `batchShelfOff(data)` | `POST /api/shelf-on/batch-off` |
| `batchUpdatePrice(data)` | `PUT /api/shelf-on/price` |
| `getValidStockList()` | `GET /api/shelf-on/list-valid` |
| `getForSaleList()` | `GET /api/shelf-on/for-sale` |
| `reserveCartItem(batchId)` | `POST /api/shelf-on/reserve/{batchId}` |
| `releaseCartItem(batchId)` | `POST /api/shelf-on/release/{batchId}` |
| `getProductStockDetail(baseProductId)` | `GET /api/product-base/stock-detail/{baseProductId}` |
| `getDashboardStats()` | `GET /api/dashboard/stats` |
| `getPromotionPage(params)` | `GET /api/promotion/page` |
| `getActivePromotions()` | `GET /api/promotion/active` |
| `addPromotion(data)` | `POST /api/promotion` |
| `togglePromotion(id)` | `PUT /api/promotion/toggle/{id}` |
| `deletePromotion(id)` | `DELETE /api/promotion/{id}` |
| `uploadImage(file)` | `POST /api/upload/image` |
| `order.js` | |
| `getPurchasePage(params)` | `GET /api/purchase/page` |
| `addPurchase(data)` | `POST /api/purchase` |
| `getOrderPage(params)` | `GET /api/order/page` |
| `getOrderDetail(id)` | `GET /api/order/{id}` |
| `createOrder(data)` | `POST /api/order/create` |
| `payOrder(data)` | `POST /api/order/pay` |
| `cancelOrder(data)` | `POST /api/order/cancel` |
| `updateOrderPayStatus(data)` | `PUT /api/order/pay-status` |
| `getRefundPage(params)` | `GET /api/refund/page` |
| `processRefund(data)` | `POST /api/refund` |
| `approveRefund(data)` | `POST /api/refund/approve` |
| `rejectRefund(data)` | `POST /api/refund/reject` |
| `batchApproveRefund(data)` | `POST /api/refund/batch-approve` |
| `batchRejectRefund(data)` | `POST /api/refund/batch-reject` |
| `getLossPage(params)` | `GET /api/loss/page` |
| `addLoss(data)` | `POST /api/loss` |
| `getLossMonthlyStats(params)` | `GET /api/loss/monthly-stats` |
| `getMonthlyProfit(params)` | `GET /api/profit/monthly` |
| `getFeedbackPage(params)` | `GET /api/feedback/page` |
| `submitFeedback(data)` | `POST /api/feedback` |
| `replyFeedback(data)` | `POST /api/feedback/reply` |
