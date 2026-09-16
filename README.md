# 超市管理系统 (SupermarketManage)

首先强调，这只是一个数据库原理学校课程的期末大作业，存在缺陷，仅用于参考，1.5天完成的课程设计，大量使用AI。

前后端分离的超市进销存管理系统，支持商品管理、进货入库、货架上架、收银选购、订单支付、退货退款、损耗登记、月度盈利统计等功能。

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.0 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.4 |
| 前端框架 | Vue 3 | 3.4 |
| 构建工具 | Vite | 5.0 |
| UI 组件库 | Element Plus | 2.5 |
| 状态管理 | Pinia | 2.1 |
| 路由 | Vue Router | 4.2 |
| HTTP 客户端 | Axios | 1.6 |
| 图片裁剪 | Cropper.js | 2.1 |
| 密码加密 | Spring Security Crypto (BCrypt) | - |
| 认证 | JWT (jjwt) | 0.12.3 |
| Java | OpenJDK | 21 |
| Node.js | - | ≥ 18 |
| 包管理 | npm / Maven | - |

## 环境要求

| 软件 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 21 | 后端编译和运行 |
| Maven | 3.6+ | 后端依赖管理和打包 |
| Node.js | 18+ | 前端开发服务器和构建 |
| npm | 9+ | 前端依赖安装（随 Node.js 附带） |
| MySQL | 8.0+ | 数据库 |

## 项目结构

```
SupermarketManage/
├── smanage_backend/          # Spring Boot 后端
│   ├── src/main/java/com/supermarket/
│   │   ├── common/           # 通用类（Result、异常）
│   │   ├── config/           # 配置（Web、JWT 拦截器）
│   │   ├── controller/       # 控制器
│   │   ├── entity/           # 实体类
│   │   ├── mapper/           # MyBatis Mapper
│   │   └── service/          # 业务逻辑
│   ├── src/main/resources/
│   │   └── application.yml   # 应用配置
│   └── pom.xml               # Maven 配置
│
├── smanage_frontend/         # Vue 3 前端
│   ├── src/
│   │   ├── api/              # API 请求封装
│   │   ├── components/       # 公共组件
│   │   ├── router/           # 路由配置
│   │   ├── store/            # Pinia 状态管理
│   │   ├── utils/            # 工具函数
│   │   └── views/            # 页面组件
│   ├── index.html            # 入口 HTML
│   ├── vite.config.js        # Vite 配置
│   └── package.json          # npm 配置
│
├── picture/                  # 商品和宣传图片存储
├── 建表.md                   # 数据库建表脚本
├── 数据库表结构.md            # 表结构文档
└── 功能操作说明.md            # 功能操作手册
```

## 快速开始

### 1. 安装环境

**macOS:**

可以用下面的，也可以自己官网下载对应版本后导入环境

```bash
# 安装 Homebrew（如果未安装）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装 JDK 21
brew install openjdk@21

# 安装 Maven
brew install maven

# 安装 Node.js (含 npm)
brew install node

# 安装 MySQL
brew install mysql
```

**Windows:**

AI给的链接仅做参考，可以直接去哔哩哔哩找教程安装

- JDK 21: https://adoptium.net/download/
- Maven: https://maven.apache.org/download.cgi
- Node.js: https://nodejs.org/ (选择 LTS 版本)
- MySQL: https://dev.mysql.com/downloads/installer/

### 2. 创建数据库

下面是AI给的，也可以直接在navicat手动创建，建表语句就是建表.md。

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE supermarket_manage DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

# 导入建表脚本（在项目根目录执行）
mysql -u root -p supermarket_manage < 建表.md
```

### 3. 配置数据库连接

编辑 `smanage_backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/supermarket_manage?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: 你的数据库密码
```

### 4. 启动后端

如果不用下面的，可以直接用idea运行就行，推荐直接idea。

```bash
cd smanage_backend

# 安装依赖并打包
mvn clean package -DskipTests

# 启动
java -jar target/smanage-backend-1.0.0.jar

# 后端运行在 http://localhost:8088
```

### 5. 启动前端

推荐从vscode打开文件夹运行。

```bash
cd smanage_frontend

# 安装依赖（仅首次）
npm install

# 启动开发服务器
npm run dev

# 前端运行在 http://localhost:5173
```

### 6. 访问系统

浏览器打开 `http://localhost:5173`，即可进入登录页面。

## 默认账号

系统首次使用需要注册账号：

| 角色 | 注册方式 |
|------|----------|
| 普通用户 | 注册页面 → 选择"普通用户" → 填写信息 |
| 管理员 | 需先在数据库 `admin_register_code` 表中插入一条校验码，再通过注册页面选择"管理员"注册 |

```sql
-- 插入管理员注册校验码（示例）
INSERT INTO admin_register_code (code, total_usable_times) VALUES ('ADMIN2026', 1);
```

## 数据库配置

| 配置项 | 默认值 |
|--------|--------|
| 地址 | localhost:3306 |
| 数据库名 | supermarket_manage |
| 字符集 | utf8mb4 |
| 用户名 | root |
| 密码 | 自行设置 |

## 常用命令

```bash
# 前端
cd smanage_frontend
npm install          # 安装依赖
npm run dev          # 开发模式运行
npm run build        # 生产构建

# 后端
cd smanage_backend
mvn clean package    # 打包
mvn spring-boot:run  # 直接运行

# 数据库
# 清空所有数据（保留表结构）
mysql -u root -p supermarket_manage -e "
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE admin_register_code;
TRUNCATE TABLE loss_record;
TRUNCATE TABLE product_batch_detail;
TRUNCATE TABLE product_base;
TRUNCATE TABLE product_category;
TRUNCATE TABLE promotion_image;
TRUNCATE TABLE purchase_record;
TRUNCATE TABLE sale_order_item;
TRUNCATE TABLE sale_order;
TRUNCATE TABLE sale_refund;
TRUNCATE TABLE shelf_capacity_tracking;
TRUNCATE TABLE shelf_info;
TRUNCATE TABLE sms_code;
TRUNCATE TABLE user_feedback;
TRUNCATE TABLE user_info;
SET FOREIGN_KEY_CHECKS = 1;"
```

## 端口说明

| 服务 | 端口 |
|------|------|
| 后端 API | 8088 |
| 前端开发服务器 | 5173 |
| MySQL | 3306 |

- 前端开发服务器自动将 `/api` 请求代理到 `http://localhost:8088`
- 图片通过 `/api/picture/` 访问，映射到项目根目录的 `picture/` 文件夹
