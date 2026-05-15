# AGENTS.md

> This file defines how Claude Code should work in ZBlog.

## Prime rule

A task is complete only when the current conversation has fresh verification evidence. If no verification command or live UI check was run, do not say the work is done.

## Required reading

Before starting any ZBlog task, read:

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

If the task touches frontend migration, also inspect:

```text
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog\blog
D:\MyCode\ZBlogProject\ZBlog\_reference\FlecBlog\admin
```

If the task touches Markdown articles or Java backend content models, also inspect:

```text
D:\MyCode\ZBlogProject\ZBlog
```

## Hard prohibitions

- Do not treat a phase label, roadmap item, or controller existence as proof of delivery.
- Do not report a feature complete if the live frontend page or admin screen cannot finish the flow against the backend.
- Do not count placeholder arrays, hardcoded totals, empty acknowledgments, or `ok(null)`-style stubs as shippable behavior.
- Do not replace FlecBlog behavior with a new design when the existing reference baseline can be reused.
- Do not mechanically port FlecBlog Go internals into Java.
- Do not drop existing reusable frontend structure just to rebuild the same page from scratch.
- Do not modify `_reference/` source as if it were product code.
- Do not submit secrets, tokens, certificates, or `.env` values.

## Working shape

Use this structure for any non-trivial task:

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

## Done definition

A task may be marked done only when all of these are true:

- the implementation is complete;
- the verification command or live UI check succeeded;
- docs are updated if the behavior changed;
- any remaining issue is explicitly recorded;
- no unrelated changes were introduced.

## Verification rules

Document task:

```powershell
Test-Path docs
Get-ChildItem docs -File
Select-String -Path docs\*.md -Pattern "delivery|retro|acceptance"
git status --short
```

Frontend task:

```powershell
npm install
npm run type-check
npm run build
```

Backend task:

```powershell
mvn test
mvn package
```

Deployment task:

```powershell
docker compose config
```

## Reporting rule

When reporting progress, split the result into:

- closed loops
- partially implemented flows
- missing backend routes
- stubbed behavior
- UI pages that still cannot finish their work

If something is still open, say so directly.