# Batch 7 Issues: 文档误判与假完成风险

## Issue 1: 全局访问统计闭环被误读为文章级浏览量闭环

### Phenomenon

部署前审计发现，公开文章详情页上的文章级 `view_count` 仍可能保持为 `0` 或旧值。

此前 Batch 1 证明了 `POST /api/v1/collect` 能写入 `visit_events`，并且站点统计、后台 dashboard、趋势和访问日志能从 `visit_events` 派生数据。但这不等于 `articles.view_count` 已经随文章访问增长。

### Expected behavior

文章详情页访问一篇文章后：

- 访问事件应被记录；
- 文章级 `view_count` 应有明确更新或明确不用该字段；
- 公开文章详情、后台文章列表、侧边栏/站点统计应使用一致的计数语义；
- 不应出现文章页访问被通用 pageview 和 article-specific pageview 重复计算的问题。

### Cause

当前代码显示：

- `blog/app/pages/posts/[slug].vue` 会在文章加载后调用 tracker，发送带 `article_id` 的 pageview。
- `server/src/main/java/com/zblog/stats/application/VisitCollectionService.java` 会把 `article_id` 写入 `visit_events`。
- 但没有代码把该访问回写到 `articles.view_count`。
- `StatsService.siteStats()` 当前把 `sum(articles.view_count)` 与 `visit_events` pageview 数相加，后续如果直接增加 article counter，可能造成重复计数。

### Solution

本批次是审计 batch，不写生产代码。

已修正文档状态：

- `docs/EXECUTION_LOCK.md` 把文章浏览量/统计语义列为 Batch 8 推荐优先事项。
- `docs/DELIVERY_AUDIT.md` 把“访问统计闭环”从完全 closed 调整为 partial，并明确文章级 `view_count` 未闭环。

### Key code

- `blog/app/pages/posts/[slug].vue` — 文章详情页发送带 `article_id` 的访问埋点。
- `server/src/main/java/com/zblog/stats/application/VisitCollectionService.java` — 收集访问事件并写入 `visit_events`。
- `server/src/main/java/com/zblog/stats/application/StatsService.java` — 站点统计把 `articles.view_count` 与 `visit_events` pageview 相加。
- `server/src/main/java/com/zblog/content/infrastructure/JdbcArticleRepository.java` — 文章查询返回 `view_count`。

### Verification

- 代码审计确认当前没有 `articles.view_count` 增量更新路径。
- 已在 `docs/EXECUTION_LOCK.md` 和 `docs/DELIVERY_AUDIT.md` 记录为 Batch 8 must-fix-before-deploy。

### Residual risk or deferred work

Batch 8 实现时必须先写 RED 测试，证明访问文章后文章级浏览量不会变化，再修复。

修复时需要同时定义：

- 是否只用 `visit_events` 派生文章浏览量；
- 是否维护 `articles.view_count` 聚合列；
- 如何避免文章页重复发送 pageview；
- 站点总 PV 是否包含文章 PV、普通页面 PV，还是二者分别展示。

## Issue 2: 部分后台筛选/分页 UI 可能造成“看起来可用，实际无效”

### Phenomenon

审计发现后台多个列表页面暴露了筛选项，但后端接口没有完全证明这些筛选项都会影响结果。

重点包括：

- 访问列表筛选；
- 文章列表更多筛选；
- 评论列表筛选/分页；
- 文件列表筛选/分页。

### Expected behavior

后台 UI 中每一个可操作筛选项都应该满足至少一个条件：

- 后端真实支持并返回筛选后的结果；
- 或 UI 明确禁用/隐藏/提示暂不支持。

### Cause

当前前端类型和页面包含多种筛选参数，但后端部分 endpoint 只接收少量参数，例如访问列表后端只接收 `page` / `page_size`。

### Solution

本批次不修代码。

已把 `ADMIN-LIST-FILTER-PAGINATION-HONESTY-BATCH-009` 列为部署前推荐 batch。

### Key code

- `admin/src/views/visit/VisitList.vue` — 访问列表筛选 UI。
- `admin/src/api/stats.ts` — 访问列表请求参数传递。
- `server/src/main/java/com/zblog/stats/controller/StatsController.java` — 访问列表后端 endpoint。
- `server/src/main/java/com/zblog/content/controller/ArticleController.java` — 后台文章列表 endpoint。
- `server/src/main/java/com/zblog/comment/application/CommentService.java` — 评论列表逻辑。
- `server/src/main/java/com/zblog/media/application/FileService.java` — 文件列表逻辑。

### Verification

- 代码搜索和文件阅读确认多个 UI 参数与后端参数不完全一致。
- 已在 `docs/EXECUTION_LOCK.md` 和 `docs/DELIVERY_AUDIT.md` 记录为部署前诚实性问题。

### Residual risk or deferred work

Batch 9 应逐页判断：哪些筛选必须实现，哪些可以隐藏或延期。

不要为了“看起来完整”保留无效筛选控件。

## Issue 3: 技术栈路线初稿低估了 Redis / MQ 的学习与作品集目标

### Phenomenon

Batch 7 初稿把 Redis、MQ、CDC、Elasticsearch 主要写成部署后可选增强，容易让后续实现继续走“低成本上线优先、技术栈延后”的路线。

这与用户最新目标不一致：Redis 是必须做好的热门技术栈，MQ/outbox 也需要形成轻量但真实的链路；Elasticsearch 可以先做策略代码，但线上默认仍使用 DB 搜索。

### Expected behavior

后续路线应同时满足两类目标：

