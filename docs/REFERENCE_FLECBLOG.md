# REFERENCE_FLECBLOG.md

> 本文职责：记录 FlecBlog 的本地审计结论，明确哪些前端源码优先复用、哪些后端能力只作参考、哪些内容暂不纳入当前主线。

## 本地参考源

```text
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog
```

原始仓库：

```text
https://github.com/talen8/FlecBlog
```

## 仓库结构

```text
admin/      Vue 3 + Element Plus + Vite 管理端
blog/       Nuxt 4 博客前台
server/     Go + Gin + GORM + PostgreSQL 后端
installer/  安装器
hub/        文档站
panel/      面板相关
theme/      主题相关
```

## FlecBlog 的定位

FlecBlog 是一个三端分离的博客系统：

- `server`：Go / Gin / GORM / PostgreSQL；
- `admin`：Vue 3 / Element Plus / Vite；
- `blog`：Nuxt 4 / Vue 3 / SCSS；
- 核心能力包括 SSR、SEO、Sitemap、Atom Feed、Markdown 渲染、评论、友链、统计和部署。

## 博客前台优先复用

位置：

```text
_reference\FlecBlog\blog
```

优先复用：

- Nuxt 4 工程结构；
- `app/pages` 下的博客路由体系：首页、文章详情、分类、标签、归档、关于、友链、留言等；
- `app/components/features/article` 的文章详情组件；
- `app/components/features/comment` 的评论组件；
- `app/components/layouts` 的导航、侧栏、页脚和页面框架；
- `app/utils/markdown.ts` 和 `app/assets/css/_prose.scss` 的 Markdown 阅读体验；
- `nuxt.config.ts` 中与 SEO、PWA、图片和性能相关的配置思路。

局部修改：

- 品牌、站点名、Logo、作者信息；
- API 地址和接口约定；
- 当前主线不需要的任务、说说、AI、订阅等页面可先隐藏或延后；
- 视觉风格先做必要清理，不要第一阶段就重做成个人品牌官网。

## 管理端优先复用

位置：

```text
_reference\FlecBlog\admin
```

优先复用：

- Vue 3 + Vite + Element Plus 的工程结构；
- `AdminLayout.vue`、侧栏、顶部栏和路由结构；
- `ArticleList.vue`、`ArticleForm.vue`、CodeMirror Markdown 编辑器；
- 分类、标签、评论、友链、文件、设置、统计等管理页面；
- `api/*.ts` 和 `types/*.ts` 的模块拆分方式；
- 图片上传、筛选面板、通用列表等组件。

局部修改：

- 适配 Java 后端 API；
- 清理 FlecBlog 品牌；
- 第一阶段暂时隐藏 AI、说说、RSS 订阅、反馈等非主线模块；
- 保留正式 CMS 的质感，不要改成临时工具页。

## 后端仅作参考

位置：

```text
_reference\FlecBlog\server
```

只参考：

- 文章、分类、标签、评论、友链、文件、设置、统计、用户等领域模型；
- `api/v1` 路由组织方式；
- PostgreSQL 初始化脚本和迁移节奏；
- JWT、OAuth、上传、通知、定时任务、RSS/Atom、Swagger 等方向；
- Dockerfile、docker-compose 和环境变量组织方式。

不直接复用：

- Go / Gin / GORM 代码；
- 任何机械 Java 翻译；
- 需要根据 ZBlog 实际需求重写的扩展能力。

## 第一阶段吸收清单

第一阶段必须吸收：

- 博客前台 SSR / SEO 结构；
- 管理后台正式 CMS 结构；
- Markdown 编辑与阅读体验；
- 分类、标签、归档、评论、友链、文件、设置、统计；
- Docker 部署和环境变量分层。

第一阶段暂缓：

- 强个人品牌首页；
- 项目展示；
- DevWiki Studio；
- 相册、说说；
- AI 摘要、MCP、复杂通知集成；
- 一键安装器。
