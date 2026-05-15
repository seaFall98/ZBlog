# Batch 1/2 人工复验问题修复报告

## 背景

本轮工作延续 ZBlog 闭环交付原则：功能不能只以接口返回 200、类型检查通过、或文档标记完成作为完成标准，必须经过真实前后台页面、真实后端 API、真实数据持久化与刷新后可用的验证。

Batch 1 聚焦后端真实数据闭环，Batch 2 聚焦内容资源闭环。两个 batch 都在自动化测试通过后进入人工复验，并根据人工复验发现的问题继续修复。

## Batch 1：后端真实数据闭环复验

### 问题 1：提交反馈后显示 Unauthorized

#### 现象

用户已注册并登录普通账号，但在前台提交反馈后，页面显示 Unauthorized。

#### 原因

反馈提交属于公共前台入口，不应强制要求管理员认证。当前安全配置或接口权限边界与前台真实使用方式不一致，导致前端已登录普通用户仍无法完成反馈提交。

#### 修复方式

将反馈提交相关公共接口纳入可公开访问或普通用户可访问的路径，确保前台反馈表单可以直接调用真实后端，后端持久化反馈记录，并产生对应通知数据。

#### 验证

人工复验确认反馈提交有效，通知中心可以看到反馈相关通知。通知详情页跳转仍是后续产品体验迭代，不影响本轮闭环验收。

### 问题 2：通知中心反馈通知体验不完整

#### 现象

反馈提交后通知中心出现工单通知，但点击后不是进入独立工单详情页，而是跳转到反馈页。

#### 原因

FlecBlog 主站参考行为本身没有在通知中心展示反馈工单详情。本轮 Batch 1 目标是闭环“反馈产生真实通知、通知读状态可变、未读数真实变化”，不是补齐完整工单详情页。

#### 修复/处理方式

保持当前实现：反馈提交产生真实通知，通知列表与未读状态可用；工单详情页作为后续迭代，不在 Batch 1 范围内扩大。

#### 验证

用户人工确认“先做到这个程度，等后面再慢慢迭代完善”。

## Batch 2：内容资源闭环复验

### 问题 1：后台/前台上传图片后页面加载失败

#### 现象

人工复验发现多处图片上传成功但页面显示失败：

- 后台系统设置上传站长头像后，文件管理页图片加载失败。
- 前台首页头像未显示。
- 文章编辑器上传本地图片后预览加载失败。
- 前台评论图片上传成功但评论中图片加载失败。
- 导入带 `/uploads/...` 图片的 Markdown 后，文章内容中的图片仍不显示。

#### 原因

后端上传接口返回的是相对路径：

```text
/uploads/<filename>
```

后端 `localhost:8080` 可以访问这个路径，但浏览器在不同前端页面中会按当前 origin 解析相对路径：

```text
http://localhost:4000/uploads/<filename>  # admin nginx
http://localhost:3000/uploads/<filename>  # blog Nuxt
```

Docker 场景下，admin nginx 和 blog Nuxt 起初没有把 `/uploads/**` 代理到后端，所以文件真实存在、后端也可访问，但前端页面 origin 下访问失败。

#### 修复方式

##### admin nginx 代理 `/uploads/**`

在 `admin/nginx.conf` 增加上传资源代理，并使用 `^~` 保证优先级高于图片静态资源正则匹配：

