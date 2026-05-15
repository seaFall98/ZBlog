# FRONTEND_MIGRATION_LOG.md

> 本文职责：记录 FlecBlog 前端源码迁入 ZBlog 的基础状态、来源路径、当前未改动内容和后续适配步骤。

## 迁入时间

2026-05-14

## 来源

```text
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog\blog
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog\admin
```

## 目标

```text
D:\MyCode\ZBlogProject\ZBlog\blog
D:\MyCode\ZBlogProject\ZBlog\admin
```

## 当前状态

本次迁入保持 FlecBlog 前端源码原貌，不做功能改造、不做品牌替换、不做 Java API 适配。

这样做的原因：

- 先保留可运行基线；
- 后续每个改动都有清晰 diff；
- 避免第一步就把源码改乱；
- 符合“能复用先复用，再局部修改”的策略。

## 后续适配顺序

1. 验证 `blog` 和 `admin` 的依赖安装、类型检查和构建；
2. 清理品牌信息和默认文案；
3. 隐藏当前主线不需要的入口；
4. 设计 Java API adapter；
5. 用 Java 后端替换 FlecBlog Go API；
6. 迁移 ZBlog 更强的 Markdown / 文章详情局部能力；
7. 浏览器截图验收主要页面。

## 当前不做

- 不重设计首页；
- 不把项目改成个人品牌官网；
- 不接入 DevWiki Studio；
- 不处理相册、说说、一键安装器；
- 不修改 `_reference` 目录。

## 验证记录

2026-05-14：

- `admin`: `npm install` 成功；
- `admin`: `npm run type-check` 成功；
- `admin`: `npm run build` 成功；
- `blog`: 先前安装过程因镜像源问题失败，后续已切换回官方源处理；
- `blog`: `npm run type-check` 成功，但存在 Vue / Volar 插件提示；
- `blog`: `npm run build` 成功，但存在 Nuxt 和 sharp 的环境提示。

## 后续处理

- 如有需要，给 `blog` 显式加入 `vue-tsc` 来减少 type-check 阶段提示；
- 部署到 Linux 时注意 `sharp` 的构建信息，并按目标架构重新构建镜像；
- 当前可确认的范围仅限：FlecBlog 前端源码已迁入，`admin` 和 `blog` 已完成本地依赖安装、类型检查和构建。
