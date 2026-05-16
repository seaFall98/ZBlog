 已完成并推送到 GitHub。

  提交信息

  95fe838 fix: close rss reader batch

  已推送：

  main -> origin/main

  本轮 Batch 4 工作汇报

  1. RSS Reader 真实闭环完成

  本轮完成 RSS-READER-CLOSED-LOOP-BATCH-004：

  - 后台 RSS 页面新增“刷新RSS”按钮。
  - 新增后端接口：

  POST /api/v1/admin/rssfeed/refresh

  - 使用友链里的 rss_url 作为 RSS 源配置。
  - 支持手动抓取 RSS / Atom。
  - 解析 RSS item / Atom entry。
  - 将真实文章写入 rss_feed_articles。
  - 保持已有已读状态。
  - 重复刷新不会重复插入同一篇文章。
  - 抓取失败会记录明确状态：
    - friends.rss_status
    - friends.rss_last_fetch_at
    - friends.rss_last_error

  2. 数据库变更

  新增迁移：

  server/src/main/resources/db/migration/V11__batch4_rss_reader_closed_loop.sql

  包含：

  - RSS 源状态字段。
  - RSS 源最后抓取时间。
  - RSS 源最后错误信息。
  - (friend_id, link) 唯一索引，防止重复导入。

  3. 自动化验证

  已通过：

  mvn -f server/pom.xml -Dtest=Batch4RssReaderClosedLoopTest test

  结果：

  2 tests, 0 failures, 0 errors

  已通过完整后端测试：

  mvn -f server/pom.xml test

  结果：

  50 tests, 0 failures, 0 errors

  已通过前端类型检查：

  npm --prefix admin run type-check

  4. 人工验收

  你已按步骤在本地运行栈完成验收，确认：

  - 友链 RSS 源可配置。
  - 手动刷新可抓取真实 RSS。
  - 文章能出现在后台 RSS 列表。
  - 刷新页面后数据仍存在。
  - 标记已读后状态保持。
  - 重复刷新不会产生重复文章。
  - 错误源会提示失败，不再静默成功。

  5. 文档同步

  已更新：

  docs/EXECUTION_LOCK.md
  docs/DELIVERY_AUDIT.md
  CLAUDE.md

  其中：

  - Batch 4 标记为 closed, user accepted。
  - 记录自动化验证结果和人工验收结果。
  - 固化后续协作流程：

  每个 batch 结束先给人工验收步骤；
  用户确认验收通过后；
  再推代码、汇报工作、准备下一步。

---
  给 Codex / 下一轮 Claude Code 的 Batch 5 提示词

  继续 ZBlog 项目。先拉取最新 main。

  必须先阅读：
  1. docs/README.md
  2. docs/EXECUTION_LOCK.md
  3. docs/DELIVERY_AUDIT.md
  4. docs/RETRO_ACCEPTANCE_LOOP.md
  5. CLAUDE.md

  注意：
  - Batch Roadmap 已写入 docs/EXECUTION_LOCK.md。
  - Batch 1、Batch 2、Batch 3、Batch 4 已完成并人工验收。
  - 本轮只做 Batch 5：AI-ARTICLE-METADATA-BATCH-005。
  - 不要做 RSS、Search/SEO、Deployment、FlecBlog parity。
  - 每个 batch 结束必须先给用户具体人工验收步骤；只有用户确认验收通过后，才能推代码、汇报工作、准备下一步。

  目标：
  处理 AI 文章元数据闭环，明确并完成“AI 生成摘要/标题/元数据是否需要独立持久化”的产品决策，避免 AI 工具只返回文本但编辑器或文章数据不能保存、 reload
  后丢失的假完成。

  范围：
  1. 审计当前 admin 文章编辑页的 AI 功能入口。
  2. 审计当前后端 AI 工具接口：
     - AI summary
     - title generation
     - config test
     - any article metadata related endpoints
  3. 明确当前文章模型里已有字段：
     - summary
     - title
     - content_markdown
     - content_html
     - content_text
     - 是否存在 ai_summary 或类似字段
  4. 做出 Batch 5 产品决策：
     - 如果现有 `summary` 字段就是 AI 摘要落点，则确保 AI 生成结果能写入并保存到 `summary`。
     - 如果需要独立 `ai_summary` 字段，则新增迁移、DTO、后端读写、admin 编辑绑定和测试。
  5. 保证 AI 生成结果不是只在前端临时显示，而是能保存、读取、刷新页面后仍存在。
  6. 不要接入真实第三方 AI 密钥作为验收前提；测试使用 fake/local provider 或 mock HTTP server。
  7. 不要扩展到搜索、SEO、部署或最终 FlecBlog parity。

  硬性要求：
  1. 严格 TDD：
     - 先写最小 RED 集成测试，证明当前 AI metadata 结果不能形成持久化闭环，或证明当前行为需要产品决策。
     - 观察 RED 失败。
     - 再写生产代码。
  2. 测试必须证明业务效果，不只是响应 envelope：
     - AI 生成的元数据可以被保存到文章；
     - 再次读取文章能拿到该字段；
     - 重新打开编辑页所需 API 能返回该字段；
     - 如果使用 fake AI provider，测试必须证明后端实际调用或处理了 fake provider 响应。
  3. 如果产品决策是“复用 summary 字段”，不要额外创建 ai_summary。
  4. 如果产品决策是“新增 ai_summary 字段”，必须补齐：
     - Flyway migration
     - 后端 article DTO/list/detail/read/write
     - admin 编辑页绑定
     - regression test
  5. 不允许空数组、ok(null)、硬编码假数据、前端临时态假完成。
  6. 不新增 docs 根目录计划文档，只允许更新：
     - docs/EXECUTION_LOCK.md
     - docs/DELIVERY_AUDIT.md
  7. Batch 结束运行：
     - targeted AI metadata tests
     - mvn -f server/pom.xml test
     - 如果改 admin，运行 npm --prefix admin run type-check
  8. 最后先给用户人工验收步骤，不要直接推代码。

  人工验收目标：
  1. 打开后台文章编辑页。
  2. 使用 AI 生成功能生成标题/摘要/元数据。
  3. 将生成结果保存到文章。
  4. 刷新页面或重新打开文章编辑页。
  5. 确认生成结果仍然存在。
  6. 打开前台文章页或对应展示位置，确认需要展示的摘要/标题来自真实保存的数据。
  7. 如果 AI provider 未配置，确认 UI 或接口明确提示配置缺失，不是假装成功。

  完成后：
  - 更新 docs/EXECUTION_LOCK.md：Batch 5 状态、RED/GREEN 验证、deferred 项。
  - 更新 docs/DELIVERY_AUDIT.md：AI Article Metadata 从 partial 改为 verified/accepted 或记录剩余风险。
  - 不要宣称 Search/SEO/Deployment/FlecBlog parity 完成，它们不是本轮范围。