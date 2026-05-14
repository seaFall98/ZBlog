# AGENTS.md

> 本文职责：定义 Codex 在 ZBlog2 中的工作规则、必读文档、禁止事项、验证要求和阶段推进方式。

## Prime Rule

任何任务完成声明必须有当前会话的新鲜验证证据。没有运行验证命令，就不能说已经完成或通过。

## Required Reading

每次 ZBlog2 任务开始前，先阅读：

```text
README.md
AGENTS.md
docs/0_PROJECT_CONTEXT.md
docs/REFERENCE_FLECBLOG.md
docs/REUSE_ASSET_AUDIT.md
docs/PRODUCT_PLAN.md
docs/FEATURE_LIST.md
docs/FRONTEND_PLAN.md
docs/BACKEND_PLAN.md
docs/API_PLAN.md
docs/DATA_MODEL_PLAN.md
docs/TECH_STACK_PLAN.md
docs/IMPLEMENTATION_ROADMAP.md
docs/CODEX_WORKFLOW.md
docs/FRONTEND_MIGRATION_LOG.md
```

如果任务涉及前端迁移，还必须先检查：

```text
D:\MyCode\ZBlogProject\ZBlog2\_reference\FlecBlog\blog
D:\MyCode\ZBlogProject\ZBlog2\_reference\FlecBlog\admin
```

如果任务涉及 Markdown、文章详情或 Java 后端文章模型，还必须检查：

```text
D:\MyCode\ZBlogProject\ZBlog
```

## Hard Prohibitions

- 不要把当前主线改成个人品牌官网、项目集或 DevWiki Studio。
- 不要把项目降级成简单 CRUD Demo。
- 不要把 FlecBlog 后端 Go 代码机械翻译成 Java。
- 不要在没有迁移任务的情况下直接大规模复制源码。
- 不要丢弃 FlecBlog 前端可复用结构后重新发明页面。
- 不要盲目照搬旧 ZBlog/Klee 的视觉和路线摇摆。
- 不要把 `_reference/` 里的源码当作一方业务代码直接修改。
- 不要提交密钥、服务器密码、域名证书、真实 `.env`。

## Feat Shape

后续每个阶段任务按此格式推进：

```text
feat/<name>
Goal:
Scope:
Inputs:
Implementation steps:
Verification commands:
Done definition:
Status:
```

`Done` 必须满足：

- 实现完成；
- 验证命令运行通过；
- 文档同步更新；
- 遗留问题明确记录；
- 没有混入无关改动。

## Verification Rules

文档任务：

```powershell
Test-Path docs
Get-ChildItem docs -File
Select-String -Path docs\*.md -Pattern "本文职责"
git status --short
```

前端任务：

```powershell
npm install
npm run type-check
npm run build
```

后端任务：

```powershell
mvn test
mvn package
```

部署任务：

```powershell
docker compose config
```
