Batch 5 已完成到“自动化验证通过、等待你人工验收”的状态；还没有提交/推送。

  本轮产品决策

  采用你同意的方案：

  复用现有 article.summary。
  不新增 ai_summary 字段。

  含义：

  - AI 生成“文章摘要” → 写入 summary。
  - AI 生成“AI 总结” → 也写入 summary。
  - AI 生成“标题” → 写入 title。
  - 后端不新增 ai_summary 字段。
  - 前台文章页不再读取 article.ai_summary，而是使用真实持久化的 article.summary。

  已完成自动化验证

  通过：

  mvn -f server/pom.xml -Dtest=Batch5AiArticleMetadataClosedLoopTest test

  结果：

  1 test, 0 failures, 0 errors

  通过完整后端测试：

  mvn -f server/pom.xml test

  结果：

  51 tests, 0 failures, 0 errors

  通过 admin 类型检查：

  npm --prefix admin run type-check

  blog 类型检查也跑了，仍然是之前已知的本地警告：

  vue-router/volar/sfc-route-blocks 缺少 @vue/language-core

  不是本轮改动引入的新问题。

---
  Batch 5 人工验收步骤

  1. 启动本地栈

  在项目根目录运行：

  docker compose up --build -d
  docker compose ps

  确认：

  server / admin / blog / postgres 都正常运行

  后台地址：

  http://localhost:4000

  前台地址：

  http://localhost:3000

---
  2. 配置 DeepSeek AI

  进入后台：

  系统设置 / AI 设置

  填写 OpenAI-compatible 配置：

  Base URL: https://api.deepseek.com
  API Key: 使用你的 DeepSeek key
  Model: deepseek-chat

  如果页面里有摘要提示词、AI 总结提示词、标题提示词，可以先保持默认，也可以填简单测试提示词。

  保存配置。

  然后点击：

  测试配置

  期望结果：

  测试成功 / 配置可用

  如果失败，先检查：

  1. API Key 是否有效
  2. Base URL 是否是 https://api.deepseek.com
  3. Model 是否是 deepseek-chat
  4. 本地网络是否能访问 DeepSeek

---
  3. 验证 AI 生成标题

  进入后台：

  文章管理

  新建一篇文章，或者编辑一篇草稿文章。

  填写正文，例如：

  # Batch 5 AI 验收

  这是一篇用于验证 AI 文章元数据持久化的文章。
  我们要确认 AI 生成的标题和摘要保存后不会丢失。

  点击标题输入框旁边的 AI 生成图标。

  期望结果：

  标题被 AI 生成结果替换。

  然后点击：

  保存

  刷新页面，或返回文章列表后重新打开这篇文章。

  期望结果：

  AI 生成的标题仍然存在。

---
  4. 验证“生成摘要”写入 summary

  打开文章设置抽屉。

  找到：

  文章摘要

  点击：

  生成摘要

  期望结果：

  文章摘要输入框被 AI 生成结果填充。

  点击保存。

  刷新页面或重新打开文章编辑页。

  期望结果：

  文章摘要仍然存在，没有丢失。

---
  5. 验证“AI 总结”也写入同一个 summary

  还是在文章设置抽屉里，找到：

  文章摘要

  现在这里应该有两个按钮：

  生成摘要
  AI 总结

  点击：

  AI 总结

  期望结果：

  AI 总结生成结果写入同一个“文章摘要”输入框。

  注意：本轮不再有单独的“AI 总结”字段，这是预期行为。

  点击保存。

  刷新页面或重新打开文章编辑页。

  期望结果：

  AI 总结生成的内容仍然保存在“文章摘要”里。

  这就是 Batch 5 的核心验收点：AI 结果不再停留在前端临时字段，而是保存到真实的 summary。

---
  6. 验证前台文章页使用真实 summary

  把这篇文章设为发布。

  打开前台文章详情页：

  http://localhost:3000/posts/你的文章-slug

  期望结果：

  文章页顶部的 AI 摘要展示使用的是刚才保存的 summary。

  如果你看页面源码或 SEO 信息，描述信息也应该来自 summary。

---
  7. 验证刷新后仍存在

  分别刷新：

  1. 后台文章编辑页
  2. 前台文章详情页

  期望结果：

  标题和摘要都仍然存在。

---
  验收通过标准

  如果下面都满足，就可以认为 Batch 5 人工验收通过：

  1. DeepSeek 配置测试成功。
  2. AI 生成标题能写入 title。
  3. 保存后重新打开文章，标题仍存在。
  4. “生成摘要”能写入文章摘要 summary。
  5. “AI 总结”也写入同一个文章摘要 summary。
  6. 保存后刷新/重新打开，摘要仍存在。
  7. 前台文章页展示的是保存后的 summary。
  8. 页面里不再出现一个无法持久化的独立 ai_summary 编辑字段。

  已知设计决策，不是问题：

  1. 不新增 ai_summary 数据库字段。
  2. AI 总结提示词配置项仍保留，因为 /admin/ai/ai-summary 仍作为生成工具存在。
  3. 两种摘要生成方式最终都保存到 article.summary。

  你验收通过后，我再提交并推送 Batch 5，然后汇报本轮工作和准备 Batch 6。