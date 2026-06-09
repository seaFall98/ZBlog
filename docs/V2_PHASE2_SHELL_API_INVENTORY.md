# ZBlog V2 Phase 2 Shell/API Inventory

> 任务 0 产物。本文档只记录当前 Paico React `front` 壳子与 `/api/v1`、`admin` 支撑之间的盘点事实，作为后续 Phase 2 实现顺序依据。当前任务不修改 `ZBlog/blog/`，不扩大实现范围。

## 盘点口径

- **Front 当前实现：** 以 `ZBlog/front/src/App.tsx`、`ZBlog/front/src/pages/`、`ZBlog/front/src/features/` 为准。
- **Server API：** 以 `ZBlog/server/src/main/java/com/zblog/**/controller/*Controller.java` 中 `/api/v1` public/admin 路由为准。
- **Admin 支撑：** 以 `ZBlog/admin/src/router/index.ts`、`ZBlog/admin/src/api/`、`ZBlog/admin/src/views/` 的已存在管理页面为准。
- **处理方式分类：** `已接入 / 待接 front / 待补 server / 待补 admin / 明确延期`。
- **Shell-critical：** 当前 Paico 壳子真实化是否需要本阶段优先处理。

## Inventory 矩阵

| Front 区域 / 页面 / 组件 | 当前实现状态 | 需要的数据 / API | Server 是否已有 API | Admin 是否已有支撑 | 处理方式 | 是否 shell-critical | 备注 |
|---|---|---|---|---|---|---|---|
| 首页 `/` | 已通过 hooks 读取文章、相册、Moments、站点统计；Hero 文案、站点名仍硬编码。 | 文章列表、精选/近期文章、相册封面、Moments 摘要、有限站点统计、站点文案。 | 已有 `GET /api/v1/articles`、`GET /api/v1/albums`、`GET /api/v1/moments`、`GET /api/v1/stats/site`；站点文案可考虑 `GET /api/v1/settings/{group}`。 | 文章、相册、Moments、统计已有管理入口；设置页存在。 | 已接入主要数据；站点文案与安全 settings 待接 front。 | 是 | 首页已能消费多类真实数据，但 Paico seed 文案仍是内容基线，后续替换需谨慎。 |
| Header 导航 | `Header.tsx` 使用硬编码导航；搜索跳转 `/search?q=`；登录图标跳 `/login`。 | 菜单树、站点名、搜索入口。 | 已有 `GET /api/v1/menus`；搜索已有 `GET /api/v1/articles/search`。 | 菜单管理已有 `/menus` 页面与 `admin/menus` API。 | 待接 front。 | 是 | 接菜单时不能破坏 Paico 下拉导航设计；可先只映射 navigation/footer 类型。 |
| Footer | `Footer.tsx` 导航、站点简介和订阅状态均为本地硬编码；订阅表单只本地显示成功。 | Footer 菜单、站点简介、订阅提交。 | 菜单已有 `GET /api/v1/menus`；订阅后端已有 subscription 控制器，需后续确认公共路径和字段。 | 菜单管理、设置页、订阅者管理入口存在。 | 导航/站点信息待接 front；订阅交互可后移或另列。 | 是 | 本阶段优先 footer 菜单和安全站点信息；订阅提交不是第一批核心。 |
| 文章列表 `/blog`、`/category/:slug`、`/tag/:slug` | 已使用 `usePosts` 和 `useCategories`；按 category/tag 参数请求文章列表。 | 分页文章、分类/标签过滤、封面、摘要、阅读时间、空状态。 | 已有 `GET /api/v1/articles`，支持 `category`、`tag`、`year`、`month`；已有 `GET /api/v1/categories`、`GET /api/v1/tags`。 | 文章管理、分类管理、标签管理存在。 | 已接入，后续做真实数据验收。 | 是 | 当前列表一次拉取较大 `pageSize`，后续可再接分页 UI。 |
| 文章详情 `/posts/:slug`、`/blog/:slug` | 已使用 `usePost(slug)` 和 `ArticleContent`；侧边目录仍为静态假目录；收藏/分享为本地 toast。 | 文章详情、Markdown/HTML 渲染、标签、分类、相关文章、目录、前后篇、浏览/评论数。 | 已有 `GET /api/v1/articles/{slug}`；相关文章可能由前端从列表推导；前后篇、目录需基于现有字段确认。 | 文章管理存在，Markdown 编辑存在。 | 待补 front。 | 是 | Phase 2 明确要修 Markdown 阅读体验和真实目录；收藏不是本阶段核心。 |
| 分类页 `/categories` | 已使用 `useCategories`，页面会渲染 `coverUrl`。 | 分类列表、文章数、描述、分类封面。 | 已有 `GET /api/v1/categories`；当前 taxonomy 代码未检索到 cover/image/banner 字段处理。 | 类型 `Category` 有 `cover_url?`，但 `CategoryManager.vue` 表单只含名称、描述、排序。 | 待补 server / 待补 admin / 待接 front 验收。 | 是 | 分类封面是本阶段明确问题：用户新增分类缺少封面能力。 |
| 标签页 `/tags` | 已使用 `useTags` 渲染标签云。 | 标签列表、文章数、slug。 | 已有 `GET /api/v1/tags`。 | 标签管理存在。 | 已接入，后续做真实数据验收。 | 是 | 目前无需补 server/admin。 |
| 归档 `/archive`、`/archive/:year/:month` | 已用文章列表在前端聚合归档；月份路由用文章列表过滤。 | 按年月的文章列表、年/月计数、空状态。 | 已有 `GET /api/v1/articles?year=&month=`；另有 `GET /api/v1/stats/archives` 可提供归档统计。 | 文章管理存在；统计管理存在。 | 已接入；可视需要改用 archives 统计。 | 是 | 当前真实化可先保持前端聚合，避免扩大范围。 |
| 搜索 `/search` | 已使用 `usePosts({ keyword })` 调 `articles/search`；热门搜索词硬编码。 | 搜索结果、关键词、摘要高亮、空状态、热门词。 | 已有 `GET /api/v1/articles/search`。 | 搜索维护入口存在。 | 已接入；热门词明确延期。 | 是 | 热门词可后移到搜索优化阶段。 |
| Gallery `/gallery` | 已使用 `useAlbums(50)`，列表卡片跳 `/gallery/:slug`。 | 相册列表、封面、描述、照片数、创建时间、媒体 URL。 | 已有 `GET /api/v1/albums`。 | 相册管理存在。 | 已接入，需真实数据和视觉验收。 | 是 | Roadmap 中提到的 `/gallery/{id}` 与 `/gallery/detail` 不匹配已在当前代码中以 `/gallery/:slug` 为主；`/gallery/detail` 仍映射列表页。 |
| Gallery 详情 `/gallery/:slug` | 已存在详情页并通过 `useAlbum` 读取详情。 | 相册详情、照片列表、标题/描述、照片元数据、弹层、前后切换、媒体 URL 规范化。 | 已有 `GET /api/v1/albums/{slug}`；管理端照片 CRUD API 存在。 | 相册管理和照片增删改/排序存在。 | 待补 front。 | 是 | 后续重点是照片弹层、键盘切换、元数据和媒体 URL 规范化，不需要新增旧 `blog` 改动。 |
| Moments `/moments` | 已使用 `useMoments(30)`；当前 mapper 只取 text/images/date/mood，mood 缺省为「平静」。 | 文本、图片、视频/音频/音乐/链接/位置、tags、mood。 | 已有 `GET /api/v1/moments`；管理端支持 text/images/video/audio/music/link/location/tags 等过滤字段。 | Moments 管理入口与表单存在。 | 待补 front。 | 是 | 本阶段要修「每条瞬间都显示平静」；评论明确后移。 |
| Guestbook `/guestbook` | 已 GET 留言并 POST 提交；提交后立即加本地弹幕并 toast「留言成功」。 | 留言列表、提交、审核状态、pending/success/empty/error、弹幕展示。 | 已有 `GET /api/v1/guestbook/messages`、`POST /api/v1/guestbook/messages`；admin 审核/置顶/删除 API 存在。 | 留言管理入口存在。 | 待补 front。 | 是 | 提交后不能暗示立即公开，应根据后端返回状态表达待审核或可见。 |
| Friend Links `/links` | 已使用 `useFriendLinks()` 读取分组友链；申请区引导到留言墙。 | 分组友链、logo、描述、有效/无效展示、申请入口。 | 已有 `GET /api/v1/friends`、`POST /api/v1/friends/apply`。 | 友链管理与类型管理存在。 | 列表已接入；申请流程明确后移或待决。 | 是 | 若本批不做友链申请表单，应记录为 Phase 4/backlog；当前可先保持留言墙引导。 |
| About `/about` | 当前全部为 Paico seed 静态内容。 | 博主资料、站点介绍、头像、社交链接、时间线、联系邮箱。 | 可读取 `GET /api/v1/settings/{group}`，但需限定安全 group/key。 | 设置页存在，含基础/博客设置等 tab。 | 待接 front。 | 是 | 只读取已知安全 public settings，不暴露敏感配置。 |
| Stats `/stats` | 已使用 `useSiteStats()` 和近期文章；页面是轻量统计壳子。 | 文章数、访问数、照片数、留言数、Moments 数、近期文章。 | 已有 `GET /api/v1/stats/site`、`GET /api/v1/stats/archives`、`POST /api/v1/collect`。 | Dashboard、访问日志、统计接口存在。 | 有限统计已接入；完整统计页明确延期。 | 否 | Roadmap 允许 Phase 2 只消费有限公共 stats，完整 `/stats` 归后续阶段。 |
| Login / user shell `/login` | 当前为本地演示账号 `admin / 123456`，未调用 API。 | 登录、注册/找回、刷新、退出、用户态。 | 已有 `/api/v1/auth/login`、`/register`、`/forgot-password`、`/reset-password`、`/refresh`、`/logout`；OAuth 返回未支持。 | Admin 登录已有独立实现；用户管理存在。 | 明确延期。 | 否 | Phase 2 公共前台真实化不以用户登录闭环为核心，避免扩大认证范围。 |

