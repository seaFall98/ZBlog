# Batch 9 最终报告

## 范围

Batch 9 已完成 Redis + 轻量 MQ 闭环，范围保持收敛：Redis 用于访问防抖、近期热门榜、站点统计短缓存和采集限流；RabbitMQ 配合应用 outbox 完成一个真实的文章发布通知链路。PostgreSQL 仍是事实源，Debezium、Kafka、Elasticsearch、Redis+Nginx+Lua 和部署硬化均延后。

## 验证

- RED 已观察：最初缺少 `/collect` 计数元数据、采集限流和 `event_outbox` 表；后续人工验收问题也通过回归测试暴露出 10 秒防抖、双榜、直接发布 outbox、重复 slug、通知链接等缺口。
- PASS：`mvn -f server/pom.xml -Dtest=Batch9RedisOutboxRabbitMqTest test`，7 tests。
- PASS：`mvn -f server/pom.xml test`，67 tests。
- PASS：`npm --prefix blog run type-check`，仍有既有 `@vue/language-core` warning。
- PASS：`npm --prefix blog run build`，仍有既有 Nuxt sourcemap/deprecation warnings。
- PASS：`npm --prefix admin run type-check`。
- PASS：`docker compose up --build -d server blog`。
- PASS：Docker running-stack smoke 验证了 outbox/RabbitMQ 通知、10 秒阅读防抖、重复 slug 409、双榜 API、通知链接 `/articles/edit/{id}` 和默认亮色主题。

## 人工验收

用户已在 2026-05-18 确认 Batch 9 验收通过，并确认最后两个小问题修复后可以收尾：

- 通知点击跳转到 `http://localhost:4000/articles/edit/{id}`。
- 网站默认亮色，不再默认跟随系统暗色。

## 后续建议

下一轮不建议继续往 Batch 9 塞功能。后续应单独规划 Elasticsearch Strategy、部署硬化，以及 Redis+Nginx+Lua 网关增强；Debezium/CDC/Kafka 如果要做，也应作为独立可验证批次推进。
