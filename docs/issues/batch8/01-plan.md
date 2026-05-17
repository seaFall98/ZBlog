# Batch 8 Plan: Pre-deployment Core Closure

## Scope

Batch 8 closes visible pre-deployment gaps without introducing Redis, Elasticsearch, MQ, CDC, or deployment hardening.

The batch uses a mixed counting model:

- `visit_events` is the persisted pageview/event fact table.
- `articles.view_count` is the article-level aggregate counter displayed on public article pages and admin article lists.
- Site PV is derived only from `visit_events` and must not add `sum(articles.view_count)`.

## Features

- Article pageview collection increments `articles.view_count` when `POST /api/v1/collect` receives `type=pageview` plus `article_id`.
- Site statistics keep `total_page_views` as visit-event pageviews only.
- Admin visit list supports real filtering by keyword/url, visitor ID, IP, excluded IPs, date range, browser, OS, and location.
- Admin article list supports real filtering by keyword, category, tags, location, publish state, top, essence, outdated, and date range.
- Admin comment list supports real filtering by keyword, status, deleted state, parent/child comment, and date range.
- Admin file list supports real filtering by keyword, content type, status, upload type, size range, and date range.
- Visit list returns admin-facing browser/OS parsed from User-Agent; location remains `unsupported`.
- Public comment API and public comment UI do not expose `location`, `browser`, or `os`.
- Upload settings UI shows only currently real local storage behavior and the fixed 10MB backend limit.
- Import/export UI states the current boundary: Markdown import supports only already-uploaded `/uploads/**` images; HTML export is basic HTML, not a full WeChat layout engine.

## TDD

Add `Batch8PreDeploymentCoreClosureTest` before production implementation.

Expected RED:

- Article pageview does not increment article `view_count`.
- Site PV semantics are not protected from future double counting.
- Admin visit/article/comment/file filters are not fully honored.
- Public comment API exposes privacy-adjacent fields.

Expected GREEN:

- Targeted Batch 8 test proves all behavior above.
- Full backend test suite remains green.
- Admin and blog type-check/build continue to pass after frontend honesty changes.

## Out of Scope

- Redis hot ranking/cache/rate limit.
- Elasticsearch search strategy.
- PostgreSQL CDC, Debezium, RabbitMQ, Kafka, or outbox worker hardening.
- New import/export engines beyond honest UI boundary text.
- Real geographic IP lookup.

