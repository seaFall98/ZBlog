# BACKEND_PLAN.md

> 本文职责：定义 Java 后端的工程边界、模块划分、基础能力和与 FlecBlog 前端的接口适配策略。

## 技术基线

```text
Java 21
Spring Boot 3
Spring Security
JWT
PostgreSQL
Flyway
Redis
RabbitMQ
Elasticsearch/OpenSearch 后续可选
OpenAPI
Docker Compose
```

## 模块划分

```text
common        统一响应、异常、校验、分页、审计字段
identity      用户、登录、JWT、权限
content       文章、Markdown 渲染、发布状态
taxonomy      分类、标签
comment       评论、回复、审核
friend        友链和申请
media         文件、图片上传、资源管理
site          站点配置、页面配置、菜单配置
stats         访问统计、阅读量、仪表盘数据
search        搜索端口和实现
notification  邮件/消息通知，后续扩展
ops           健康检查、日志、任务状态
```

## DDD 分层

每个核心模块按轻量 DDD 分层：

```text
controller    REST API
application   用例编排
domain        聚合、值对象、领域规则、端口
infrastructure 数据库、缓存、搜索、外部服务适配
```

不要为了形式堆抽象，小模块可以简化，但边界必须清晰。

## Markdown 内容模型

文章必须以 Markdown 为真源：

```text
content_markdown
content_html
content_text
toc_json
summary
```

后端负责生成安全 HTML 快照和 TOC，前端负责 Mermaid/代码高亮/图片交互等展示增强。

## 第一阶段后端能力

- 统一响应和异常；
- 参数校验；
- OpenAPI；
- 认证登录和 JWT；
- 文章 CRUD、草稿、发布；
- 分类标签；
- 评论；
- 友链；
- 文件上传；
- 站点配置；
- 访问统计；
- sitemap/feed 数据支撑；
- Docker Compose 本地依赖。

## 后续增强

- Redis 缓存热门文章、站点配置、阅读量；
- RabbitMQ 处理评论通知、文章发布事件、统计事件；
- 搜索端口先 PostgreSQL fallback，后续接 ES；
- Java 21 虚拟线程用于外部资源抓取、批处理、通知发送等 IO 密集场景。

