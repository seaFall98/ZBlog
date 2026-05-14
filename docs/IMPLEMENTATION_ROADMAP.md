# IMPLEMENTATION_ROADMAP.md

> 本文职责：把 ZBlog2 从当前空项目推进到可上线博客网站的阶段计划、产物和验收标准写清楚。

## Phase 0: 文档与参考源准备

目标：

- 初始化 ZBlog2；
- 克隆 FlecBlog 到 `_reference`；
- 完成文档包；
- 明确 FlecBlog 前端优先复用和旧 ZBlog 资产迁移边界。

验收：

```powershell
git rev-parse --is-inside-work-tree
Test-Path _reference\FlecBlog
Test-Path docs
Select-String -Path docs\*.md -Pattern "本文职责"
```

## Phase 1: 前端源码迁入

目标：

- 将 FlecBlog `blog` 迁入 `blog/`；
- 将 FlecBlog `admin` 迁入 `admin/`；
- 保留 attribution；
- 完成本地安装和构建；
- 只做必要品牌和入口清理，不重设计。

验收：

```powershell
cd blog
npm install
npm run type-check
npm run build

cd ..\admin
npm install
npm run type-check
npm run build
```

## Phase 2: Java 后端基础工程

目标：

- 创建 Spring Boot 3 + Java 21 后端；
- 加入统一响应、异常、校验、OpenAPI；
- PostgreSQL + Flyway；
- 登录认证和 JWT；
- 健康检查。

验收：

```powershell
cd server
mvn test
mvn package
```

2026-05-15 执行结果：
- `server/` Spring Boot 后端基础工程已创建。
- 已落地统一响应、统一异常、参数校验、JWT 登录、无状态安全配置、健康检查、OpenAPI 入口。
- `mvn test` 通过：5 tests, 0 failures, 0 errors。
- `mvn package` 通过：生成 Spring Boot 可运行 jar。
- 详细记录见 `docs/PHASE2_BACKEND_FOUNDATION_LOG.md`。

## Phase 3: 文章系统闭环

目标：

- 文章、分类、标签数据模型；
- Markdown 真源、HTML 快照、TOC；
- 后台文章 CRUD；
- 前台文章列表和详情 API。

验收：

- 后台可保存草稿和发布文章；
- 前台可读取文章列表和详情；
- Markdown、代码块、目录正常。

2026-05-15 执行结果：
- `server/` 已落地文章、分类、标签、菜单数据库 baseline。
- 已实现公开文章读取闭环：菜单、分类、标签、文章列表、文章详情、随机文章。
- 已实现后台文章、分类、标签 CRUD 和文章发布/取消发布。
- 已接入 Flyway + JDBC + PostgreSQL 生产配置，测试使用 H2 PostgreSQL mode。
- 已调整统一响应为前端兼容的 `code=0` 数值码。
- `mvn test` / `mvn package` / 前后台 `type-check` / 前后台 `build` 均通过。
- 详细记录见 `docs/PHASE3_CONTENT_CLOSED_LOOP_LOG.md`。

## Phase 4: 评论、友链、文件、设置

目标：

- 评论提交、审核、展示；
- 友链展示和管理；
- 文件/图片上传；
- 站点配置；
- 后台对应页面接入 Java API。

验收：

- 后台可完成日常博客管理；
- 前台评论、友链、站点信息可用。

执行结果：

- 已完成评论、友链、文件上传、站点设置的后端闭环。
- 新增 Phase 4 集成测试，覆盖设置读取/更新、友链管理/申请、评论提交/审核/删除、文件上传/列表/删除。
- `mvn test` / `mvn package` / 前后台 `type-check` / 前后台 `build` 均通过。
- 详细记录见 `docs/PHASE4_INTERACTION_SITE_MEDIA_LOG.md`。

## Phase 5: 统计、SEO、搜索

目标：

- 访问统计和仪表盘；
- sitemap、feed、Open Graph；
- 搜索功能；
- Redis 缓存阅读量和热门文章。

验收：

- sitemap/feed 可访问；
- 搜索可用；
- 热门/推荐文章数据稳定。

## Phase 6: 部署上线

目标：

- Dockerfile；
- Docker Compose；
- Nginx；
- HTTPS；
- 数据备份；
- README 部署说明；
- 发布 checklist。

验收：

```powershell
docker compose config
```

并完成云服务器部署 smoke test。

## Phase 7: 体验打磨

目标：

- 前台阅读体验；
- 移动端布局；
- 后台表单体验；
- loading/empty/error；
- 视觉细节。

验收：

- 主要页面截图验收；
- 移动端不崩；
- 页面不粗糙、不像默认模板。
