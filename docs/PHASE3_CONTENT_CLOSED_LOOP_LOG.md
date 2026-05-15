# PHASE3_CONTENT_CLOSED_LOOP_LOG.md

> 本文职责：记录 Phase 3 文章系统闭环的实际落地结果、验证命令、当前边界和下一阶段建议。

## 本次落地

时间：2026-05-15 本地执行记录。

已完成：

- 后端统一响应从字符串码调整为 FlecBlog 前端兼容的数值码：成功为 `code=0`；
- 登录响应改为 `access_token`、`token_type`、`expires_in`，兼容后台登录页；
- 引入 `spring-boot-starter-jdbc`、Flyway PostgreSQL 支持和测试用 H2 PostgreSQL mode；
- 创建 Flyway baseline：`categories`、`tags`、`articles`、`article_tags`、`menus`；
- 加入 baseline 种子数据：默认分类、默认标签、示例文章、顶部和底部菜单；
- 按轻量 DDD 结构落地 `site`、`taxonomy`、`content` 模块；
- 实现公共 API：菜单、分类、标签、文章列表、文章详情、随机文章 slug；
- 实现后台 API：分类 CRUD、标签 CRUD、文章列表、详情、创建、更新、删除、发布、取消发布；
- 实现基础 Markdown 渲染快照：`content_markdown`、`content_html`、`content_text`。

## 已验证

已执行：

```powershell
cd D:\MyCode\ZBlogProject\ZBlog\server
mvn test
mvn package

cd D:\MyCode\ZBlogProject\ZBlog\blog
npm run type-check
npm run build

cd D:\MyCode\ZBlogProject\ZBlog\admin
npm run type-check
npm run build
```

结果：

- 后端 `mvn test` 通过：15 tests, 0 failures, 0 errors；
- 后端 `mvn package` 通过，生成 Spring Boot jar；
- 前台 `type-check` 和 `build` 通过；
- 后台 `type-check` 和 `build` 通过。

## 当前边界

- Markdown 渲染目前只是基础实现，只覆盖标题和段落；后续还需要增强代码块、目录、Mermaid、图片资源处理；
- 文章导入、微信公众号导出、zip 下载接口尚未实现，后台对应按钮如果被点击，后续仍需补齐；
- 用户体系仍是 bootstrap admin，尚未替换成数据库用户名和密码哈希；
- 评论、友链、文件上传、站点配置仍属于下一阶段；
- PostgreSQL 生产连接已经按环境变量驱动配置，本地测试仍使用 H2 PostgreSQL mode。

## 下一阶段建议

推荐进入 Phase 4：评论、友链、文件上传、站点配置。

优先顺序建议：

- `site/settings`：让站点名称、头像、社交链接、SEO、菜单可后台配置；
- `friend`：补齐前台底部友链和友链页面，解决 footer 数据不完整的问题；
- `comment`：文章评论提交、审核、展示；
- `media`：图片上传，为文章封面和正文图片服务。

这个顺序比先做统计/搜索更合适，因为它直接补齐博客网站日常运营闭环。
