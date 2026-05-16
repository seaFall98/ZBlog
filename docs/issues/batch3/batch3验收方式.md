> 进行下一步建议是启动本地栈做人工验收：反馈/回复、订阅/退订、忘记/重置密码、OAuth deferred UI。告诉我怎么启动本地栈，需要验收哪些结果

● 可以这样启动本地栈：

  docker compose up --build -d
  docker compose ps
  docker compose logs -f server

  看到 server 日志里 Spring Boot 启动完成后，再打开：

  - 前台博客：http://localhost:3000
  - 后台管理：http://localhost:4000
  - 后端 API：http://localhost:8080

  后台默认账号一般是：

  admin
  admin123456

  如果你想只重启这次相关服务，也可以用：

  docker compose up --build -d server admin blog

  但第一次验收建议直接跑完整：

  docker compose up --build -d

---
  Batch 3 人工验收清单

  1. 反馈提交后产生站长邮件记录

  操作：

  1. 打开前台博客。
  2. 找到反馈/留言/投诉入口。
  3. 提交一条反馈，邮箱可以填类似：

  feedback-manual@example.com

  4. 内容里写一个方便识别的文本，例如：

  Batch3 manual feedback check

  预期结果：

  - 页面提示反馈提交成功。
  - 后端 feedbacks 表有反馈记录。
  - mail_outbox 表新增一条站长邮件记录：
    - audience = admin
    - mail_type = feedback_new
    - recipient 是站长邮箱
    - status = sent
    - subject/body 里包含反馈单号或反馈内容。

  可以查：

  docker compose exec postgres psql -U zblog -d zblog -c "select id,audience,mail_type,recipient,subject,status,created_at from mail_outbox order by id desc limit 10;"

  如果你的数据库服务名不是 postgres，先看：

  docker compose ps

  然后把命令里的 postgres 换成实际服务名。

---
  2. 后台回复反馈后产生用户邮件记录

  操作：

  1. 打开后台：http://localhost:4000
  2. 登录后台。
  3. 进入反馈管理。
  4. 找到刚才提交的反馈。
  5. 修改状态为已处理/已解决，并填写管理员回复，例如：

  Batch3 manual admin reply

  预期结果：

  - 后台保存成功。
  - mail_outbox 新增一条用户回复邮件记录：
    - audience = user
    - mail_type = feedback_reply
    - recipient = feedback-manual@example.com
    - status = sent
    - body 包含管理员回复内容。

  查询：

  docker compose exec postgres psql -U zblog -d zblog -c "select id,audience,mail_type,recipient,subject,body,status,created_at from mail_outbox order by id desc limit 10;"

---
  3. 订阅后产生订阅确认邮件记录

  操作：

  1. 打开前台博客。
  2. 找到订阅入口。
  3. 输入邮箱，例如：

  subscriber-manual@example.com

  4. 提交订阅。

  预期结果：

  - 页面提示订阅成功。
  - subscribers 表新增或恢复该邮箱。
  - mail_outbox 新增订阅确认邮件：
    - audience = user
    - mail_type = subscribe_confirm
    - recipient = subscriber-manual@example.com
    - status = sent
    - body 中包含 unsubscribe token。

  查询订阅者：

  docker compose exec postgres psql -U zblog -d zblog -c "select id,email,unsubscribe_token,active,created_at from subscribers where email='subscriber-manual@example.com';"

  查询邮件：

  docker compose exec postgres psql -U zblog -d zblog -c "select id,audience,mail_type,recipient,subject,body,status,created_at from mail_outbox where
  recipient='subscriber-manual@example.com' order by id desc;"

