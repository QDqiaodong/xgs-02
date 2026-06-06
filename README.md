# 家常食谱共享创作社区

面向家常美食爱好者搭建的原创菜谱沉淀平台。用户可在线创作、浏览、收藏家常食谱，支持菜系/场景/难度多维度筛选，通过 Redis 有序集合缓存热门榜单，提供流畅的浏览与创作体验。

## 技术栈

### 前端
- **框架**: Vue 3.4 + Vite 5
- **UI 组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **Markdown 解析**: marked
- **样式**: SCSS
- **特色功能**:
  - 自研轻量化 Markdown 编辑器（支持实时预览、工具栏快捷操作）
  - 食材表格组件（常用食材快速添加、增删行操作）
  - 路由懒加载 + 图片懒加载
  - 响应式布局，适配移动端与桌面端

### 后端
- **框架**: Spring Boot 3.3 + JDK 17
- **ORM**: MyBatis-Plus 3.5.5
- **构建工具**: Maven
- **缓存**: Redis 7（ZSet 有序集合存储热门榜单）
- **数据库**: MySQL 8.0
- **工具**: Lombok、Jackson
- **特色功能**:
  - Redis 有序集合缓存月度热门食谱榜单，每日凌晨定时刷新
  - 图片无损压缩工具（根据文件大小自适应压缩质量）
  - 菜品配图本地存储 + 静态资源访问
  - 逻辑删除 + 自动填充创建/更新时间
  - 全局统一异常处理与响应封装

### 容器化部署
- **Docker**: 多容器独立部署
- **编排**: Docker Compose
- **镜像优化**:
  - 前端：npm 淘宝镜像，分层隔离依赖与源码
  - 后端：阿里云 Maven 镜像，分层拆分 pom.xml 与源码
  - 构建缓存：仅依赖文件变更时才重新下载，源码修改仅触发重新编译
  - 纯原生 Docker 分层缓存，无 BuildKit 语法依赖
- **健康检查**: MySQL、Redis 均配置健康检查，依赖服务就绪后再启动应用

## 端口映射

所有端口均可在 `.env` 文件中统一配置修改。

| 服务 | 宿主机端口 | 容器内部端口 | 说明 |
|------|-----------|-------------|------|
| 前端 (Nginx) | 8025 | 80 | 静态资源 + API 代理 |
| 后端 (Spring Boot) | 9025 | 8088 | REST API 服务 |
| MySQL | 3325 | 3306 | 数据库 |
| Redis | 6425 | 6379 | 缓存 |

> 避开常用默认端口：80、443、8080、3306、6379、5432、9200 等

## 快速开始

### Docker 一键启动（推荐）

```bash
chmod +x start.sh
./start.sh
```

启动脚本会自动：
1. 检测端口占用，自动顺延分配可用端口
2. 构建并启动所有容器
3. 等待服务就绪
4. 输出前端访问地址

启动成功后访问：
- 前端: http://localhost:8025
- 后端API: http://localhost:9025/api

### 手动 Docker 命令

```bash
# 构建并启动
docker compose up -d --build

# 查看日志
docker compose logs -f

# 停止服务
docker compose down

# 重启服务
docker compose restart
```

### 本地开发

#### 前端开发

```bash
cd frontend
npm install
npm run dev
```

#### 后端开发

```bash
cd backend
mvn clean package -DskipTests
java -jar target/recipe-backend.jar
```

## 核心功能模块

### 1. 原创食谱编撰
- 分步填写食材清单、烹饪流程
- 自研 Markdown 编辑器，支持粗体/斜体/标题/列表/引用/链接/图片等格式
- 食材表格组件，支持快捷添加常用食材
- 插入菜品实拍图，支持图片压缩上传
- 自定义菜系标签
- 支持草稿暂存、随时二次编辑发布

### 2. 食谱分类查阅
- 菜系、用餐场景、制作难度三重标签筛选
- 关键词搜索（匹配标题与描述）
- 分页列表展示
- 首页基于收藏数自动生成热门菜谱榜单（Top 10）
- Redis 缓存加速访问，每日定时刷新

### 3. 个人食谱收藏夹
- 一键收藏/取消收藏目标菜谱
- 支持批量勾选取消收藏
- 收藏数据独立归档

### 4. 个人菜谱中心
- 统一管理本人全部发布食谱
- 可修改内容或下架废弃菜谱
- 草稿与已发布分类管理

