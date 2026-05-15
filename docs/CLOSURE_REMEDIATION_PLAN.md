# CLOSURE_REMEDIATION_PLAN.md

> 本文职责：把交付审计后的修复计划落成可执行清单，避免继续依赖口头计划或 phase 名称判断完成。

## 当前状态

已经完成：

- 交付审计文档：`docs/DELIVERY_AUDIT.md`；
- 事故复盘文档：`docs/RETRO_ACCEPTANCE_LOOP.md`；
- 仓库规则更新：`CLAUDE.md`、`Agents.md`；
- 乱码文档修复：项目背景、产品计划、API 计划、路线图、FlecBlog 参考、Phase 日志等文档已恢复为正常中文。

下一步工作必须按“真实页面 + 真实 API + 真实数据 + 刷新后仍正确 + 验证记录”来判定完成。

## 总原则

每个功能都按下面闭环判断：

```text
真实页面
  -> 真实 API
  -> 后端真实实现
  -> 数据真实持久化或真实派生
  -> 刷新/重开仍然正确
  -> 有测试或明确验证记录
```

不能再按下面这些信号判断完成：

```text
有 Controller
有接口名
有页面
type-check 通过
phase 文档写了完成
```

## 第一阶段：接口缺口清单化

目标：先把“前端到底调用了什么”列清楚，再动代码。

### 1. 扫描前台 `blog/` API 调用

重点目录：

```text
blog/app/composables/api/
blog/app/pages/
blog/app/components/
blog/app/utils/request.ts
```

输出字段：

```text
页面 / 组件
调用的 API
后端是否存在
是否真实实现
是否是 stub
是否需要数据库表
优先级
```

重点模块：

- 用户登录 / 注册 / 刷新；
- 个人资料；
- 密码修改 / 设置；
- OAuth 绑定 / 解绑；
- 评论；
- 订阅；
- 反馈；
- 文件上传；
- moments；
- 通知；
- 搜索；
- 统计。

### 2. 扫描后台 `admin/` API 调用

重点目录：

```text
admin/src/api/
admin/src/views/
admin/src/utils/request.ts
```

重点模块：

- 用户管理；
- 文章导入导出；
- 评论导入；
- 反馈管理；
- 订阅者管理；
- RSS feed 管理；
- 工具页；
- AI 配置；
- 系统信息；
- 通知；
- 访问统计。

输出分类：

```text
已闭环
部分实现
后端缺失
后端 stub
前端页面暂不可用
```

### 3. 扫描后端 stub / 空实现 / 假数据

重点模式：

```text
ok(null)
List.of()
Map.of()
return 0
emptyList
TODO
placeholder
hardcoded
```

重点目录：

```text
server/src/main/java/com/zblog/
```

目标是找出：

- 表面有接口，实际返回空；
- 统计类数据硬编码；
- admin 页面能打开但数据不真实；
- controller 有路由但 service 没业务；
- service 有方法但 repository 没持久化。

## 第二阶段：按优先级补闭环

### P0：登录 / 用户 / 账户闭环

优先原因：

- 它影响后台管理系统基础能力；
- 前台 `profile.vue` 已经调用大量用户相关接口；
- 后台 `UserList.vue` 已经有页面和 API 调用；
- 如果这块不闭环，后续反馈、通知、评论用户身份都会继续悬空。

涉及前端：

```text
blog/app/composables/api/user.ts
blog/app/pages/profile.vue
admin/src/api/user.ts
admin/src/views/user/
admin/src/utils/auth.ts
admin/src/utils/request.ts
```

需要确认 / 补齐的后端接口：

```text
POST   /api/v1/auth/login
POST   /api/v1/auth/register
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout

GET    /api/v1/user/profile
PATCH  /api/v1/user/profile
PUT    /api/v1/user/password
POST   /api/v1/user/password
DELETE /api/v1/user/deactivate
DELETE /api/v1/user/oauth/{provider}

GET    /api/v1/admin/users
GET    /api/v1/admin/users/{id}
POST   /api/v1/admin/users
PUT    /api/v1/admin/users/{id}
DELETE /api/v1/admin/users/{id}
PUT    /api/v1/admin/users/{id}/password
```

