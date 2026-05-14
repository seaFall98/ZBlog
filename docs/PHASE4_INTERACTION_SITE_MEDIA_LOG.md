# Phase 4 Interaction, Site And Media Log

> 本文记录 Phase 4 的实际执行结果：评论、友链、文件上传、站点设置。

## 目标

- 让前台具备评论提交、评论展示、友链展示、站点基础信息读取能力。
- 让后台具备评论审核、友链管理、文件上传管理、站点设置管理能力。
- 在不偏离当前主线的前提下，优先补齐“博客网站”所需的互动与站点运营闭环。

## 已完成

- 新增 Flyway `V2__phase4_interaction_site_media.sql`，包含 `comments`、`friend_types`、`friends`、`files`、`settings` 基础表和默认配置数据。
- 新增评论 API：公开提交/列表/删除，后台列表/创建/状态切换/删除，返回结构兼容现有前台评论类型。
- 新增友链 API：公开分组展示与申请入口，后台友链分类与友链 CRUD。
- 新增设置 API：公开读取站点设置，后台按分组更新基础设置，并保留 MCP secret reset 占位能力。
- 新增文件 API：后台 multipart 上传、列表、删除；文件保存到本地 `uploads`，并通过 `/uploads/**` 提供静态访问。
- 更新安全配置：公开博客前台所需读取/提交接口，后台接口继续要求 JWT。

## 验证

```text
mvn -Dtest=Phase4InteractionApiTest test
Result: 4 tests, 0 failures

mvn test
Result: 11 tests, 0 failures

mvn package
Result: success

blog: npm run type-check
Result: success, with existing Volar route-block warning

blog: npm run build
Result: success, with existing Nuxt/dependency warnings

admin: npm run type-check
Result: success

admin: npm run build
Result: success, with existing chunk-size/config.js warnings
```

## 边界

- 文件存储当前为本地磁盘，适合本地联调和第一版上线前验证；正式部署前建议切换到对象存储或明确服务器磁盘备份策略。
- 评论目前是基础审核流，尚未加入反垃圾、邮件通知、IP 风控、敏感词、频率限制。
- 设置为 key-value 基础模型，后续可按页面配置、SEO 配置、社交链接、主题配置继续拆分。
- 友链申请先入库为待展示数据，站点可达性检测和人工通知放到后续增强。

## 下一阶段建议

进入 Phase 5：统计、SEO、搜索。优先顺序建议为：

1. SEO 基础：sitemap、RSS/Atom feed、Open Graph、robots.txt。
2. 搜索闭环：先做数据库 LIKE/全文索引版，后续再切 Elasticsearch。
3. 统计闭环：文章 PV/UV、后台概览卡片、基础趋势图。
