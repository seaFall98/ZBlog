# FRONTEND_MIGRATION_LOG.md

> 本文职责：记录 FlecBlog 前端源码迁入 ZBlog2 的基线状态、来源路径、当前未修改内容和后续适配步骤。

## 迁入时间

2026-05-14

## 来源

```text
D:\MyCode\ZBlogProject\ZBlog2\_reference\FlecBlog\blog
D:\MyCode\ZBlogProject\ZBlog2\_reference\FlecBlog\admin
```

## 目标

```text
D:\MyCode\ZBlogProject\ZBlog2\blog
D:\MyCode\ZBlogProject\ZBlog2\admin
```

## 当前状态

本次迁入保持 FlecBlog 前端源码原貌，不做功能改造、不做品牌替换、不做 Java API 适配。

这样做的原因：

- 先保留可运行基线；
- 后续每个改动都有清晰 diff；
- 避免第一步就把源码改乱；
- 符合“可复用就先用，再局部修改”的策略。

## 后续适配顺序

1. 验证 `blog` 和 `admin` 依赖安装、类型检查和构建；
2. 清理品牌信息和默认站点文案；
3. 隐藏当前博客主线不需要的入口；
4. 设计 Java API adapter；
5. 用 Java 后端替换 FlecBlog Go API；
6. 迁移旧 ZBlog 中更强的 Markdown/文章详情局部能力；
7. 浏览器截图验收主要页面。

## 当前不做

- 不重设计首页；
- 不把项目改成个人品牌官网；
- 不接入 DevWiki Studio；
- 不处理相册、说说、一键安装器；
- 不修改 `_reference` 目录。

## 验证记录

2026-05-14：

- `admin`: `npm install` 成功。
- `admin`: `npm run type-check` 成功。
- `admin`: `npm run build` 成功。
- `blog`: 第一次 `npm install` 失败，`package-lock.json` 中的 resolved 地址指向腾讯 npm 镜像，下载 `whatwg-url` 返回 `E567 Unknown Status`。
- `blog`: 使用 `--registry=https://registry.npmjs.org/` 仍读取 lock 中腾讯镜像地址，下载 `serialize-javascript` 返回 `E567 Unknown Status`。
- `blog`: 使用 `--package-lock=false --registry=https://registry.npmjs.org/` 未立即报错，但超过 5 分钟仍未生成 `node_modules`，残留 npm 进程已停止。
- `blog`: 将 `blog/package-lock.json` 中 `https://mirrors.cloud.tencent.com/npm/` 批量替换为 `https://registry.npmjs.org/` 后，`npm install --registry=https://registry.npmjs.org/` 成功。
- `blog`: `npm run type-check` 成功，但输出 Vue/Volar 插件解析警告：`vue-router/volar/sfc-route-blocks` 找不到 `@vue/language-core`，命令退出码仍为 0。
- `blog`: `npm run build` 成功，输出 Nuxt sourcemap、Node deprecated exports pattern、sharp win32-x64 架构等警告，命令退出码为 0。

后续处理：

- 检查是否需要给 `blog` 显式加入 `vue-tsc` 以消除 type-check 期间的 npx/Volar 警告；
- 后续部署到 Linux 服务器时，注意 Nuxt build 提示的 `sharp` 架构信息，应在目标架构上重新构建镜像；
- 当前可声明的范围仅限：FlecBlog 前端源码已迁入，`admin` 和 `blog` 均完成本地依赖安装、类型检查和生产构建。