---
  4. 退订后产生退订确认邮件记录

  操作：

  1. 从上一步 subscribers.unsubscribe_token 拿到 token。
  2. 浏览器打开：

  http://localhost:8080/api/v1/subscribe/unsubscribe?token=你的token

  或者如果前端有退订入口，也可以走前端。

  预期结果：

  - 返回成功。
  - subscribers.active = false
  - mail_outbox 新增退订确认邮件：
    - mail_type = unsubscribe_confirm
    - recipient = subscriber-manual@example.com
    - status = sent
    - 内容包含“已退订”。

  查询：

  docker compose exec postgres psql -U zblog -d zblog -c "select email,active,updated_at from subscribers where email='subscriber-manual@example.com';"

  docker compose exec postgres psql -U zblog -d zblog -c "select id,mail_type,recipient,subject,body,status,created_at from mail_outbox where
  recipient='subscriber-manual@example.com' order by id desc;"

---
  5. 忘记密码 / 重置密码闭环

  建议用一个新注册账号验收。

  操作：

  1. 前台打开登录弹窗。
  2. 点击注册。
  3. 注册账号，例如：

  reset-manual@example.com
  旧密码：oldPass123

  4. 退出或重新打开登录弹窗。
  5. 点击“忘记密码”。
  6. 输入：

  reset-manual@example.com

  7. 点击发送验证码。

  预期结果：

  - 页面提示验证码已发送。
  - password_reset_tokens 表新增未使用 token。
  - mail_outbox 新增：
    - mail_type = password_reset
    - recipient = reset-manual@example.com
    - body 包含 token。

  查询 token：

  docker compose exec postgres psql -U zblog -d zblog -c "select email,token,expires_at,used_at,created_at from password_reset_tokens where email='reset-manual@example.com' order
  by id desc limit 3;"

  8. 复制最新 token，填入前台忘记密码表单的验证码输入框。
  9. 设置新密码，例如：

  newPass123

  10. 提交重置。

  预期结果：

  - 页面提示密码重置成功。
  - password_reset_tokens.used_at 不再为空。
  - 旧密码 oldPass123 登录失败。
  - 新密码 newPass123 登录成功。
  - 再次用同一个 token 重置密码应失败。

---
  6. OAuth deferred / unsupported 验收

  当前没有真实第三方 OAuth 凭据，所以不能假装可用。

  需要确认两点：

  A. 前台 UI 不展示可用第三方登录按钮
  需要确认两点：

  当前没有真实第三方 OAuth 凭据，所以不能假装可用。

  需要确认两点：

  A. 前台 UI 不展示可用第三方登录按钮
  需要确认两点：

  A. 前台 UI 不展示可用第三方登录按钮

  操作：

  1. 打开前台登录弹窗。
  2. 查看登录表单下方是否有 GitHub / Google / QQ / Microsoft 登录按钮。

  操作：

  1. 打开前台登录弹窗。
  2. 查看登录表单下方是否有 GitHub / Google / QQ / Microsoft 登录按钮。

  预期结果：

  - 默认配置下不应该展示第三方登录按钮。
  - 如果你手动开启了 OAuth 配置，也不应该表现成完整可用的真实登录。

  B. 后端明确返回 unsupported

  可以直接访问：

  http://localhost:8080/api/v1/auth/github?redirect=%2F

  预期结果：

  - HTTP 状态码是 501
  - 返回内容包含类似：

  OAuth github unsupported without provider credentials

  这说明不是假登录、不是 404、不是 401，而是明确 deferred。

---
  最终验收通过标准

  这次 Batch 3 可以接受的结果是：

  1. 反馈提交后，站长邮件记录真实写入 mail_outbox。
  2. 后台回复反馈后，用户邮件记录真实写入 mail_outbox。
  3. 订阅后，订阅确认邮件记录真实写入 mail_outbox。
  4. 退订后，退订确认邮件记录真实写入 mail_outbox。
  5. 忘记密码会产生 reset token 和邮件记录。
  6. 重置密码后旧密码失效，新密码可登录。
  7. 同一个 reset token 不能重复使用。
  8. OAuth 在无凭据时不假装可用，UI 默认隐藏，后端明确返回 501 unsupported。