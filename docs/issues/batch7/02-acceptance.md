# Batch 7 验收：部署前功能与技术栈审计

## 状态

待用户人工审阅。

本批次是审计/规划 batch，没有修改生产业务代码，没有实现 Redis、Elasticsearch、MQ、CDC 或部署代码。

## 审计结论摘要

### 1. Batch 粒度修正

上一版后续 batch 切得太细，容易变成很多小规划。现在调整为更粗的 4 个后续批次：

1. **Batch 8：PRE-DEPLOYMENT-CORE-CLOSURE**
   - 合并处理文章浏览量/统计闭环；
   - 后台列表筛选/分页诚实性；
   - 导入导出/设置页诚实性；
   - 详细访问地理/浏览器/OS 解析；
   - 隐私策略：后台可以保存和读取这些信息，前台不展示。

2. **Batch 9：REDIS-PG-DEBEZIUM-MQ-MINIMAL**
   - Redis 热门文章/阅读排行、统计/设置缓存、访问去重/限流；
   - PG + Debezium + MQ 做一个最小真实功能，对标 min-lj Blog 里 MySQL + Canal + MQ 的思路；
   - Kafka 暂时不强行做，先解释清楚用途，复杂的话只规划/后续 proof。

3. **Batch 10：SEARCH-STRATEGY-DEPLOYMENT-HARDENING**
   - Elasticsearch 做 Strategy 代码，默认仍走 DB search；
   - 不要求线上部署 ES；
   - 同批做部署硬化。

4. **Batch 11：FLECBLOG-PARITY-RECHECK**
   - 最终对照 FlecBlog 复查差异。

### 2. 上线前核心功能缺口

1. **文章浏览量与统计语义闭环**
   - 文章页会发送带 `article_id` 的访问埋点。
   - 后端目前只写入 `visit_events`，没有更新 `articles.view_count`。
   - 站点总浏览量目前把 `articles.view_count` 和 `visit_events` pageview 相加，后续修复文章计数时必须避免重复计数。

2. **后台列表筛选和分页诚实性**
   - 文章、访问、评论、文件等后台列表 UI 暴露了较多筛选条件。
   - 当前后端没有完全证明这些筛选都生效。
   - 上线前应实现真实筛选/分页，或隐藏/禁用暂不支持的筛选项。

3. **导入导出与设置页诚实性**
   - Markdown 导入当前主要支持已上传的 `/uploads/**` 图片引用。
   - 远程/相对图片、HTML 图片重写、公众号专用格式化导出等仍是部分能力。
   - 上传设置 UI 与后端实际硬编码限制之间需要进一步对齐。

4. **访问地理/浏览器/OS 解析与隐私展示边界**
   - 现在访问列表里的 location/browser/os 仍是 `unsupported`。
   - 可以现在做解析和保存，后台用于访问分析。
   - 前台相关位置不展示这些隐私相关信息，例如评论区不显示用户 IP/地区/浏览器/OS。
   - 可以保留后端读取能力，但公开 UI 默认不暴露。

### 3. 技术栈路线修正

1. **Redis：必做，并且要有真实业务闭环**
   - Redis 不再作为“可选装饰项”处理，而是必须做好的作品集/面试技术栈。
   - 候选闭环：热门文章/阅读排行、统计缓存、设置缓存、访问采集幂等/去重/限流。
   - 可以考虑类似 `@RedisTopic` 的自动订阅封装，作为 Spring 扫描注解、动态注册 Topic、自动绑定监听器的简历亮点。
   - 边界：Redis Pub/Sub 不持久化消息，不能承载核心可靠投递；关键业务事件仍应走 MQ/CDC/outbox。

2. **PG + Debezium + MQ：现在就做一个最小功能**
   - 不再只规划。
   - 目标是对标 min-lj Blog 里的 MySQL + Canal + MQ 思路，但改成 PostgreSQL + Debezium + MQ。
   - 只做一个最简单的真实业务流：一个源表/事件、一个队列/topic、一个消费者副作用、可观察的错误/重试状态。
   - Kafka 暂时不强行做；Kafka 主要适合高吞吐、可持久化、可重放的事件流平台，如果现在理解和部署成本太高，可以先规划。

3. **Elasticsearch：做策略代码，不作为线上默认依赖**
   - 线上默认仍使用 DB-backed search。
   - 后续 ES work 应做成 `SearchPort` / Strategy：DB 默认适配器 + 可配置启用的 ES 适配器。
   - ES scope 必须包含索引结构、文章创建/更新/删除索引、重建索引、失败降级、策略选择测试。
   - 不要求当前部署环境启动 Elasticsearch 服务。

## 人工审阅步骤

请审阅以下内容是否符合你的产品目标和学习目标：

1. 打开 `docs/EXECUTION_LOCK.md`。
2. 查看 `Active Work Slot` 中 Batch 7 的审计结论：
   - Current functional gaps
   - Current technology gaps
   - Redis candidate flows and recommendation
   - Elasticsearch strategy recommendation
   - PostgreSQL CDC / Debezium / RabbitMQ/Kafka recommendation
   - Privacy-aware visit detail recommendation
   - Must-fix-before-deploy list
   - Required portfolio-enhancement list
   - Optional or post-deploy deferred list
   - Updated batch order
3. 打开 `docs/DELIVERY_AUDIT.md`。
4. 确认它已经把 `view_count` 从“全局统计闭环”里拆出来，标成仍需修复的文章级计数问题。
5. 确认推荐顺序是否合理：
   - Batch 8 先粗粒度修核心功能缺口；
   - Batch 9 做 Redis + PG/Debezium/MQ 最小真实链路；
   - Batch 10 做 ES Strategy + 部署硬化；
   - Batch 11 做 FlecBlog 最终复查。
6. 确认隐私策略是否符合预期：
   - 后台可以保存/读取访问地理、浏览器、OS 等分析信息；
   - 前台不展示这些相对隐私的信息，尤其是评论区不展示用户地区/IP/浏览器/OS。
7. 如果你认可，就回复“Batch 7 审计通过”。
8. 如果不认可，请指出你想调整的地方：
   - Batch 8 是否还要继续拆；
   - PG+Debezium+MQ 的最小功能应该选哪个业务流；
   - Kafka 是否需要提前解释或提前 proof；
   - 访问隐私字段哪些能后台展示，哪些连后台也不该保存。

## 验收通过后的流程

用户确认后，再写 `docs/issues/batch7/05-final-report.md`，然后 commit/push。
