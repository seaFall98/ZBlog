# Batch 8 人工验收问题修复报告

## Issue 1: 文章阅读量手动浏览不增长

### 根因

Batch 8 的后端统计链路是可用的，但前台文章页的触发时机不完整。

原逻辑只在 `route.params.slug` 变化时执行：

- 设置当前 `article_id`。
- 发送带 `article_id` 的 `pageview`。

直接打开文章页或刷新文章页时，Nuxt 首屏数据通过 `useAsyncData` 初始化完成，但不会触发 slug watcher，所以没有发送文章级 pageview，最终 `articles.view_count` 不增长。

### 修复方式

1. 在文章页首屏挂载时调用 `trackArticlePageView()`，确保直接打开和刷新文章页也会发送带 `article_id` 的访问事件。
2. 将 hash 滚动逻辑抽成 `scrollToHash()`，避免首屏加载和 slug 切换两处逻辑分叉。
3. 在通用路由埋点中跳过 `/posts/` 文章页，让文章页只由文章级 pageview 负责统计，避免站点 PV 双算。
4. 调整 tracker 类型，让 `trackPageView` / `trackEvent` 返回是否尝试发送，便于后续需要时做更明确的调用判断。

### 关键代码

- `blog/app/pages/posts/[slug].vue`
- `blog/app/plugins/tracker.client.ts`

### 验证

自动验证：

```powershell
npm --prefix blog run type-check
npm --prefix blog run build
docker compose up --build -d blog
```

结果：

- `npm --prefix blog run type-check` 通过，仍有既有 `@vue/language-core` warning。
- `npm --prefix blog run build` 通过，仍有既有 Nuxt sourcemap/deprecation warning。
- Docker blog/server running-stack 重建启动成功。

运行栈复验：

```powershell
# 使用干净 Chrome profile 打开文章页，避免本机登录态影响统计。
chrome --headless=new --user-data-dir=<temp> --virtual-time-budget=6000 --dump-dom http://localhost:3000/posts/test2
```

结果：

- 第一次打开 `test2`：`articles.view_count` 从 `0` 增加到 `1`，`visit_events` 新增 1 条带 `article_id=4` 的 `pageview`。
- 第二次打开 `test2`：`articles.view_count` 从 `1` 增加到 `2`。
- 同一次第二次打开中，站点 pageview 总数从 `234` 增加到 `235`，只增加 `1`，没有出现文章页双算。

### 残余风险

如果前台博客里以超级管理员身份登录，当前 tracker 会按既有逻辑跳过统计。这属于原有“管理员访问不计入统计”的产品行为。人工验收文章阅读量时建议使用未登录窗口、无痕窗口，或普通访客身份验证。

## Issue 2: 前台阅读量比后台稳定少 1

### 根因

文章页由两个数据视图组成：

- `posts/[slug].vue` 拉取文章正文，并在客户端 mounted 后发送文章 pageview。
- `PostHeader.vue` 自己独立 SSR 拉取文章头部数据，并显示 `article.view_count`。

阅读量递增发生在页面初始渲染之后。原先 `PostHeader.vue` 只显示 SSR 时拿到的旧 `view_count`，没有消费文章页统计成功后的当前文章状态，因此后台数据库已经加 1，前台头部仍显示旧值。

### 修复方式

1. `posts/[slug].vue` 在 `$tracker.trackPageView()` 返回已尝试发送后，同步将当前文章 `view_count` 本地加 1，并写回 `useCurrentArticle()`。
2. `PostHeader.vue` 改为优先读取 `useCurrentArticle()` 中的共享文章状态；SSR 拉取到的头部文章只作为初始值写入共享状态。
3. 这样当前页面显示的是“本次访问计数后”的值，和后台数据库累计值保持一致。

### 关键代码

- `blog/app/pages/posts/[slug].vue`
- `blog/app/components/layouts/header/PostHeader.vue`

### 验证

自动验证：

```powershell
npm --prefix blog run type-check
npm --prefix blog run build
docker compose up --build -d blog
```

结果：

- `npm --prefix blog run type-check` 通过，仍有既有 `@vue/language-core` warning。
- `npm --prefix blog run build` 通过，仍有既有 Nuxt sourcemap/deprecation warning。
- Docker blog/server running-stack 重建启动成功。

运行栈复验：

```powershell
# 使用干净 Chrome CDP profile 打开普通文章页 http://localhost:3000/posts/test2
```

结果：

- 打开前数据库：`articles.view_count = 3`。
- 打开后数据库：`articles.view_count = 4`。
- 前台页面 DOM 显示：`浏览量: 4`。
- 页面 URL：`http://localhost:3000/posts/test2`。

