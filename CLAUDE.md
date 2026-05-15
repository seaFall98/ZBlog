# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working in this repository.

## Big picture

ZBlog is a blog-first rebuild:

- `blog/` is the Nuxt 4 public site.
- `admin/` is the Vue 3 + Vite + Element Plus CMS.
- `server/` is the Java 21 + Spring Boot 3 API.
- `docker-compose.yml` wires the local stack together with PostgreSQL.

The current direction is to reuse the FlecBlog frontend baseline where it already fits, while keeping the backend as the Java implementation for this repo.

## Architecture notes

- API surface is organized under `/api/v1`.
- Admin endpoints live under `/api/v1/admin`.
- Responses use the unified `{ code, message, data }` envelope.
- Articles are Markdown-first: `content_markdown` is the source of truth, with `content_html` and `content_text` as derived fields.
- Menus are hierarchical; frontend consumers expect tree-shaped menu data with `children` for navigation, footer, and aggregate menus.
- Backend code is split by domain rather than by technical layer only. The main domains are `identity`, `content`, `taxonomy`, `comment`, `friend`, `media`, `site`, `stats`, `search`, `notification`, and `ops`.
- Flyway migrations seed the local baseline data used by the app at startup.

## Reference sources

- Read `docs/README.md` first, then `docs/EXECUTION_LOCK.md`. The root `docs/` files contain active direction; `docs/archive/` is historical reference only.
- `_reference/FlecBlog/` is the frontend reference baseline. Compare against it before changing `blog/` or `admin/`.
- FlecBlog’s `server/` is Go-based and should be treated as a reference for behavior and contracts, not as code to port mechanically.

## Common commands

### Full local stack

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f server
```

### Backend

Use the system Maven; there is no Maven wrapper in this repo.

```powershell
mvn -f server/pom.xml test
mvn -f server/pom.xml package
```

Run a single test class:

```powershell
mvn -f server/pom.xml -Dtest=UserAccountApiTest test
```

Run a single test method:

```powershell
mvn -f server/pom.xml -Dtest=UserAccountApiTest#profileCanBeUpdatedAndReadBack test
```

### Admin frontend

```powershell
npm --prefix admin run dev
npm --prefix admin run lint
npm --prefix admin run type-check
npm --prefix admin run build
```

### Blog frontend

```powershell
npm --prefix blog run dev
npm --prefix blog run lint
npm --prefix blog run type-check
npm --prefix blog run build
```

## Practical repo-specific reminders

- Continue approved multi-step remediation work proactively instead of waiting to be nudged; at the end of each turn, state the next concrete step.
- If you change article editing or admin article APIs, keep the Markdown editor bound to Markdown input, not rendered HTML.
- If you change menu code, keep the tree shape intact and verify both public and admin menu consumers.
- If you change frontend API wiring, check the running docker-compose stack as well as type-check/build output; SSR/base-URL issues can pass type-check but still fail at runtime.

## Delivery acceptance rules

- Do not treat a phase label, roadmap item, or passing type-check as completion.
- A feature is only complete when the real frontend page or admin screen can call the real backend API, persist or read real data, and the result survives a reload or reopen.
- Placeholder responses, empty arrays, hardcoded counts, and `ok(null)` style stubs are not shippable for user-facing flows.
- Every new or changed user-facing flow needs a concrete happy path verification and, where practical, a regression test that asserts the business effect, not just the response envelope.
- When frontend and backend contracts differ, update both sides together and verify the actual page that consumes the contract.
- If a document says something is done, the codebase must demonstrate it now; otherwise update the document to say it is still pending.