- 不能破坏 Batch 1-6 已验收的业务闭环；
- Redis、轻量 MQ/outbox、ES Strategy 需要作为有边界、有测试、有真实业务入口的作品集能力规划，而不是 README 装饰。

### Cause

初稿主要从“首次部署成本最低”的角度评估技术栈，把 Redis/MQ/ES 统一归到 post-deploy optional，没有充分体现用户的学习、面试和作品集目标。

### Solution

已修正文档状态：

- `docs/EXECUTION_LOCK.md` 把 Redis 调整为 required portfolio/architecture batch，并要求先用 PostgreSQL 闭合统计语义，再做 Redis 排行/缓存/限流。
- `docs/EXECUTION_LOCK.md` 把 ES 调整为 Strategy 代码 batch：DB 默认、ES 可配置、默认不要求部署。
- `docs/EXECUTION_LOCK.md` 把 MQ/outbox 调整为 required lightweight event chain，先做应用 outbox 和 RabbitMQ-style adapter，再考虑 Debezium/Kafka proof。
- `docs/DELIVERY_AUDIT.md` 和 `02-acceptance.md` 已同步新的路线。

### Key code

本问题是路线/文档修正，本批次不改生产代码。

### Verification

- 代码审计仍确认当前 active stack 未包含 Redis、Elasticsearch、RabbitMQ/Kafka、Debezium/CDC。
- 文档已明确：Batch 7 不实现这些技术栈，只调整后续 batch 顺序和验收标准。

### Residual risk or deferred work

后续实现 Redis/MQ/ES 时必须坚持“小而真实”：每个技术点都要绑定可验证业务场景、回归测试和人工验收步骤，不应为了复杂而复杂。

## Issue 4: 后续 batch 过细且 PG+Debezium+MQ 被推迟过多

### Phenomenon

上一版路线把文章统计、后台筛选、导入导出、邮件 outbox、Redis、ES、MQ、部署硬化拆成太多单独 batch，并且把 PG+Debezium+MQ 放在较靠后的技术栈批次。

用户反馈：batch 切得太细；PG+Debezium+MQ 现在就应该做一个最简单的功能，而不是只规划；Kafka 如果复杂可以先规划。

### Expected behavior

后续路线应该更粗粒度：

- 用一个核心闭环 batch 合并处理上线前基础功能缺口；
- 用一个 portfolio 技术栈 batch 同时处理 Redis 和最小 PG+Debezium+MQ；
- Kafka 暂不强行实现，先解释清楚用途，再决定是否做 proof。

### Cause

文档初稿为了降低每批风险，把任务拆得过细；同时过度使用“先 outbox、再 MQ、再 Debezium/Kafka”的保守路线，没有体现用户希望尽快对标 MySQL+Canal+MQ 做一个最小真实链路的目标。

### Solution

已修正文档状态：

- `docs/EXECUTION_LOCK.md` 将后续路线压缩为 Batch 8-11：核心闭环、Redis+PG/Debezium/MQ 最小链路、ES Strategy+部署硬化、FlecBlog parity。
- `docs/issues/batch7/02-acceptance.md` 已同步新的粗粒度验收顺序。
- Kafka 被明确为后续 explain/proof，不作为当前最小 MQ 链路的强依赖。

### Key code

本问题是路线/文档修正，本批次不改生产代码。

### Verification

- 文档已明确 `REDIS-PG-DEBEZIUM-MQ-MINIMAL` 是下一个 portfolio 技术栈 batch。
- 文档已明确 PG+Debezium+MQ 要做一个最小真实业务流，而不是继续停留在规划层。

### Residual risk or deferred work

Batch 9 开始前必须选定一个足够小的事件流，避免 PG+Debezium+MQ 一次性扩成大工程。Kafka 需要先用通俗说明解释用途，再决定是否值得 proof。

## Issue 5: 访问地理/浏览器/OS 信息应后台可见但前台不展示

### Phenomenon

当前访问列表中的 location/browser/os 仍是 `unsupported`。上一版文档把详细访问地理/浏览器/OS 解析列为 post-deploy deferred。

用户反馈：这些信息可以现在做，但希望和 FlecBlog 有差异；后台可以保存和查看，前台不要展示这些相对隐私的信息，例如评论区不要展示用户地区/IP/浏览器/OS。

### Expected behavior

- 后端可以解析、保存、读取访问地理/浏览器/OS，用于后台访问分析。
- 管理后台可以展示这些信息给站点管理员。
- 公开前台不展示这些隐私相关信息，尤其评论区不展示 IP/地区/浏览器/OS。
- 如 API 保留读取能力，也应明确区分 admin/public contract。

### Cause

初稿只从“是否补完整 visit log 功能”角度评估，没有单独记录隐私展示边界。

### Solution

已修正文档状态：

- `docs/EXECUTION_LOCK.md` 新增 `Privacy-aware visit detail recommendation`。
- `docs/issues/batch7/02-acceptance.md` 将访问解析并入 Batch 8，并明确后台保存/读取、前台不展示。
- `docs/DELIVERY_AUDIT.md` 增加 visit privacy 说明。

### Key code

本问题是路线/文档修正，本批次不改生产代码。

### Verification

- 文档已明确 Batch 8 应处理 backend/admin-only visit detail parsing。
- 文档已明确 public/frontend areas must not display privacy-adjacent visitor details。

### Residual risk or deferred work

Batch 8 实现时必须检查 public API 和评论区 UI，确保不会因为复用 admin 字段而把隐私信息展示到前台。