验收标准：

- 后台登录成功；
- refresh 能续 token；
- logout 能正常退出；
- 当前用户信息能读取；
- 用户资料能修改并刷新后仍保留；
- 后台用户列表能显示真实用户；
- 创建、编辑、重置密码、删除用户有真实数据效果；
- 测试不只验证 `code=0`，还要验证数据库结果。

### P1：反馈 / 订阅 / RSS feed 闭环

可能涉及接口：

```text
POST   /api/v1/feedback
GET    /api/v1/admin/feedback
PUT    /api/v1/admin/feedback/{id}
DELETE /api/v1/admin/feedback/{id}

POST   /api/v1/subscribers
GET    /api/v1/admin/subscribers
DELETE /api/v1/admin/subscribers/{id}

GET    /api/v1/rssfeed
GET    /api/v1/admin/rssfeed
POST   /api/v1/admin/rssfeed
PUT    /api/v1/admin/rssfeed/{id}
DELETE /api/v1/admin/rssfeed/{id}
```

验收标准：

- 前台提交反馈后，后台能看到；
- 前台订阅后，后台订阅者列表能看到；
- 删除 / 更新后刷新仍正确；
- 不是只返回 success。

### P2：文章导入 / 导出 / 评论导入

可能涉及接口：

```text
POST /api/v1/admin/articles/import
GET  /api/v1/admin/articles/export
GET  /api/v1/admin/articles/{id}/wechat-export
GET  /api/v1/admin/articles/{id}/zip
POST /api/v1/admin/comments/import
```

验收标准：

- 导入 Markdown 后生成真实文章；
- 文章内容仍以 `content_markdown` 为真源；
- 导出内容可下载；
- 微信公众号导出不是空 HTML；
- 导入失败有错误列表，而不是吞掉。

### P3：访问统计 / 通知 / 系统信息

重点接口：

```text
/admin/stats/**
/admin/visits/**
/admin/notifications/**
/admin/system/**
/stats/**
```

验收标准：

- 统计值来自真实表或真实事件；
- 文章阅读量刷新后能增长；
- admin dashboard 数字不是硬编码；
- 通知列表不是永远空；
- 系统信息能反映当前运行环境。

### P4：上传路径一致性

需要确认前端拿到的 `file_url` 在这些场景中都可访问：

```text
blog 前台
admin 后台
server 静态资源
docker compose 环境
```

验收标准：

- 后台上传图片；
- 文章中引用图片；
- 前台详情页能正常显示；
- docker 重启后仍可访问；
- 路径不是本地临时路径。

### P5：FlecBlog 对齐差异复查

最后对照：

```text
_reference/FlecBlog/blog
_reference/FlecBlog/admin
```

确认：

- 用户看得到的入口是否可用；
- 后台显示的菜单是否有真实业务；
- 不做的功能是否明确隐藏；
- 暂缓功能是否写进文档，不冒充完成。

## 实际执行节奏

每次只处理一个闭环：

```text
1. 列出这个闭环的前端入口
2. 列出调用 API
3. 查后端实现状态
4. 确认数据库表
5. 补后端
6. 补前端契约
7. 补测试
8. 本地跑验证
9. 更新审计文档
```

不要一口气乱修多个模块，否则会再次回到“看起来都动了，但不知道哪个真的闭环”的状态。

## 下一步

先做 **P0：用户 / 账户 / 后台用户管理闭环**。

开始写代码前，先输出 P0 接口对照表：

```text
前端文件
调用方法
请求路径
后端是否存在
当前问题
需要新增/修改的后端类
需要新增/修改的表
验收方式
```