### 5. 图片上传与压缩
- 支持图片上传，最大 10MB
- 自动压缩：大于 2MB 压缩质量 0.6，大于 1MB 压缩质量 0.7，其余 0.8
- UUID 重命名避免文件名冲突
- 本地磁盘存储 + 静态 URL 访问

## 项目结构

```
.
├── frontend/                 # 前端项目
│   ├── src/
│   │   ├── components/      # 公共组件（RecipeCard、IngredientsTable、MarkdownEditor等）
│   │   ├── views/          # 页面（首页、食谱列表、详情、创建编辑、收藏、我的菜谱）
│   │   ├── router/         # 路由配置
│   │   ├── store/          # Pinia 状态管理
│   │   ├── utils/          # 工具函数（API 请求封装）
│   │   └── assets/styles/  # 全局样式
│   ├── .npmrc              # npm 镜像配置
│   ├── Dockerfile
│   ├── nginx.conf
│   └── vite.config.js
├── backend/                 # 后端项目
│   ├── .m2/
│   │   └── settings.xml    # Maven 镜像配置
│   └── src/main/java/com/recipe/
│       ├── controller/     # 控制器（Recipe、Favorite、Upload、User）
│       ├── service/        # 服务层接口与实现
│       ├── mapper/         # 数据访问层（MyBatis-Plus）
│       ├── entity/         # 实体类（Recipe、Favorite）
│       ├── dto/            # 数据传输对象
│       ├── config/         # 配置类（MyBatis-Plus、Redis、WebMvc、元对象处理器）
│       ├── util/           # 工具类（图片压缩）
│       ├── exception/      # 全局异常处理
│       └── common/         # 公共类（统一响应结果）
├── docker/                  # Docker 配置
│   └── mysql/              # MySQL 配置与初始化脚本
├── scripts/                 # 工具脚本
│   └── port-check.sh       # 端口检测脚本
├── .env                     # 全局环境变量
├── docker-compose.yml       # Docker Compose 配置
├── start.sh                # 一键启动脚本
└── README.md
```

## 数据库设计

### recipe 菜谱表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID，自增 |
| title | VARCHAR(255) | 菜谱标题 |
| description | TEXT | 菜谱描述 |
| cover_image | VARCHAR(500) | 封面图片URL |
| author | VARCHAR(100) | 作者 |
| cook_time | INT | 烹饪时间（分钟） |
| difficulty | TINYINT | 难度：1-简单，2-中等，3-困难 |
| favorite_count | INT | 收藏数 |
| view_count | INT | 浏览数 |
| tags | VARCHAR(500) | 标签，逗号分隔 |
| ingredients | TEXT | 食材清单（JSON格式） |
| steps | TEXT | 烹饪步骤（JSON格式） |
| tips | TEXT | 小贴士 |
| status | TINYINT | 状态：0-下架，1-上架 |
| is_draft | TINYINT | 是否草稿：0-否，1-是 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除：0-未删，1-已删 |

### favorite 收藏表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID，自增 |
| user_id | BIGINT | 用户ID |
| recipe_id | BIGINT | 菜谱ID |
| created_at | DATETIME | 创建时间 |
| deleted | TINYINT | 逻辑删除 |

## API 接口

### 食谱相关
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/recipes/hot` | 获取热门食谱榜单 |
| GET | `/api/recipes` | 分页查询食谱列表（支持关键词、菜系、场景、难度筛选） |
| GET | `/api/recipes/{id}` | 获取食谱详情（浏览数+1） |
| POST | `/api/recipes` | 创建食谱 |
| PUT | `/api/recipes/{id}` | 更新食谱 |
| DELETE | `/api/recipes/{id}` | 删除食谱（逻辑删除） |
| POST | `/api/recipes/draft` | 保存草稿 |
| GET | `/api/recipes/drafts` | 获取草稿列表 |

### 收藏相关
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/favorites` | 获取收藏列表 |
| POST | `/api/favorites` | 添加收藏 |
| DELETE | `/api/favorites/{recipeId}` | 取消收藏 |
| DELETE | `/api/favorites/batch` | 批量取消收藏 |

### 上传相关
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/upload/image` | 上传图片（自动压缩） |

### 用户相关
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/recipes` | 获取用户发布的食谱列表 |

## 配置说明

### .env 环境变量

项目根目录 `.env` 文件集中管理所有配置：