## Phase 2 第一批建议处理顺序

1. **文章详情阅读体验：** 修复 Markdown 渲染、真实目录、目录吸附和 Paico 视觉融合。
2. **Gallery / Albums：** 验证真实 `/albums` 数据闭环，补详情弹层、前后切换、元数据与媒体 URL 规范化。
3. **Moments：** 补 content block 映射和 mood/tags fallback，避免全部显示「平静」。
4. **Guestbook：** 对齐审核状态，修正提交成功语义和弹幕/留言墙真实数据展示。
5. **Friend Links：** 保持分组列表真实化；申请表单若不纳入本批，明确后移。
6. **About / Settings / Menus：** 只接安全 settings 和菜单树，不破坏 Paico 导航与 footer 设计。
7. **分类封面：** 补 server/admin 最小字段与表单，再验证用户新增分类可在前台显示封面。

## 明确延期项

- Moment 评论。
- Friend Links 独立申请表单（除非后续明确纳入本批）。
- 完整公共统计页与复杂统计图表。
- Public login/user 闭环、OAuth 与收藏能力。
- 搜索热门词、订阅提交等非 shell-critical 互动增强。

## 已检查的关键文件

- `ZBlog/front/src/App.tsx`
- `ZBlog/front/src/pages/`
- `ZBlog/front/src/features/`
- `ZBlog/front/src/components/layout/Header.tsx`
- `ZBlog/front/src/components/layout/Footer.tsx`
- `ZBlog/server/src/main/java/com/zblog/content/controller/ArticleController.java`
- `ZBlog/server/src/main/java/com/zblog/album/controller/AlbumController.java`
- `ZBlog/server/src/main/java/com/zblog/moment/controller/MomentController.java`
- `ZBlog/server/src/main/java/com/zblog/guestbook/controller/GuestbookController.java`
- `ZBlog/server/src/main/java/com/zblog/friend/controller/FriendController.java`
- `ZBlog/server/src/main/java/com/zblog/site/controller/MenuController.java`
- `ZBlog/server/src/main/java/com/zblog/site/controller/SettingController.java`
- `ZBlog/server/src/main/java/com/zblog/stats/controller/StatsController.java`
- `ZBlog/server/src/main/java/com/zblog/identity/AuthController.java`
- `ZBlog/admin/src/router/index.ts`
- `ZBlog/admin/src/views/article/components/CategoryManager.vue`
