# IMPLEMENTATION_ROADMAP.md

> 本文职责：把 ZBlog 从当前空项目推进为可上线博客网站的阶段计划、产物和验收标准写清楚。

## Phase 0：文档与参考源准备

目标：

- 初始化 ZBlog；
- 克隆 FlecBlog 到 `_reference`；
- 完成文档包；
- 明确 FlecBlog 前端优先复用和旧 ZBlog 资产迁移边界。

验证：

```powershell
git rev-parse --is-inside-work-tree
Test-Path _reference\FlecBlog
Test-Path docs
Select-String -Path docs\*.md -Pattern "本文件职责"
```

## Phase 1：前端源码迁入

目标：

- 将 FlecBlog `blog` 迁入 `blog/`；
- 将 FlecBlog `admin` 迁入 `admin/`；
- 保留 attribution；
- 完成本地安装和构建；
- 只做必要的品牌和入口整理，不重设计。

验证：

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

## Phase 2：Java 后端基础工程

目标：

- 创建 Spring Boot 3 + Java 21 后端；
- 加入统一响应、异常、参数校验、OpenAPI；
- 接入 PostgreSQL + Flyway；
- 加入登录认证和健康检查。

验证：

```powershell
cd server
mvn test
mvn package
```

## Phase 3：文章系统闭环

目标：

- 文章、分类、标签数据模型；
- Markdown 真源、HTML 快照、TOC；
- 后台文章 CRUD；
- 前台文章列表和详情 API。

验收：

- 后台可以保存草稿和发布文章；
- 前台可以读取文章列表和详情；
- Markdown、代码块、目录正常。

## Phase 4：评论、友链、文件、设置

目标：

- 评论提交、审核、展示；
- 友链展示和管理；
- 文件上传；
- 站点配置；
- 后台对应页面接入 Java API。

验收：

- 后台可以完成日常博客管理；
- 前台评论、友链、站点信息可用。

## Phase 5：统计、SEO、搜索

目标：

- 访问统计和仪表盘；
- sitemap、feed、Open Graph；
- 搜索功能；
- Redis 缓存阅读量和热门文章。

验收：

- sitemap/feed 可访问；
- 搜索可用；
- 热门/推荐数据稳定。

## Phase 6：部署上线

目标：

- Dockerfile；
- Docker Compose；
- Nginx；
- HTTPS；
- 数据备份；
- README 部署说明；
- 发布 checklist。

验证：

```powershell
docker compose config
```

## Phase 7：体验打磨

目标：

- 前台阅读体验；
- 移动端布局；
- 后台表单体验；
- loading / empty / error 状态；
- 视觉细节。

验收：

- 主要页面截图通过；
- 移动端不崩；
- 页面不粗糙，不像默认模板。