```nginx
location ^~ /uploads/ {
    proxy_pass http://server:8080/uploads/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

这里曾经出现过一次二次问题：初版写成 `location /uploads/`，但 nginx 的 `location ~* \.(png|jpg|...)$` 正则会抢走 PNG 请求，导致 admin 端仍然 404。改成 `location ^~ /uploads/` 后，`/uploads/**` 会优先进入代理。

##### blog Nuxt 代理 `/uploads/**`

新增 `blog/server/routes/uploads/[...path].ts`：

```ts
export default defineEventHandler(async event => {
  const path = event.context.params?.path;
  const assetPath = Array.isArray(path) ? path.join('/') : path;
  if (!assetPath) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' });
  }

  const config = useRuntimeConfig();
  const apiUrl = (config.apiServerUrl || config.public.apiUrl || '').replace(/\/api\/v\d+\/?$/, '');
  if (!apiUrl) {
    throw createError({ statusCode: 503, statusMessage: 'Asset backend is not configured' });
  }

  return proxyRequest(event, `${apiUrl}/uploads/${assetPath}`);
});
```

##### 修正 `.gitignore` 隐藏路由文件的问题

新增 Nuxt 路由后发现 `git status` 中没有出现该文件。原因是 `.gitignore` 中全局忽略了：

```gitignore
uploads/
```

这会把 `blog/server/routes/uploads/[...path].ts` 也忽略掉。修复为只放开这个 Nuxt 路由源码，不放开运行时上传目录：

```gitignore
uploads/
!blog/server/routes/uploads/
!blog/server/routes/uploads/[...path].ts
```

#### 验证

Docker 中上传同一个真实文件后，三端访问同一 `/uploads/...` 路径均返回 200 且字节数一致：

```text
http://localhost:8080/uploads/... -> 200
http://localhost:4000/uploads/... -> 200
http://localhost:3000/uploads/... -> 200
```

人工复验确认文章中的图片、文章编辑页图片显示已恢复。

### 问题 2：文章编辑页“是否发布”按钮无效

#### 现象

文章编辑页打开“是否发布”后保存，文章仍显示未发布。此前 `Hello ZBlog` 这篇文章可以发布，说明发布能力曾经可用，但编辑页保存路径没有闭环。

#### 原因

后台文章编辑页保存时调用的是：

```ts
PUT /api/v1/admin/articles/{id}
```

请求体包含：

```ts
is_publish: formData.is_publish
```

但后端 `ArticleService.update(...)` 原本只更新标题、内容、分类、标签等元数据，没有根据 `is_publish` 调用 publish/unpublish。单独的 `/publish` 接口可用，但真实编辑页保存路径没有用它。

#### 修复方式

在 `ArticleService.update(...)` 中处理 `is_publish`：

```java
Map<String, Object> updated =
    articleRepository.update(
        id,
        title,
        slug,
        markdown,
        rendered.html(),
        rendered.text(),
        textOrDefault(request, "summary", value(existing, "summary")),
        nullableTextOrDefault(request, "cover", value(existing, "cover")),
        nullableLong(request, "category_id"),
        tagIds(request),
        nullableTextOrDefault(request, "location", value(existing, "location")),
        boolOrDefault(request, "is_top", (Boolean) existing.get("is_top")),
        boolOrDefault(request, "is_essence", (Boolean) existing.get("is_essence")),
        boolOrDefault(request, "is_outdated", (Boolean) existing.get("is_outdated")));
if (request.containsKey("is_publish")) {
  return bool(request, "is_publish") ? articleRepository.publish(id) : articleRepository.unpublish(id);
}
return updated;
```

#### TDD 验证

先把 `Phase3ContentApiTest` 改为通过真实编辑页保存路径发布文章：

```java
ResponseEntity<Map> publishByUpdateResponse =
    restTemplate.exchange(
        "/api/v1/admin/articles/" + articleId,
        HttpMethod.PUT,
        new HttpEntity<>(Map.of("is_publish", true), headers),
        Map.class);
assertThat(publishByUpdateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(((Map<?, ?>) data(publishByUpdateResponse)).get("is_publish")).isEqualTo(true);
```

修复前该测试失败：

```text
expected: true
 but was: false
```

修复后 targeted test 与完整后端测试通过。

Docker 中也验证了创建文章后通过 `PUT /api/v1/admin/articles/{id}` 保存 `is_publish=true`，公开文章接口返回 200。

### 问题 3：首页站长头像和 about 个人照片仍为空白/加载失败

#### 现象

第二次人工复验发现：

- `http://localhost:3000/` 首页站长头像位置为空白。
- `http://localhost:3000/about` 关于页个人照片加载失败。
- 文章图片已经正常显示，说明 `/uploads/**` 代理本身已经可用。

#### 原因

首页站长头像和 about 个人照片不是普通 Markdown HTML 图片，而是通过 Nuxt Image 渲染：

```vue
<NuxtImg :src="avatarUrl" alt="头像" loading="lazy" />
<NuxtImg :src="info.photo" alt="个人照片" loading="lazy" />
```

当图片路径是 `/uploads/...` 时，Nuxt Image 会把它改写为：

```text
/_ipx/_/uploads/<filename>
```

这绕过了我们修复的 `/uploads/**` 代理路径，因此不是图片格式或尺寸问题，而是图片组件改写路径导致的问题。

#### 修复方式

对这些已由后端负责提供的本地上传图片，改用原生 `img`，避免 Nuxt Image/IPX 改写：

`blog/app/components/layouts/sidebar/AuthorCard.vue`：

```vue
<img :src="avatarUrl" alt="头像" loading="lazy" />
```

`blog/app/pages/about.vue`：

```vue
<img :src="info.photo" alt="个人照片" loading="lazy" />
```

#### 验证

Docker 重建后检查页面 HTML：

```html
<img src="/uploads/...png" alt="头像" loading="lazy">
<img src="/uploads/...png" alt="个人照片" loading="lazy">
```

不再出现：

```text
/_ipx/_/uploads/...
```

并验证真实图片文件在三端都返回 200：

```text
localhost:3000 -> 200
localhost:4000 -> 200
localhost:8080 -> 200
```

人工复验确认已修好。

### 问题 4：文章 ZIP 导出后，本地打开 Markdown 图片仍不显示

#### 现象

文章页面和文章编辑页中的图片已经正常显示，但导出 ZIP 后，在本地打开 Markdown 仍然显示失败。导出的 Markdown 中图片路径类似：

```md
![图片](/uploads/1778864547709_2bec41cda9752047bbca0ecfcd222095.png)
```

#### 原因

`/uploads/...` 是站点运行时路径，本地文件系统直接打开 Markdown 时没有这个站点 origin，也没有对应图片文件。此前 ZIP 里只有 `.md` 文件，没有把图片一起打包，因此本地打开并不是真正可用。

#### 修复方式

将 Markdown ZIP 导出改为“Markdown + assets 图片目录”的闭环：

- 扫描 Markdown 中的 `/uploads/<filename>` 图片。
- 从后端上传目录读取真实文件。
- 写入 ZIP 的 `assets/<filename>`。
- 把 Markdown 链接改写为 `assets/<filename>`。

核心代码在 `ArticleService.downloadMarkdownZip(...)`：

```java
private String markdownForZip(String markdown, ZipOutputStream zip) throws IOException {
  Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(markdown == null ? "" : markdown);
  StringBuilder rewritten = new StringBuilder();
  while (matcher.find()) {
    String url = matcher.group(1).trim();
    if (url.startsWith("/uploads/")) {
      String filename = url.substring("/uploads/".length());
      Path asset = uploadRoot.resolve(filename).normalize();
      if (asset.startsWith(uploadRoot) && Files.exists(asset)) {
        String entryName = "assets/" + filename;
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(asset, zip);
        zip.closeEntry();
        matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group().replace(url, entryName)));
      }
    }
  }
  matcher.appendTail(rewritten);
  return rewritten.toString();
}
```

#### TDD 验证

先将 Batch 2 测试增强为要求 ZIP 同时包含改写后的 Markdown 和图片字节：

```java
assertThat(zipContents)
    .anySatisfy(
        entry -> {
          assertThat(entry.name()).endsWith(".md");
          assertThat(new String(entry.bytes(), StandardCharsets.UTF_8))
              .contains("![Imported Image](assets/" + imageUrl.substring("/uploads/".length()) + ")");
        });
assertThat(zipContents)
    .anySatisfy(
        entry -> {
          assertThat(entry.name()).isEqualTo("assets/" + imageUrl.substring("/uploads/".length()));
          assertThat(new String(entry.bytes(), StandardCharsets.UTF_8)).isEqualTo("imported image");
        });
```

修复前 RED 失败：

```text
Expecting actual:
  "# Batch2 Image Import

![Imported Image](/uploads/...)"
to contain:
  "![Imported Image](assets/...)"
```

修复后 targeted test、Batch 2 全类测试和完整后端测试均通过。

## Batch 2 其他闭环修复摘要

### 上传返回契约补齐

上传接口返回 `original_name`，方便前端保持一致契约：

```java
return Map.of(
    "id", id,
    "file_url", url,
    "file_name", filename,
    "original_name", original,
    "file_size", file.getSize());
```

### 文件删除语义补齐

删除文件时不只软删数据库记录，也删除物理文件，避免列表没了但 URL 仍可访问：

```java
Path target = uploadRoot.resolve(filenames.getFirst()).normalize();
if (target.startsWith(uploadRoot)) {
  Files.deleteIfExists(target);
}
```

并为静态资源不存在补 404 处理，避免被全局异常处理成 500。

### 公共上传类型收敛

公共 `/api/v1/upload` 只接受前端真实使用的类型，避免未知类型被静默改成其他类型造成假成功：

```java
private String publicUploadType(String type) {
  return switch (type) {
    case "用户头像", "评论贴图", "反馈投诉" -> type;
    default -> throw new BusinessException(40001, "不支持的上传类型", HttpStatus.BAD_REQUEST);
  };
}
```

### Markdown 导入图片策略明确

当前支持已经上传到本站的 `/uploads/**` 图片引用。导入时会验证文件真实存在：

```java
private boolean isSupportedMarkdownImage(String url) {
  if (url.startsWith("/uploads/")) {
    String filename = url.substring("/uploads/".length());
    Path asset = uploadRoot.resolve(filename).normalize();
    return asset.startsWith(uploadRoot) && Files.exists(asset);
  }
  return false;
}
```

远程图片、本地相对图片、HTML `<img>` 重写、ZIP 内媒体批量导入仍然是 deferred；不支持的情况必须明确失败或报告，不能假装导入成功。

### 评论导入目标绑定收紧

评论导入不再把缺少目标的评论默认绑定到 `hello-zblog`，避免假成功：

```java
String targetKey = firstText(comment, "target_key", "page_key", "path", "url");
if (targetKey.isBlank()) {
  throw new IllegalArgumentException("target_key is required");
}
```

## 验证命令汇总

本轮关键验证包括：

```powershell
mvn -f server/pom.xml -Dtest=Batch2ContentAssetBatchTest test
mvn -f server/pom.xml -Dtest=P2ImportExportApiTest test
mvn -f server/pom.xml -Dtest=Phase4InteractionApiTest test
mvn -f server/pom.xml -Dtest=Phase3ContentApiTest#adminCanCreateUpdateAndPublishArticleWithTaxonomy test
mvn -f server/pom.xml test
npm --prefix admin run type-check
npm --prefix blog run type-check
npm --prefix admin run build
npm --prefix blog run build
docker compose up --build -d server admin blog
```

其中 `npm --prefix blog run type-check` 仍会出现既有的 `vue-router/volar/sfc-route-blocks` / `@vue/language-core` warning，但命令完成；这不是本轮修改引入的新失败。

## 当前仍 deferred 的事项

- 反馈通知点击进入独立工单详情页。
- 设置头像更新自动生成动态。
- 远程图片导入并下载到本地媒体库。
- 本地相对图片或 ZIP 媒体批量导入。
- HTML `<img>` 导入重写。
- OAuth 真实 provider 回调与密码重置邮件链路。

## 结论

Batch 1 和 Batch 2 的人工复验问题体现出同一个核心经验：接口存在、返回成功、甚至页面某一处可用，都不等于真实闭环完成。必须验证真实入口、真实 origin、真实文件、真实数据库状态、刷新/导出/本地打开后的行为。

本轮修复后，上传资源已经在后端、admin、blog 三个 origin 下闭环；文章发布路径与真实编辑页保存路径闭环；站长头像与 about 个人照绕过了错误的 IPX 改写；Markdown ZIP 导出也从“只有 Markdown 链接”补齐为“Markdown + assets 文件”的本地可用包。
