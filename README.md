# ZBlog2

> 本文职责：说明 ZBlog2 的当前目标、工作区边界、文档入口和后续执行方式。

ZBlog2 当前主线是先做一个完整、现代、可上线的博客网站。类似 `https://bugstack.cn/` 的强个人品牌网站是后续规划，不作为当前阶段的工作内容。

## 工作区

正式工作目录：

```text
D:\MyCode\ZBlogProject\ZBlog2
```

本地参考源码：

```text
D:\MyCode\ZBlogProject\ZBlog2\_reference\FlecBlog
D:\MyCode\ZBlogProject\ZBlog
```

`_reference/FlecBlog` 是本地只读参考源。FlecBlog 前端源码允许优先复用：能直接用的先迁入，局部不符合 ZBlog2 目标的再修改。旧 ZBlog 只作为资产库，迁移已经验证过的高价值模块。

## 当前策略

1. 先完成文档、审计、任务节奏，不直接进入业务编码。
2. 前端以 FlecBlog 的 `blog` 和 `admin` 源码为优先复用基线。
3. 后端使用 Java 21 + Spring Boot 重新实现，不机械翻译 FlecBlog 的 Go 后端。
4. 当前第一阶段只围绕博客网站闭环：前台阅读、后台 CMS、文章编辑、分类标签、评论、友链、站点配置、统计、SEO、部署。
5. 项目展示、相册、说说、个人品牌首页、DevWiki Studio 进入后续 roadmap，不压进第一阶段主线。

## 文档阅读顺序

```text
AGENTS.md
docs/0_PROJECT_CONTEXT.md
docs/REFERENCE_FLECBLOG.md
docs/REUSE_ASSET_AUDIT.md
docs/PRODUCT_PLAN.md
docs/FEATURE_LIST.md
docs/FRONTEND_PLAN.md
docs/BACKEND_PLAN.md
docs/API_PLAN.md
docs/DATA_MODEL_PLAN.md
docs/TECH_STACK_PLAN.md
docs/IMPLEMENTATION_ROADMAP.md
docs/CODEX_WORKFLOW.md
docs/FRONTEND_MIGRATION_LOG.md
docs/PHASE2_BACKEND_FOUNDATION_LOG.md
docs/PHASE3_CONTENT_CLOSED_LOOP_LOG.md
docs/PHASE4_INTERACTION_SITE_MEDIA_LOG.md
```
