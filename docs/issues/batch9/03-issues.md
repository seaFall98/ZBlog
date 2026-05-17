# Batch 9 验收问题记录

## 问题 1：文章阅读去重过重

现象：文章阅读去重生效后，同一浏览器在切换文章、切换登录用户、重启浏览器后，30 分钟内仍不增加阅读量。

期望行为：只防止短时间重复刷新刷量，不应让正常重新进入页面长期不计数。

原因：Batch 9 初版使用 `articleId + visitorId` 维度，并设置 30 分钟 Redis TTL。`visitorId` 由 IP、User-Agent、屏幕、语言派生，切换登录用户或重启浏览器不会改变它，因此 30 分钟窗口过重。

解决方向：改成 10 秒防抖，并使用新版 Redis key，避免旧 30 分钟 key 继续污染验收。

## 问题 2：热门文章排序看起来是乱的

现象：热门文章排序与右侧显示的阅读量不一致，用户肉眼看起来像乱序。

期望行为：排行榜排序口径和展示指标必须一致。

原因：后端按 Redis ZSET 的近期热度排序，但前端显示的是 PostgreSQL `articles.view_count` 总阅读量。两个指标语义不同，混在一个榜单里会造成产品口径冲突。

解决方向：不把近期热度和总阅读量混在同一个榜单。前台热门文章卡片改成两个可切换榜单：`热门榜` 使用 Redis ZSET 近期热度，展示热度；`总榜` 使用 PostgreSQL 总阅读量，展示阅读数。

## 问题 3：直接访问 `/api/v1/collect` 报错

现象：在浏览器地址栏直接输入 `http://localhost:8080//api/v1/collect` 显示错误。

期望行为：验收时应明确这是 POST 采集接口，不是 GET 页面。

原因：`/api/v1/collect` 是 `POST` JSON API，浏览器地址栏会发 `GET`，因此不能用这种方式验证限流。

解决方向：验收文档保留接口行为说明，人工验收时用页面访问或 POST 请求验证。

## 问题 4：新建并发布文章没有通知

现象：后台新建文章并直接勾选发布后，没有看到文章发布通知。

期望行为：草稿发布和新建时直接发布都应生成 `ARTICLE_PUBLISHED` outbox 事件，并最终产生后台通知。

原因：`ArticleService.publish(id)` 会创建 outbox 事件，但 `ArticleService.create()` 在 `is_publish=true` 时直接调用 `articleRepository.publish(id)`，绕过了统一发布逻辑。

解决方向：新建时直接发布改为调用 `publish(id)`，保证两条路径都进入同一个 outbox 事件链。

## 问题 5：发布文章出现 Internal Server Error

现象：用户新建文章时看到 `Internal server error`。

期望行为：如果 slug 重复，应返回清晰的业务错误，而不是 500。

原因：日志显示数据库唯一约束冲突：`articles_slug_key`，重复 slug 为 `mqtest111`。后端没有把 `DuplicateKeyException` 转换为业务响应。

解决方向：创建/更新文章时捕获重复 slug，返回 `409` 和明确错误信息。
