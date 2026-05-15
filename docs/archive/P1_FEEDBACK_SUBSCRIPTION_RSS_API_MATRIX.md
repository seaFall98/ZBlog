# P1_FEEDBACK_SUBSCRIPTION_RSS_API_MATRIX.md

> 本文职责：记录 P1 反馈 / 订阅 / RSS feed 闭环的前后端接口对照，作为本轮代码修复的执行清单。

## 结论

P1 基础闭环已完成：前台反馈提交和工单查询、后台反馈处理、前台邮件订阅和退订、后台订阅者列表/删除、后台 RSS 文章列表和已读状态管理已接入真实数据库并有业务测试。邮件实际发送、外部 RSS 抓取调度和反馈通知推送仍是后续项。

## 反馈接口

| 前端文件 | 调用方法 | 请求路径 | 当前后端状态 | 当前问题 | 处理方式 | 验收方式 |
| --- | --- | --- | --- | --- | --- | --- |
| `blog/app/composables/api/feedback.ts` | `submitFeedback` | `POST /api/v1/feedback` | 已完成 | 原后端缺路由 | 新增 `feedbacks` 表并保存工单、类型、内容、邮箱、UA、IP | 提交后返回工单号，后台列表可查 |
| `blog/app/composables/api/feedback.ts` | `getFeedbackByTicketNo` | `GET /api/v1/feedback/ticket/{ticketNo}` | 已完成 | 原后端缺路由 | 按工单号读取未删除反馈 | 工单查询返回真实状态和回复 |
| `admin/src/api/feedback.ts` | `getFeedbackList` | `GET /api/v1/admin/feedback` | 已完成 | 原后端缺路由 | 新增分页、关键词、类型、状态、时间筛选 | 前台提交后后台列表可见 |
| `admin/src/api/feedback.ts` | `getFeedbackDetail` | `GET /api/v1/admin/feedback/{id}` | 已完成 | 原后端缺路由 | 返回指定反馈完整详情 | 详情页能看到提交内容 |
| `admin/src/api/feedback.ts` | `updateFeedback` | `PUT /api/v1/admin/feedback/{id}` | 已完成 | 原后端缺路由 | 更新状态、管理员回复和回复时间 | 更新后重读仍保留 |
| `admin/src/api/feedback.ts` | `deleteFeedback` | `DELETE /api/v1/admin/feedback/{id}` | 已完成 | 原后端缺路由 | 软删除反馈 | 删除后默认列表不可见 |

## 邮件订阅接口

| 前端文件 | 调用方法 | 请求路径 | 当前后端状态 | 当前问题 | 处理方式 | 验收方式 |
| --- | --- | --- | --- | --- | --- | --- |
| `blog/app/pages/subscribe.vue` | `handleSubscribe` | `POST /api/v1/subscribe` | 已完成 | 原后端缺路由 | 新增 `subscribers` 表，按邮箱创建或重新激活订阅者 | 订阅后后台订阅者列表可见 |
| `blog/app/pages/subscribe.vue` | `handleUnsubscribe` | `GET /api/v1/subscribe/unsubscribe?token=...` | 已完成 | 原后端缺路由 | 按退订 token 将订阅者设为 inactive | 退订后状态为 inactive |
| `admin/src/api/subscriber.ts` | `getSubscribers` | `GET /api/v1/admin/subscribers` | 已完成 | 原后端缺路由 | 新增分页列表接口 | 后台弹窗返回真实订阅者和 total |
| `admin/src/api/subscriber.ts` | `deleteSubscriber` | `DELETE /api/v1/admin/subscribers/{id}` | 已完成 | 原后端缺路由 | 软删除订阅者并设为 inactive | 删除后默认列表不可见 |

## RSS feed 接口

| 前端文件 | 调用方法 | 请求路径 | 当前后端状态 | 当前问题 | 处理方式 | 验收方式 |
| --- | --- | --- | --- | --- | --- | --- |
| `blog/server/routes/rss.xml.ts` / 订阅页链接 | public feed | `GET /rss.xml` / `GET /atom.xml` | 已存在 | 公共 feed 已由文章派生 | 保留 `SeoFeedController` | feed 从已发布文章生成 XML |
| `admin/src/api/rssfeed.ts` | `getRssArticles` | `GET /api/v1/admin/rssfeed` | 已完成 | 原后端缺路由 | 新增 `rss_feed_articles` 表、分页筛选和未读统计 | 后台 RSS 页面返回真实列表和 unread_count |
| `admin/src/api/rssfeed.ts` | `markRssArticleRead` | `PUT /api/v1/admin/rssfeed/{id}/read` | 已完成 | 原后端缺路由 | 更新单篇 `is_read=true` | 标记后该文章不再未读 |
| `admin/src/api/rssfeed.ts` | `markAllRssArticlesRead` | `PUT /api/v1/admin/rssfeed/read-all` | 已完成 | 原后端缺路由 | 批量更新所有未读文章 | 返回 affected 数量 |

## 需要新增 / 修改的后端文件

```text
server/src/main/resources/db/migration/V7__phase8_feedback_subscriptions_rss.sql
server/src/main/java/com/zblog/feedback/application/FeedbackService.java
server/src/main/java/com/zblog/feedback/controller/FeedbackController.java
server/src/main/java/com/zblog/subscription/application/SubscriptionService.java
server/src/main/java/com/zblog/subscription/controller/SubscriptionController.java
server/src/main/java/com/zblog/rssfeed/application/RssFeedAdminService.java
server/src/main/java/com/zblog/rssfeed/controller/RssFeedAdminController.java
server/src/main/java/com/zblog/config/SecurityConfig.java
server/src/test/java/com/zblog/server/P1FeedbackSubscriptionRssApiTest.java
```

## 数据表方向

```text
feedbacks
- id
- ticket_no
- report_url
- report_type
- form_content
- email
- status
- admin_reply
- reply_time
- user_agent
- ip
- feedback_time
- deleted_at

subscribers
- id
- email
- unsubscribe_token
- active
- created_at / updated_at
- deleted_at

rss_feed_articles
- id
- friend_id
- title
- link
- description
- published_at
- is_read
- created_at
```

## 验证记录

- `mvn -f server/pom.xml -Dtest=P1FeedbackSubscriptionRssApiTest test`：通过。
- `mvn -f server/pom.xml test`：通过，29 tests。
- `npm --prefix admin run type-check`：通过。
- `npm --prefix blog run type-check`：命令完成，仅有既有 `@vue/language-core` Volar 警告。
- `docker compose up --build -d`：server、blog、admin、postgres 均启动。
- `GET http://localhost:8080/api/v1/health`：200。
- `GET http://localhost:3000/subscribe`：200。
- `GET http://localhost:4000/login`：200。
- Docker 栈真实 API 验证：前台反馈提交、工单查询、前台订阅、后台登录、后台反馈处理、后台订阅者列表、后台 RSS 列表和标记已读均通过。

本环境没有浏览器自动化工具，尚未执行人工点击页面验证；已验证真实页面可访问，以及页面对应 API 在 Docker 栈中可持久化并重读。

## 本轮不做

- 订阅邮件实际发送；
- 邮件退订链接投递；
- 外部 RSS feed 自动抓取、解析和定时任务；
- 反馈处理后的站内通知或邮件通知；
- 反馈附件的额外安全扫描。

这些不阻塞 P1 基础闭环，但不能冒充已完成。
