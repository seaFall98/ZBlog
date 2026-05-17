# Batch 9 计划：Redis + 轻量 MQ

## 目标

Batch 9 只实现 Redis + 应用层轻量 MQ，不实现 Debezium、Elasticsearch、OpenResty/Nginx+Lua、Kafka 或部署硬化。

## 范围

- 将 Redis 接入后端和 Docker 运行栈。
- 文章阅读量增加 10 秒访客级防抖，PostgreSQL 仍是最终事实源。
- 增加 Redis 近期热门榜，并保留 PostgreSQL 总阅读榜。
- 增加 `/collect` 轻量限流，避免单访客短时间刷爆采集接口。
- 增加 `/stats/site` 短 TTL Redis 缓存，缓存失效或 Redis 不可用时回退 PostgreSQL。
- 增加 `event_outbox` + RabbitMQ 最小闭环。
- 文章从草稿发布时写入 `ARTICLE_PUBLISHED` outbox 事件，消费者幂等创建后台通知。
- Redis+Nginx+Lua 只进入后续部署/网关路线图，本批次不写生产网关代码。

## 不做

- 不做 Debezium/PG CDC。
- 不做 Elasticsearch。
- 不做 Kafka。
- 不做 OpenResty/Lua 生产配置。
- 不把 Redis 作为唯一事实源。
- 不把 RabbitMQ 用作替代数据库事务的“魔法完成态”。

## 验证

- 已先写 `Batch9RedisOutboxRabbitMqTest` 并观察 RED。
- 目标测试证明 Redis 防抖、近期热门榜、总阅读榜、限流、站点统计缓存，以及 outbox 发布/消费/幂等/失败可见。
- 用户人工验收前不提交、不推送。
