# Batch 9 验收问题修复报告

## 修复内容

- 将文章阅读去重窗口从 30 分钟改为 10 秒，并升级 Redis 去重 key 为 `zblog:article:view:dedup:v2:*`，避免旧 key 影响新策略。
- 将热门文章卡片改为双榜：
  - 热门榜：`/api/v1/articles/hot?type=recent`，按 Redis ZSET 近期热度排序，展示 `hot_score`。
  - 总榜：`/api/v1/articles/hot?type=total`，按 PostgreSQL `view_count` 排序，展示累计阅读数。
- 新建文章直接发布时统一走 `publish(id)`，确保生成 `ARTICLE_PUBLISHED` outbox 事件。
- 文章创建/更新遇到重复 slug 时返回 `409 Article slug already exists`，不再返回 500。
- 修复文章发布通知跳转链接：`/article/edit/{id}` 改为 `/articles/edit/{id}`。
- 博客前台默认主题改为亮色；只有用户显式保存 `localStorage.theme=dark` 时才默认暗色。

## 关键代码

- `VisitCollectionService`：使用 10 秒去重 TTL，并升级 Redis 去重 key。
- `ArticleService`：新增 `type=recent|total` 双榜语义；直接发布走统一 publish；重复 slug 转业务异常。
- `BlogCache` / `RedisBlogCache` / `InMemoryBlogCache`：支持读取 Redis ZSET 热度分数。
- `HotArticlesCard.vue`：新增热门榜/总榜切换。
- `NotificationService`：文章发布通知链接改为后台真实路由 `/articles/edit/{id}`。
- `nuxt.config.ts` / `theme.ts`：默认亮色，不再跟随系统暗色。
- `Batch9RedisOutboxRabbitMqTest`：覆盖验收失败回归，包括通知链接。

## 自动化验证

- PASS：`mvn -f server/pom.xml -Dtest=Batch9RedisOutboxRabbitMqTest test`，7 tests。
- PASS：`mvn -f server/pom.xml test`，67 tests。
- PASS：`npm --prefix blog run type-check`，仍有既有 `@vue/language-core` warning。
- PASS：`npm --prefix blog run build`，仍有既有 Nuxt sourcemap/deprecation warnings。
- PASS：`npm --prefix admin run type-check`。

## Docker 运行栈验证

- PASS：`docker compose up --build -d server blog`。
- PASS：直接新建并发布文章后，outbox 状态为 `sent`，后台存在 `article_published` 通知。
- PASS：文章阅读量 10 秒防抖验证：首次计数、立刻重复不计数、窗口后再次计数。
- PASS：重复 slug 返回 `409`。
- PASS：`type=recent` 和 `type=total` 双榜 API 均返回有效数据。
- PASS：文章发布通知链接为 `/articles/edit/{id}`。
- PASS：博客首页主题 bootstrap 脚本为“默认亮色，只有 localStorage 为 dark 才暗色”。

## 残余风险

- 热门榜当前热度值是 Redis ZSET 的有效阅读次数，未做时间衰减；它是“近期运行期热度”，不是复杂推荐算法。
- `/api/v1/collect` 是 POST JSON API，不能直接用浏览器地址栏 GET 验收。
- Redis+Nginx+Lua、Debezium/CDC、Kafka 和 Elasticsearch Strategy 仍属于后续批次，不在 Batch 9 内假实现。
