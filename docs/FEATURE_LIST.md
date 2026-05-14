# FEATURE_LIST.md

> 本文职责：列出 ZBlog2 第一阶段博客网站功能清单，并标注来源、页面、接口和数据模型方向。

## 前台功能

| 功能 | 页面 | 接口方向 | 数据模型 | 来源 |
| --- | --- | --- | --- | --- |
| 首页 | `/` | 文章分页、推荐、站点配置、统计 | article, setting, stats | FlecBlog 复用 |
| 文章详情 | `/posts/:slug` | 文章详情、评论列表、相关文章 | article, comment, tag | FlecBlog + 旧 ZBlog 增强 |
| 分类 | `/categories`, `/category/:slug` | 分类列表、分类文章 | category, article | FlecBlog 复用 |
| 标签 | `/tags`, `/tag/:slug` | 标签列表、标签文章 | tag, article_tag | FlecBlog 复用 |
| 归档 | `/archive` | 按年月归档 | article | FlecBlog 复用 |
| 搜索 | 搜索弹窗/页 | 文章搜索 | article/search_index | FlecBlog 复用后扩展 |
| 评论 | 文章详情 | 评论列表、提交、回复 | comment | FlecBlog 复用 |
| 友链 | `/friend` | 友链列表、申请 | friend | FlecBlog 复用 |
| 关于 | `/about` | 页面配置 | setting/page | FlecBlog 复用 |
| SEO | 全站 | sitemap/feed/meta | article, setting | FlecBlog 复用 |

## 后台功能

| 功能 | 页面 | 接口方向 | 数据模型 | 来源 |
| --- | --- | --- | --- | --- |
| 登录认证 | `/login` | 登录、刷新、退出 | user/session | FlecBlog 参考，Java 实现 |
| 仪表盘 | `/dashboard` | 统计概览 | stats | FlecBlog 复用 |
| 文章管理 | `/articles` | CRUD、发布、草稿 | article | FlecBlog 复用 |
| Markdown 编辑器 | `/articles/new`, `/articles/:id/edit` | 保存 Markdown | article | FlecBlog + 旧 ZBlog |
| 分类管理 | `/categories` | CRUD | category | FlecBlog 复用 |
| 标签管理 | `/tags` | CRUD | tag | FlecBlog 复用 |
| 评论管理 | `/comments` | 审核、删除、回复 | comment | FlecBlog 复用 |
| 友链管理 | `/friends` | CRUD、审核 | friend | FlecBlog 复用 |
| 文件管理 | `/files` | 上传、列表、删除 | file_asset | FlecBlog 复用 |
| 站点配置 | `/settings` | 配置读写 | setting | FlecBlog 复用 |
| 访问统计 | `/visits` | 访问列表、趋势 | visit_log/stats | FlecBlog 参考 |

## 后续功能

- 项目展示；
- 相册；
- 说说 / moment；
- 订阅；
- AI 摘要；
- 一键安装器；
- 个人品牌首页。

