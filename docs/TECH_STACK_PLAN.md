# TECH_STACK_PLAN.md

> 本文职责：说明技术栈如何服务博客业务，避免只罗列名词。

## 前端

Nuxt 4：

- 用于博客前台 SSR、SEO、sitemap、feed；
- 直接复用 FlecBlog `blog` 工程。

Vue 3 + Vite + Element Plus：

- 用于管理后台；
- 直接复用 FlecBlog `admin` 工程。

Markdown-it / Highlight.js / Mermaid：

- 用于文章阅读体验；
- FlecBlog 前端已有基础，旧 ZBlog 的增强经验作为补充。

## 后端

Java 21：

- 统一后端语言；
- 后续虚拟线程用于 IO 密集型任务，如通知、外部链接检查、批量统计。

Spring Boot 3：

- 提供 Web、Validation、配置、健康检查和生态基础。

Spring Security + JWT：

- 后台登录、接口保护、访客身份扩展。

PostgreSQL + Flyway：

- 作为生产数据库和 schema 版本管理；
- 对齐 FlecBlog PostgreSQL 方向，避免旧 ZBlog 的 MySQL/PostgreSQL 摇摆。

Redis：

- 热门文章、站点配置、阅读量、验证码、限流、缓存。

RabbitMQ：

- 评论通知、文章发布事件、统计事件、搜索索引事件。

Elasticsearch/OpenSearch：

- 后续用于全文搜索；
- 第一阶段先保留 PostgreSQL fallback。

OpenAPI：

- 前后端接口协作；
- 后续可生成类型或作为接口检查依据。

Docker Compose：

- 本地依赖启动；
- 生产部署基础。

## 不做的事

- 不把技术栈当展示柜；
- 不为了用 MQ/ES/虚拟线程而造假场景；
- 不把第一阶段拖成技术实验。

