# P0_USER_ACCOUNT_API_MATRIX.md

> 本文职责：记录 P0 用户 / 账户 / 后台用户管理闭环的前后端接口对照，作为本轮代码修复的执行清单。

## 结论

P0 基础用户闭环已完成：后端已引入真实 `users` 表、密码哈希、当前用户资料维护、后台用户 CRUD，以及对应测试。真 OAuth 授权回调、邮箱验证码和找回密码仍然是后续项。

## 前台个人中心接口

| 前端文件 | 调用方法 | 请求路径 | 当前后端状态 | 当前问题 | 处理方式 | 验收方式 |
| --- | --- | --- | --- | --- | --- | --- |
| `blog/app/composables/api/user.ts` | `login` | `POST /api/v1/auth/login` | 部分存在 | 只校验 bootstrap 配置，不读用户表 | 改为优先数据库用户登录，保留 bootstrap seed 用户 | 登录后拿 token 访问 profile |
| `blog/app/composables/api/user.ts` | `register` | `POST /api/v1/auth/register` | 缺失 | 前台可调用但后端无路由 | 新增注册接口并写入用户表 | 注册后返回 token 和用户资料 |
| `blog/app/composables/api/user.ts` | `refreshToken` | `POST /api/v1/auth/refresh` | 存在 | 返回结构可用，但依赖 token subject | 保留并返回真实用户资料 | refresh 后 token 可继续访问 profile |
| `blog/app/composables/api/user.ts` | `getUserProfile` | `GET /api/v1/user/profile` | stub | 返回硬编码资料 | 改为按 token subject 查询用户表 | 返回真实 nickname/email/role |
| `blog/app/composables/api/user.ts` | `updateUserProfile` | `PATCH /api/v1/user/profile` | 缺失 | 个人资料无法保存 | 新增资料更新接口 | 修改昵称后重读仍保留 |
| `blog/app/composables/api/user.ts` | `changePassword` | `PUT /api/v1/user/password` | 缺失 | 修改密码无法闭环 | 新增旧密码校验和更新 | 旧密码失效，新密码可登录 |
| `blog/app/composables/api/user.ts` | `setPassword` | `POST /api/v1/user/password` | 缺失 | OAuth 用户首次设密码无后端 | 新增无密码用户设置密码接口 | 设置后 `has_password=true` |
| `blog/app/composables/api/user.ts` | `deactivateAccount` | `DELETE /api/v1/user/deactivate` | 缺失 | 注销账户无法闭环 | 新增软删除接口 | 删除后登录失败 |
| `blog/app/composables/api/user.ts` | `unbindOAuth` | `DELETE /api/v1/user/oauth/{provider}` | 缺失 | OAuth 解绑无后端 | 新增字段清空接口 | 解绑后 `linked_oauths` 不包含 provider |

## 后台用户管理接口

| 前端文件 | 调用方法 | 请求路径 | 当前后端状态 | 当前问题 | 处理方式 | 验收方式 |
| --- | --- | --- | --- | --- | --- | --- |
| `admin/src/api/user.ts` | `getProfile` | `GET /api/v1/user/profile` | stub | 角色和用户信息硬编码 | 改为数据库用户 | 后台能识别当前角色 |
| `admin/src/api/user.ts` | `getUsers` | `GET /api/v1/admin/users` | 缺失 | 用户列表页面不可闭环 | 新增分页、关键词、角色、状态筛选 | 列表返回真实用户和 total |
| `admin/src/api/user.ts` | `getUserById` | `GET /api/v1/admin/users/{id}` | 缺失 | 编辑详情无后端 | 新增详情接口 | 返回指定用户 |
| `admin/src/api/user.ts` | `createUser` | `POST /api/v1/admin/users` | 缺失 | 新增用户不可用 | 新增后台创建用户 | 创建后列表可见 |
| `admin/src/api/user.ts` | `updateUser` | `PUT /api/v1/admin/users/{id}` | 缺失 | 编辑用户不可用 | 新增后台更新用户 | 更新后刷新仍正确 |
| `admin/src/api/user.ts` | `deleteUser` | `DELETE /api/v1/admin/users/{id}` | 缺失 | 删除用户不可用 | 新增软删除 | 删除后默认列表不可见 |
| `admin/src/api/user.ts` | `resetUserPassword` | `PUT /api/v1/admin/users/{id}/password` | 缺失 | 重置密码不可用 | 新增密码重置接口 | 新密码可登录 |

## 需要新增 / 修改的后端文件

```text
server/src/main/resources/db/migration/V6__phase7_users.sql
server/src/main/java/com/zblog/identity/UserService.java
server/src/main/java/com/zblog/identity/UserController.java
server/src/main/java/com/zblog/identity/AdminUserController.java
server/src/main/java/com/zblog/identity/AuthController.java
server/src/main/java/com/zblog/identity/CurrentUserController.java
server/src/test/java/com/zblog/server/UserAccountApiTest.java
```

## 数据表方向

```text
users
- id
- email
- password_hash
- nickname
- avatar
- badge
- website
- role
- is_enabled
- deleted_at
- last_login
- github_id / google_id / qq_id / microsoft_id / feishu_open_id
- created_at / updated_at
```

## 本轮不做

- 真 OAuth 授权回调；
- 邮箱验证码；
- 找回密码邮件发送；
- 权限系统细分到 RBAC 菜单级别。

这些不阻塞 P0 基础用户闭环，但不能冒充已完成。
