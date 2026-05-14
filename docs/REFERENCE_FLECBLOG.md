# REFERENCE_FLECBLOG.md

> 本文职责：记录 FlecBlog 的本地审计结论，明确哪些前端源码优先复用、哪些后端能力只参考、哪些内容暂不纳入当前主线。

## 本地参考源

```text
D:\MyCode\ZBlogProject\ZBlog2\_reference\FlecBlog
```

当前浅克隆自：

```text
https://github.com/talen8/FlecBlog
```

仓库结构：

```text
admin/      Vue 3 + Element Plus + Vite 管理端
blog/       Nuxt 4 博客前台
server/     Go + Gin + GORM + PostgreSQL 后端
installer/  安装器
hub/        文档站
panel/      面板相关
theme/      主题相关
```

## FlecBlog 官方定位

FlecBlog 是三端分离博客系统：

- `server`：Go 1.25 / Gin / GORM / PostgreSQL；
- `admin`：Vue 3 / Element Plus / Vite；
- `blog`：Nuxt 4.3.1 / Vue 3.5 / SCSS；
- 重点能力包括 SSR、SEO、Sitemap、Atom Feed、Markdown 渲染、评论、友链、统计、部署。

## Blog 端优先复用

位置：

```text
_reference\FlecBlog\blog
```

优先复用：

- Nuxt 4 工程结构；
- `app/pages` 的博客路由体系：首页、文章详情、分类、标签、归档、关于、友链、留言等；
- `app/components/features/article` 的文章详情组件；
- `app/components/features/comment` 的评论组件；
- `app/components/layouts` 的导航、侧栏、页脚和页面框架；
- `app/utils/markdown.ts`、`app/assets/css/_prose.scss` 的 Markdown 阅读体验；
- `nuxt.config.ts` 中 SEO、PWA、图片、性能相关配置思路。

局部修改：

- 品牌、站点名、Logo、作者信息；
- API 地址和接口契约；
- 不适合当前博客主线的 ask、moment、AI、订阅等页面先隐藏或后置；
- 视觉风格只做必要清理，不第一阶段大改成个人品牌官网。

## Admin 端优先复用

位置：

```text
_reference\FlecBlog\admin
```

优先复用：

- Vue 3 + Vite + Element Plus 工程结构；
- `AdminLayout.vue`、侧栏、顶部栏和路由结构；
- `ArticleList.vue`、`ArticleForm.vue`、CodeMirror Markdown 编辑器；
- 分类、标签、评论、友链、文件、设置、统计等后台页面；
- `api/*.ts`、`types/*.ts` 的模块拆分方式；
- 图片上传、筛选面板、通用列表等组件。

局部修改：

- 适配 Java 后端 API；
- 清理 FlecBlog 品牌；
- 初期隐藏 AI、moment、RSS 订阅、反馈等非当前主线模块；
- 后台保留正式 CMS 质感，不改成临时工具页。

## Server 端只参考

位置：

```text
_reference\FlecBlog\server
```

只参考：

- 文章、分类、标签、评论、友链、文件、设置、统计、用户等领域模型；
- `api/v1` 路由组织；
- PostgreSQL 初始化脚本和迁移节奏；
- JWT、OAuth、上传、通知、定时任务、RSS/Atom、Swagger；
- Dockerfile、docker-compose 和环境变量配置方式。

不复用：

- Go/Gin/GORM 代码不进入 ZBlog2 后端；
- 不做机械 Java 翻译；
- AI、MCP、Feishu、微信、moment 等扩展能力先后置。

## 第一阶段吸收清单

第一阶段必须吸收：

- 博客前台 Nuxt SSR/SEO 架构；
- 管理后台正式 CMS 结构；
- 文章 Markdown 编辑和阅读体验；
- 分类、标签、归档、评论、友链、文件、设置、统计；
- Docker 部署和环境变量分层。

第一阶段暂缓：

- 强个人品牌首页；
- 项目展示；
- DevWiki Studio；
- 相册、说说；
- AI 摘要、MCP、复杂通知集成；
- 一键安装器。

