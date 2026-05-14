# REUSE_ASSET_AUDIT.md

> 本文职责：盘点旧 `D:\MyCode\ZBlogProject\ZBlog` 的可迁移资产，明确直接复用、重构复用、仅参考和丢弃范围。

## 旧项目来源

```text
D:\MyCode\ZBlogProject\ZBlog
```

旧项目不是新主线，但有若干已经验证过的高价值成果，不能因为重启就全部丢掉。

## 可直接迁移或优先吸收

后端：

- `common/api/ApiResponse.java`：统一响应结构思路；
- `common/api/PageResult.java`：分页响应结构；
- `common/api/GlobalExceptionHandler.java`：统一异常处理方向；
- `content/domain/MarkdownRenderer.java`：Markdown 渲染端口；
- `content/infrastructure/CommonmarkMarkdownRenderer.java`：CommonMark + jsoup 安全渲染；
- `content/domain/Article.java`：文章聚合字段方向；
- `content/controller/*Response.java`：文章详情、TOC、标签、前后篇接口契约方向；
- 已有测试：`CommonmarkMarkdownRendererTest`、`BlogPostControllerTest`。

前台：

- `frontend/blog/src/pages/PostDetailPage.vue`：文章详情交互增强经验；
- `frontend/blog/src/styles/hexo.css`、`theme.css`：Markdown/代码块/主题样式经验；
- `frontend/blog/src/api/blog.ts`、`types/blog.ts`：文章详情接口契约经验；
- Mermaid、highlight.js、图片预览、TOC active 状态等实现经验。

后台：

- 上传组件和文件 API 经验；
- Markdown 编辑器替换 WangEditor 的历史结论；
- 后台文章管理字段围绕 `contentMarkdown` 而不是 HTML 真源。

## 重构后复用

- 旧 Java 后端目前只是文章链路雏形，需要扩展为完整博客系统；
- H2 测试配置可保留为测试便利，生产目标必须切到 PostgreSQL + Flyway；
- 旧前台文章详情体验可以迁移到 FlecBlog Nuxt 结构中，但不能原样覆盖 FlecBlog 页面；
- 旧后台 Klee 结构不作为主基线，但上传、权限、菜单、文件管理可作为备用参考。

## 仅参考

- 旧 docs 中关于 AI coding workflow、验收、防假完成、阶段推进的规则；
- Klee 改造经验中“不要暴露 admin 入口”“不要 HTML 作为文章真源”等结论；
- 旧项目对 Redis、MQ、ES、CDC 的长期技术规划。

## 应丢弃或避免带入

- 旧前端路线摇摆：自建 Nuxt baseline、Klee Vite SPA baseline 的争论不再作为当前主线；
- Live2D、烟花、过度装饰、二次元素材等不进入第一阶段；
- 旧项目中临时 mock、聊天室、复杂 DevWiki 内部导航不进入当前博客主线；
- 任何不符合 FlecBlog 前端复用策略的补丁式页面。

## 迁移原则

1. FlecBlog 前端源码优先；
2. 旧 ZBlog 只迁移“已经验证且 FlecBlog 不具备或不够强”的局部能力；
3. 每次迁移都要有文件级清单、验收标准和构建验证；
4. 迁移不是复制粘贴，必须适配 Nuxt/Admin 当前结构。

