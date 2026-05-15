# FRONTEND_PLAN.md

> 本文职责：定义前端迁移、复用、局部修改和验收策略。

## 总原则

FlecBlog 前端源码可复用就先用，再局部修改。

不要为了“重写得更像自己的”而放弃已经完整的 Nuxt 博客端和 Vue 管理端。

## 目标结构

```text
blog/      Nuxt 4 博客前台，来源 FlecBlog blog
admin/     Vue 3 + Vite 管理后台，来源 FlecBlog admin
```

## Blog 端

迁入基线：

```text
_reference\FlecBlog\blog
```

保留：

- Nuxt 4 SSR；
- SEO、sitemap、feed、PWA 相关配置；
- 首页、文章、分类、标签、归档、关于、友链、评论；
- layout、navbar、sidebar、footer；
- Markdown 渲染、代码高亮、Mermaid、图片缩放；
- 搜索弹窗和移动端基础体验。

修改：

- API base URL 适配 Java 后端；
- 类型字段适配 Java API；
- 站点名、Logo、作者信息替换；
- 暂时隐藏 ask、moment、notifications、subscribe 等后续功能入口；
- 删除与 FlecBlog 个人内容强绑定的文案。

验收：

```powershell
cd blog
npm install
npm run type-check
npm run build
```

## Admin 端

迁入基线：

```text
_reference\FlecBlog\admin
```

保留：

- Vite + Vue 3 + Element Plus；
- 后台布局；
- 仪表盘；
- 文章列表和文章表单；
- CodeMirror Markdown 编辑器；
- 分类、标签、评论、友链、文件、设置、统计；
- API/type 分模块结构。

修改：

- API base URL 适配 Java 后端；
- 登录认证、token 存储、权限判断适配 Java；
- 清理 FlecBlog 品牌和非当前主线模块；
- 首期隐藏 AI、moment、RSS、反馈等功能。

验收：

```powershell
cd admin
npm install
npm run type-check
npm run build
```

## UI 策略

当前阶段不做大规模视觉重设计。先保留 FlecBlog 干净、完整、耐看的博客气质，完成真实博客闭环。后续再根据个人品牌方向做设计系统升级。

