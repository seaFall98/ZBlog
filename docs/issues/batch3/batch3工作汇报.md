本次 Batch 3 工作汇报：

  1. 用户触达邮件闭环

    - 新增 mail_outbox 持久化表。
    - 新增邮件发送抽象与 dev/test outbox sender。
    - 反馈提交会生成站长邮件记录。
    - 后台回复反馈会生成用户邮件记录。
    - 订阅会生成订阅确认邮件记录。
    - 退订接口会生成退订确认邮件记录。
    - 退订前端入口说明：FlecBlog 本身也依赖邮件中的退订链接，没有独立前端入口；当前已保留接口和 outbox 记录能力，真实邮件上线后再完整验收链接体验。
  2. 忘记密码 / 重置密码闭环

    - 新增 password_reset_tokens 表。
    - 新增 /api/v1/auth/forgot-password。
    - 新增 /api/v1/auth/reset-password。
    - forgot 会生成 expiring token，并写入 password reset 邮件记录。
    - reset 会更新密码、旧密码失效、新密码可登录。
    - token 使用后写入 used_at，不能重复使用。
  3. OAuth 决策

    - 无真实第三方凭据时不做假 OAuth。
    - /api/v1/auth/{provider} 明确返回 501 unsupported。
    - 默认 OAuth 前端按钮保持隐藏，不假装可用。
  4. 验证结果

    - RED 已观察：最初缺少 mail_outbox、forgot/reset 端点、安全放行和 OAuth unsupported。
    - Targeted test 通过：
  mvn -f server/pom.xml -Dtest=Batch3UserTouchClosedLoopTest test
    - 3 tests, 0 failures, 0 errors。
    - Full backend tests 通过：
  mvn -f server/pom.xml test
    - 48 tests, 0 failures, 0 errors。
    - 你已完成人工验收，并接受退订入口后续随真实邮件上线完善。

  给 Codex 的下一步建议：

  继续 ZBlog 项目。先拉取最新 main。

  已完成并人工验收：
  - Batch 1: backend truth data，包括 visit stats、notifications、system info。
  - Batch 2: content asset closed loop，包括 upload/public asset delivery、Markdown import/export asset handling。
  - Batch 3: user-touch closed loop，包括 durable mail outbox、feedback/subscription mail records、forgot/reset password token loop、OAuth unsupported
    decision。
    不要回改已验收内容，除非发现回归。

  必须先阅读：
  1. docs/README.md
  2. docs/EXECUTION_LOCK.md
  3. docs/DELIVERY_AUDIT.md
  4. docs/RETRO_ACCEPTANCE_LOOP.md
  5. CLAUDE.md

  下一轮建议做 Batch 4：RSS-READER-CLOSED-LOOP。

  目标：
  补齐 RSS 阅读器真实闭环，避免“只有已读状态接口，但没有外部 RSS 抓取/解析/入库”的假完成。

  范围建议：
  1. 后台 RSS 源管理或使用现有 feed source 配置。
  2. 支持手动触发抓取 RSS feed。
  3. 解析 RSS/Atom 响应。
  4. 将 feed articles 持久化到数据库。
  5. 后台列表能读取真实抓取结果。
  6. 已读/未读状态继续基于持久化记录工作。
  7. 抓取失败要有明确错误状态，不能静默成功。
  8. 如暂不做定时任务，也要明确手动刷新为本轮闭环范围，定时刷新作为后续增强。

  硬性要求：
  1. 严格 TDD：
     - 先写最小 RED 集成测试，证明当前 RSS 只是假闭环或缺少抓取入库。
     - 再写生产代码。
  2. 测试必须使用 fake RSS HTTP response 或本地测试 server，不依赖公网。
  3. 测试必须证明：
     - feed source 被抓取；
     - RSS item 被解析；
     - article 被入库；
     - 再次查询能读到；
     - mark read 后未读数/状态变化；
     - 重复抓取不会重复插入同一 item。
  4. 不新增 docs 根目录计划文档，只允许更新：
     - docs/EXECUTION_LOCK.md
     - docs/DELIVERY_AUDIT.md
  5. Batch 结束运行：
     - targeted RSS tests
     - mvn -f server/pom.xml test
     - 如改前端，运行对应 admin/blog type-check
  6. 最后给出人工验收步骤。

  人工验收目标：
  - 在后台添加或使用一个 RSS 源；
  - 手动刷新；
  - 看到文章列表新增真实 RSS 条目；
  - 刷新页面后数据仍存在；
  - 标记已读后状态保持；
  - 重复刷新不会产生重复文章