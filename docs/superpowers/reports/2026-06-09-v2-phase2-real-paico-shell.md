# ZBlog V2 Phase 2 真实 Paico 前台交付报告

> Date: 2026-06-09  
> Branch: `feat/v2-phase2-real-paico-shell`

## 完成范围

- [x] Shell/API inventory
- [x] 文章详情 Markdown 和真实 TOC
- [x] 相册空间画廊和图片详情弹层
- [x] 分类封面 admin/server/front 闭环
- [x] Moments mood/tag/link/location 映射
- [x] Guestbook 真实 GET/POST 和竖排留言墙
- [x] Friend Links 真实数据与申请延期文案
- [x] About/settings/menus 安全接入
- [ ] Docker V2 复验（本机 Docker daemon 未启动，未完成）

## 关键实现说明

### 文章详情

- `front` 新增 Markdown 渲染依赖 `react-markdown`、`remark-gfm`。
- `PostView` 保留 `contentMarkdown`，mapper 从 Markdown 提取 h2/h3 TOC。
- `BlogDetail` 移除静态假目录，改用真实 `ArticleToc`。
- TOC 处理了重复标题、反引号/波浪线 fenced code block，以及 `C#` / `F#` 这类标题末尾 `#` 的情况。

### 相册与图片详情

- 新增 `normalizeMediaUrl`，统一处理远程、根路径和相对媒体路径。
- `/gallery/:slug` 改为空间画廊布局，方向对齐用户提供的「空间画廊」参考：白底空间、错落照片、中心主视觉、轻量 footer。
- 图片弹层参考旧 blog modal 逻辑，实现暗色遮罩、背景虚化、左侧大图、右侧说明、关闭按钮、左右键、Escape、body scroll lock 和元数据面板。

### 分类封面

- 后端 category create/update 已接收并写入 `cover_url`。
- Admin 分类表单新增封面 URL 输入与预览。
- `/categories` 为无封面分类提供 Paico 风格 fallback，避免空洞或破版。

### Moments

- `mapMoment` 已导出并测试。
- mood 优先使用 `content.mood`；无 mood 时用第一个 tag；再 fallback 到「平静」。
- 支持 tags、images、link、location 的前台 view model 和页面展示。

### Guestbook

- 留言提交返回值保留后端 `status/message`，toast 不再固定伪造成功文案。
- 页面首屏改为全屏留言墙方向，弹幕和留言卡片支持竖排展示。

### Friend Links / About / Menus / Settings

- Links 继续使用 `/api/v1/friends`，补 logo fallback 和申请流程延期说明。
- 新增 `/api/v1/settings/public-profile`，只返回前台安全站点资料字段，避免前端读取整个 settings group。
- Header 可使用 `/api/v1/menus`，并兼容旧 `/album` 菜单为 React 当前 `/gallery` 路由；无菜单时保留 Paico fallback。
- About/Footer 使用安全站点资料和公开 stats，保留 Paico 视觉。

## 验证结果

| 验证项 | 命令/页面 | 结果 |
|---|---|---|
| front tests | `npm --prefix D:/MyCode/ZBlogProject/ZBlog/ZBlog/front run test` | 通过：18 files / 76 tests |
| front build | `npm --prefix D:/MyCode/ZBlogProject/ZBlog/ZBlog/front run build` | 通过；存在 Vite chunk > 500 kB warning |
| admin type-check | `npm --prefix D:/MyCode/ZBlogProject/ZBlog/ZBlog/admin run type-check` | 通过 |
| admin build | `npm --prefix D:/MyCode/ZBlogProject/ZBlog/ZBlog/admin run build` | 通过；存在既有 `config.js` 非 module 提示和 chunk warning |
| server targeted test | `mvn -f D:/MyCode/ZBlogProject/ZBlog/ZBlog/server/pom.xml -Dtest=TaxonomyServiceTest test` | 通过：3 tests |
| server full tests | `mvn -f D:/MyCode/ZBlogProject/ZBlog/ZBlog/server/pom.xml test` | 未通过；失败位于既有 `Batch21ArticleTransactionGovernanceTest` 文章 tag 外键约束场景，统一审查判断与本轮 category cover 改动无直接关联 |
| review fix tests | `npm --prefix .../front run test -- src/features/blog/toc.test.ts src/features/site/siteApi.test.ts` | 通过：2 files / 7 tests |
| Docker | `docker compose up --build -d && docker compose ps` | 未完成；Docker daemon 未启动：`failed to connect to the docker API ... dockerDesktopLinuxEngine` |

## 统一 code review 结果

统一审查发现 3 个 blocking 问题，均已修复并提交：

1. Public settings 不能直接读取整个 `/settings/basic` group。  
   - 修复：新增后端 `/api/v1/settings/public-profile`，只返回 allowlist 字段；前端改用该 endpoint。
2. Header 使用真实 menus 时旧 `/album` 路由不匹配 React `/gallery`。  
   - 修复：前端 menu mapper 将 `/album` 归一为 `/gallery`。
3. Markdown TOC 会错误裁剪 `C#` / `F#` 标题末尾 `#`。  
   - 修复：只剥离带前置空格的 closing hashes，并补测试。

非阻塞风险：

- Gallery 照片特别多时，固定空间画布可能裁掉部分后续照片；后续可做分页、精选展示或动态高度。
- Header/Footer/About 各自调用 `useSiteProfile` 会重复请求；后续可抽为全局 context/cache。

## 手工验收步骤

请在本地启动后重点检查：

1. 打开 `/posts/autumn-light` 或任意真实文章详情：
   - Markdown 正文不再像纯文本；
   - 图片、代码块、列表正常；
   - 右侧目录来自真实标题；
   - 滚动后目录保持可用。
2. 打开 `/gallery/kyoto-2024`：
   - 对照空间画廊参考图，检查白底空间、错落照片、中心主视觉；
   - 点击照片，检查暗色图片详情、左图右文、右上关闭；
   - 测试 Escape、←、→ 和滚动锁定。
3. 打开 `/categories`：
   - seed 分类封面正常；
   - 无封面分类有 Paico fallback；
   - 在 admin 新增/编辑分类封面后刷新前台确认可见。
4. 打开 `/moments`：
   - 不应所有条目都固定「平静」；
   - admin 新增带「满足」等 tag 的 Moment 后，刷新前台应显示对应 tag/mood。
5. 打开 `/guestbook`：
   - 首屏留言墙全屏；
   - 弹幕和留言区是竖排方向；
   - 提交留言调用真实 API，toast 使用后端返回文案；
   - 刷新后显示符合后端审核/公开状态。
6. 打开 `/links`：
   - 友链来自真实 `/friends`；
   - 分组筛选和 logo fallback 可用；
   - 申请友链明确提示完整流程延期。
7. 打开 `/about`、Header、Footer：
   - About 保持 Paico 视觉；
   - Header 菜单可用，旧 `/album` 不应跳到无效路由；
   - Footer 站点资料 fallback 正常。
8. Docker 复验需先启动 Docker Desktop，然后运行：

```bash
cd D:/MyCode/ZBlogProject/ZBlog/ZBlog
docker compose up --build -d
docker compose ps
```

## 延期项

- 完整评论系统
- 登录/个人中心/OAuth
- 通知中心
- 全站 SEO/feeds/sitemap 深化
- 完整友链申请审核交互
- Gallery 大相册分页/精选展示优化
- Header/Footer/About 站点信息全局缓存

## 推送/合并状态

未推送、未合并。等待用户手工验收确认后，再按项目 finish flow 处理 push / merge / finish。
