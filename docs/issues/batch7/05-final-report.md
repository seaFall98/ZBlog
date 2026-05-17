# Batch 7 Final Report: 部署前功能与技术栈审计

## 状态

已通过用户人工审阅。

- 审阅通过时间：2026-05-17
- 本批次性质：审计/规划 batch
- 生产代码变更：无
- Redis / Elasticsearch / MQ / CDC / 部署代码实现：无

## 本批次完成内容

### 1. 修正文章浏览量与统计闭环判断

Batch 7 重新审计后确认：

- `POST /api/v1/collect` 写入 `visit_events` 是真实闭环；
- 站点统计、后台 dashboard、趋势、访问日志能从 `visit_events` 派生数据；
- 但文章详情页发送的 `article_id` pageview 并不会更新 `articles.view_count`；
- `total_page_views` 当前还存在 `sum(articles.view_count)` 与 visit-event pageview 相加的重复计数风险。

因此文档已把“全局访问统计闭环”与“文章级浏览量闭环”拆开，文章级 `view_count` 被列入 Batch 8 核心修复内容。

### 2. 梳理上线前基础功能缺口

已记录并排序以下基础缺口：

- 文章浏览量与统计语义；
- 后台文章、访问、评论、文件列表筛选/分页诚实性；
- 导入导出与上传设置 UI/后端能力对齐；
- 邮件/通知可靠性与后续 outbox/MQ 链路基础；
- 访问地理/浏览器/OS 解析。

其中访问详情解析明确采用隐私边界：后台可以保存和读取，公开前台不展示，尤其评论区不展示用户地区/IP/浏览器/OS。

### 3. 修正技术栈路线

根据用户审阅反馈，路线调整为：

- Redis 是必做作品集能力，不是可选装饰；
- PG + Debezium + MQ 需要尽快做一个最小真实功能，对标 min-lj Blog 里 MySQL + Canal + MQ 的思路；
- Kafka 先作为后续解释/proof，不强行当前实现；
- Elasticsearch 做 Strategy 代码，默认仍使用 DB search，不要求线上部署 ES；
- 后续 batch 粒度不再切得过细。

### 4. 更新后续 batch 顺序

用户已确认新的粗粒度路线：

1. **Batch 8：PRE-DEPLOYMENT-CORE-CLOSURE**
   - 文章统计闭环；
   - 后台筛选/分页诚实性；
   - 导入导出/设置诚实性；
   - 访问地理/浏览器/OS 后台解析与前台隐私屏蔽。

2. **Batch 9：REDIS-PG-DEBEZIUM-MQ-MINIMAL**
   - Redis 排行/缓存/限流；
   - PG + Debezium + MQ 最小真实链路；
   - Kafka 暂缓为后续 proof。

3. **Batch 10：SEARCH-STRATEGY-DEPLOYMENT-HARDENING**
   - ES Strategy 代码，DB 默认；
   - 部署硬化。

4. **Batch 11：FLECBLOG-PARITY-RECHECK**
   - 最终对照 FlecBlog 做差异确认。

## 修改文件

- `docs/EXECUTION_LOCK.md`
- `docs/DELIVERY_AUDIT.md`
- `docs/issues/batch7/02-acceptance.md`
- `docs/issues/batch7/03-issues.md`
- `docs/issues/batch7/05-final-report.md`

## 验证

本批次没有修改生产代码，因此没有运行全量测试。

验证方式：

- 通过代码搜索和文件阅读支撑审计结论；
- 明确记录 Batch 7 不实现 Redis、Elasticsearch、MQ、CDC 或部署代码；
- 用户已人工审阅并回复“通过”。

## 下一步

进入 **Batch 8：PRE-DEPLOYMENT-CORE-CLOSURE**。

建议 Batch 8 开始前先写 RED 测试，覆盖：

- 文章访问后 `view_count` 语义；
- 站点 PV 不重复计数；
- 后台筛选参数真实影响结果；
- 访问地理/浏览器/OS 只在后台展示，前台不暴露隐私字段。