### 残余风险

这是前台即时显示修正，依赖浏览器成功接受 tracker 发送。如果浏览器完全阻止 `sendBeacon`/`fetch keepalive`，本次访问不会计入后端，前台也不会本地加 1。当前正常浏览器、未登录/普通访客路径已验证通过。

## Issue 3: 后台访问日志默认进入时报“获取访问日志失败”

### 根因

访问日志接口的 SQL 使用 `where` 片段和排序片段拼接。

当没有任何筛选条件时，`where` 片段是：

```sql
where 1 = 1
```

排序片段拼接时缺少前导空格，最终 SQL 变成：

```sql
where 1 = 1order by created_at desc, id desc
```

这会在 PostgreSQL 中触发语法错误，导致后台页面默认进入访问日志时接口返回 500。

### 修复方式

1. 在排序 SQL 片段前补充空行/空格，保证拼接结果是合法 SQL。
2. 在 `Batch8PreDeploymentCoreClosureTest` 中补充无筛选条件默认访问日志列表测试，锁住后台首次进入访问日志页面的路径。

### 关键代码

- `server/src/main/java/com/zblog/stats/application/StatsService.java`
- `server/src/test/java/com/zblog/server/Batch8PreDeploymentCoreClosureTest.java`

### 验证

自动验证：

```powershell
mvn -f server/pom.xml -Dtest=Batch8PreDeploymentCoreClosureTest test
mvn -f server/pom.xml test
```

结果：

- Targeted Batch 8 tests：5 tests, 0 failures, 0 errors。
- Full backend tests：60 tests, 0 failures, 0 errors。

运行栈复验：

```powershell
docker compose up --build -d server
POST /api/v1/auth/login
GET /api/v1/admin/stats/visits?page=1&page_size=20
```

结果：

- `/actuator/health` 返回 `200`。
- 登录返回 `code=0`，token 存在。
- 访问日志默认列表接口返回 `200`。
- 响应包含 `data.list`，不再触发 500。

### 残余风险

访问日志的浏览器/OS/location 筛选仍是 Batch 8 的验收重点之一。当前修复只关闭“默认进入页面失败”的 SQL 拼接问题。

## Issue 4: 文件列表使用状态全部显示未使用

### 根因

文件上传时 `files.status` 默认写入 `0`，表示“未使用”。

但后续文章、设置、评论、反馈、用户、友链、动态等业务内容引用 `/uploads/**` 文件时，没有任何统一机制回写 `files.status=1`。

所以文件本身可以被前台正常访问和展示，但后台文件列表仍然只读静态字段，导致“使用状态”失真。

### 修复方式

文件列表查询时动态计算有效使用状态：

- `files.status = 1` 时仍然算“使用中”。
- 文件 `file_url` 被以下内容引用时，也算“使用中”：
  - `articles.cover_url`
  - `articles.content_markdown`
  - `articles.content_html`
  - `settings.value_text`
  - `comments.content`
  - `comments.avatar`
  - `feedbacks.form_content`
  - `feedbacks.admin_reply`
  - `users.avatar`
  - `friends.avatar`
  - `friends.screenshot`
  - `moments.content_json`

后台文件列表返回的 `status` 改为这个动态计算结果；`status=1/status=0` 筛选也使用同一套动态计算表达式，避免“显示使用中但筛选查不到”。

### 关键代码

- `server/src/main/java/com/zblog/media/application/FileService.java`
- `server/src/test/java/com/zblog/server/Batch8PreDeploymentCoreClosureTest.java`

### 验证

自动验证：

```powershell
mvn -f server/pom.xml -Dtest=Batch8PreDeploymentCoreClosureTest test
mvn -f server/pom.xml test
```

结果：

- Targeted Batch 8 tests：5 tests, 0 failures, 0 errors。
- Full backend tests：60 tests, 0 failures, 0 errors。

运行栈复验：

```powershell
docker compose up --build -d server
GET /api/v1/admin/files?keyword=<被 settings 引用的文件名>
```

结果：

- 找到真实数据库中被 `settings.value_text` 引用的文件：`/uploads/1778863362128_2bec41cda9752047bbca0ecfcd222095.png`。
- 文件列表接口返回 `200`。
- 返回文件 `upload_type=站长形象`。
- 返回文件 `status=1`，即“使用中”。

### 残余风险

当前计算覆盖项目内主要持久化引用位置。若未来新增新的业务表或新的图片字段，需要把对应引用字段加入文件使用状态计算表达式，或在后续架构批次中抽象成统一的 file-reference 关系表。