```bash
# Docker 镜像仓库（国内镜像源，无需 VPN）
DOCKER_REGISTRY=docker.m.daocloud.io

# Compose 项目名
COMPOSE_PROJECT_NAME=xgs02-recipe-app

# 宿主机映射端口（可自定义）
FRONTEND_PORT=8025
BACKEND_PORT=9025
MYSQL_PORT=3325
REDIS_PORT=6425

# 容器内部端口（一般无需修改）
BACKEND_INTERNAL_PORT=8088
MYSQL_INTERNAL_PORT=3306
REDIS_INTERNAL_PORT=6379
NGINX_INTERNAL_PORT=80

# MySQL 配置
MYSQL_ROOT_PASSWORD=recipe123456
MYSQL_DATABASE=recipe_db
MYSQL_USER=recipe_user
MYSQL_PASSWORD=recipe123456

# 时区
TZ=Asia/Shanghai
```

### 国内镜像源配置

#### 前端 npm
- 配置文件: `frontend/.npmrc`
- 镜像源: `https://registry.npmmirror.com`

#### 后端 Maven
- 配置文件: `backend/.m2/settings.xml`
- 镜像源: 阿里云公共仓库
- 镜像策略: `<mirrorOf>*</mirrorOf>` 全局镜像

#### Docker 基础镜像
- 配置文件: `.env` 中的 `DOCKER_REGISTRY`
- 镜像仓库: `docker.m.daocloud.io`
- 全链路共用：node、maven/jdk、mysql、nginx 所有基础镜像统一使用

### Docker 构建缓存机制

**核心原理**: 利用 Docker 原生分层缓存，依赖文件与业务源码分层复制。

#### 后端缓存分层
```
第1层: 复制 .m2/settings.xml （Maven 配置）
第2层: 复制 pom.xml         （依赖描述）
第3层: mvn dependency:resolve （下载依赖）
第4层: 复制 src/            （业务源码）
第5层: mvn clean package    （编译打包）
```

- 若 `pom.xml` 无变更：第 1-3 层缓存复用，不重新下载依赖
- 若仅 `src/` 下源码修改：仅执行第 4-5 层，即重新编译
- 首次构建：全量下载依赖 + 完整编译

#### 前端缓存分层
```
第1层: 复制 .npmrc          （npm 配置）
第2层: 复制 package*.json   （依赖描述）
第3层: npm install          （安装依赖）
第4层: 复制源码             （业务代码）
第5层: npm run build        （构建打包）
```

- 若 `package.json` / `package-lock.json` 无变更：第 1-3 层缓存复用
- 若仅源码修改：仅执行第 4-5 层
- 首次构建：全量安装依赖 + 完整构建

> 注意：本项目严格使用 Docker 原生分层缓存，**未使用** `# syntax=docker/dockerfile:*` 等 BuildKit 扩展语法。

## MySQL 优化配置

- 关闭慢查询日志
- 禁用闲置存储引擎 (MyISAM, BLACKHOLE, FEDERATED, ARCHIVE, MEMORY)
- 优化连接池与缓存配置
- 字符集统一使用 utf8mb4

## 性能优化

### 前端
- 路由懒加载
- 图片懒加载
- 组件代码分割
- Gzip 压缩（Nginx）
- 静态资源 CDN 友好

### 后端
- Redis 缓存热门数据，每日定时刷新
- 图片压缩存储，减少带宽占用
- 数据库连接池优化（HikariCP）
- 逻辑删除，避免物理删除风险
- 定时任务刷新缓存，避免缓存击穿

### Docker
- 多阶段构建减小镜像体积
- 分层缓存加速构建
- 构建后清理临时文件
- 统一镜像仓库地址

## 常见问题

### Q: 端口被占用怎么办？
A: 运行 `./start.sh` 脚本会自动检测端口占用并顺延分配可用端口。也可以手动修改 `.env` 文件中的端口配置。

### Q: 依赖下载慢怎么办？
A: 项目已配置国内镜像源（npm 淘宝镜像、Maven 阿里云镜像、Docker DaoCloud 镜像），无需 VPN 即可正常拉取。

### Q: 如何修改 MySQL 密码？
A: 修改 `.env` 文件中的 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_PASSWORD`，然后执行 `docker compose down -v && docker compose up -d` 重建数据库（注意：删除卷会丢失数据）。

### Q: 每次修改代码都要重新下载依赖吗？
A: 不会。Docker 分层缓存会复用已下载的依赖层，只有当 `pom.xml` 或 `package.json` 变更时才会重新下载依赖。仅修改业务源码只会触发重新编译/构建。

### Q: 热门榜单如何更新？
A: 热门榜单数据存储在 Redis 有序集合中，后端每天凌晨 2 点自动从数据库重新计算并刷新缓存。收藏数变更时会同步更新缓存，确保数据一致性。
