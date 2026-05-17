# ZBlog Execution Lock

This file is the single source of truth for current execution.

If another agent continues this project, read this file first. Do not infer completion from old phase names, old plans, type-check success, controller existence, or route names.

## Current State

- Project goal: build and deploy a usable blog website based on FlecBlog frontend reuse plus a Java 21 Spring Boot backend.
- Product priority: blog-first. A larger personal-brand site like bugstack.cn is a later direction, not the current mainline.
- Backend standard: DDD-style Java implementation remains expected, but completion must be proven by working behavior, persistence, and verification.
- Active audit source: `docs/DELIVERY_AUDIT.md`.
- Historical plans are archived under `docs/archive/` and are reference-only.

## Locked Rules

- Only one implementation loop may be active at a time.
- Before coding a loop, define the exact frontend/admin entry, API calls, backend behavior, persistence or derived data path, and verification command in this file.
- A loop is complete only when a real page or admin screen works against real backend behavior, survives refresh or reopen, and has test or manual verification evidence.
- Do not create new root-level harness, phase, matrix, or planning documents. Update this file during execution and `docs/DELIVERY_AUDIT.md` after verification.
- If a detailed note is temporarily needed, place it in `docs/archive/` or delete it before handoff. The root `docs` directory must stay slim.
- Do not mark a phase as complete. Mark only concrete closed loops as complete.

## Active Work Slot

ID: REDIS-OUTBOX-RABBITMQ-BATCH-009

Status: closed, user accepted

Scope:
- Batch 9 only: Redis plus lightweight application-level MQ.
- Add Redis-backed article view debounce, recent hot ranking, total view ranking, site stats cache, and collect rate limiting.
- Add application outbox plus RabbitMQ publishing/consuming for one real article-published event chain.
- Keep PostgreSQL as the source of truth; Redis is an accelerator/guard, not the only persistence layer.
- Do not introduce Debezium, Elasticsearch, OpenResty/Nginx+Lua implementation, Kafka, or deployment hardening in this batch.
- Record Redis+Nginx+Lua as a later deployment/gateway enhancement, not as Batch 9 production code.

Exact API calls:
- `POST /api/v1/collect`: remains compatible and adds `article_view_counted` plus `article_view_count` when `type=pageview` and `article_id` is present; article view debounce is 10 seconds.
- `GET /api/v1/articles/hot?type=recent&limit=10`: returns published recent hot articles using Redis ZSET ranking and `hot_score`.
- `GET /api/v1/articles/hot?type=total&limit=10`: returns published total-view articles using PostgreSQL `view_count`.
- `GET /api/v1/stats/site`: may use a short Redis cache, but values must remain PostgreSQL-consistent after cache expiry or cache eviction.
- `GET /api/v1/admin/outbox`: returns observable outbox rows.
- `POST /api/v1/admin/outbox/publish-pending`: manually drains pending/failed outbox rows for local verification and retry.

Verification evidence:
- RED observed: `mvn -f server/pom.xml -Dtest=Batch9RedisOutboxRabbitMqTest test` initially failed because `/collect` did not return article counting metadata, collect rate limiting was missing, and the `event_outbox` table did not exist.
- RED observed after manual acceptance issues: targeted test failed because 10-second debounce recovery, dual ranking semantics, direct-create publish outbox, and duplicate slug handling were not implemented.
- PASS after manual issue fixes: `mvn -f server/pom.xml -Dtest=Batch9RedisOutboxRabbitMqTest test` - 7 tests, 0 failures, 0 errors.
- PASS after manual issue fixes: `mvn -f server/pom.xml test` - 67 tests, 0 failures, 0 errors.
- PASS: `npm --prefix blog run type-check`; observed existing local `@vue/language-core` warning from vue-router/volar.
- PASS: `npm --prefix blog run build`; observed existing Nuxt sourcemap/deprecation warnings.
- PASS: `npm --prefix admin run type-check`.
- PASS: `docker compose up --build -d server blog`.
- PASS: Docker running-stack smoke verified direct create-and-publish produced a sent outbox event and article-published notification, 10-second article-view debounce recovered after the window, duplicate slug returned 409, and recent/total hot article APIs returned distinct valid data.
- PASS after final acceptance fixes: Docker running-stack smoke verified article-published notification links use `/articles/edit/{id}` and the public blog bootstrap script defaults to light theme unless `localStorage.theme` is explicitly `dark`.

Manual acceptance:
- ACCEPTED: user manually verified Batch 9 after the Redis/MQ remediation loop and final notification/theme fixes on 2026-05-18.

Previous Batch 8 lock is retained below for traceability.

ID: PRE-DEPLOYMENT-CORE-CLOSURE-BATCH-008

Status: closed, user accepted

Scope:
- Batch 8 only: pre-deployment core closure.
- Fix article view/statistics semantics before adding Redis.
- Implement backend-real admin filters for visits, articles, comments, and files.
- Make upload/import/export/settings UI honest where backend support is intentionally partial.
- Keep visit browser/OS/location details admin-facing and remove privacy-adjacent fields from public comments.
- Do not introduce Redis, Elasticsearch, MQ, CDC, or deployment hardening in this batch.

Manual acceptance:
- ACCEPTED: user manually verified Batch 8 after the issue 1-4 remediation loop on 2026-05-18.

ID: PRE-DEPLOYMENT-FEATURE-TECH-AUDIT-BATCH-007

Status: closed, user accepted

Scope:
- Batch 7 only: pre-deployment feature and technology audit.
- This is an audit/planning batch, not a production-code implementation batch.
- Re-checked current Java/backend/frontend behavior before deployment hardening.
- Compared current behavior against current frontend-visible gaps and the old ZBlog backend refactor target stack.
- Produced an updated roadmap that decides which missing features and technology-stack enhancements should happen before deployment.
- Redis, Elasticsearch, MQ, CDC, and deployment code were not implemented in this batch.

Frontend/admin entry pages:
- Public article detail page, especially article `view_count`.
- Public blog search, RSS/Atom/Sitemap, site stats, and sidebar stats.
- Admin dashboard, article list, visit list, settings, files, comments, feedback, subscribers, notifications, RSS reader, and system pages.

Exact API calls:
- Audit current frontend API calls and backend routes rather than locking new implementation APIs.
- Known issue to verify: article detail `view_count` appears to remain `0` because visit collection updates `visit_events` but does not prove per-article `articles.view_count` increments.
- Known technology candidates to review: Redis, Elasticsearch, RabbitMQ/Kafka, PostgreSQL CDC via pgoutput/Debezium, outbox/event pattern, and deployment cost.

Reference inputs:
- Current repo code and frontend-visible behavior.
- `docs/DELIVERY_AUDIT.md`.
- FlecBlog reference behavior.
- Old ZBlog backend refactor plan: `D:\MyCode\ZBlogProject\ZBlog_1\docs\06_BACKEND_REFACTOR_PLAN.md`.
- min-lj Blog may be used as a learning reference for a richer Java blog stack, but do not copy its architecture blindly.

Expected audit output:
- Updated `Feature Completion Backlog` and `Batch Roadmap`.
- A clear list of must-fix-before-deploy items, optional portfolio/architecture enhancements, and post-deploy deferred items.
- Explicit recommendation for Redis scope beyond article view count if justified by real flows.
- Explicit recommendation for Elasticsearch strategy scope.
- Explicit recommendation for lightweight PostgreSQL CDC / Debezium / MQ / outbox scope, including why it is or is not worth doing before deployment.

User-visible expected behavior:
- The user can understand what is still missing before deployment and why.
- The next implementation batches are feature-based, testable, and not random or ad hoc.
- Technology-stack additions are tied to real behavior and can be defended in code review or an interview.

Automated RED test:
- Not required for the audit itself.
- The audit must identify which next implementation batch needs RED tests first.

Automated GREEN verification:
- Documentation-only audit batch; verify by reviewing changed docs and ensuring no production code was modified.

Docker running-stack verification:
- Not required for this documentation-only audit; conclusions are backed by code search and source reads.

Manual browser verification:
- ACCEPTED: user reviewed and accepted the adjusted roadmap on 2026-05-17.

Current functional gaps:
- Article detail `view_count` is not a true closed loop yet: the public article page sends a pageview with `article_id`, and `VisitCollectionService` stores `article_id` in `visit_events`, but no code updates `articles.view_count`. Admin article list/public article data can therefore show stale article-level counts.
- Public site/sidebar stats derive from real persisted data, but `total_page_views` currently adds `sum(articles.view_count)` and visit-event pageviews. Once article counters are fixed, the counting semantics must avoid double-counting.
- Public tracker can send a generic route pageview and the article page can also send an article-specific pageview, so Batch 8 must define one article-page PV attribution path.
- Admin visit list has filter UI/query types, but the backend `/api/v1/admin/stats/visits` currently accepts only page/page_size; browser, OS, and location remain explicit `unsupported` values.
- Admin article list exposes richer filters than the backend currently honors; backend admin list filters only keyword and publish state.
- Comment admin filters and pagination need a deeper pass; the current backend list path is not yet proven to honor all UI filters or scale beyond small data.
- File admin filters and upload settings need a deeper pass; backend file validation/storage still uses hardcoded local storage and size/type rules rather than all saved settings.
- Import/export remains partial: Markdown import only supports already-uploaded `/uploads/**` image references, WeChat export returns rendered HTML rather than a richer WeChat-specific format, and ZIP asset bundling only covers `/uploads/**` assets.
- Notification persistence/read-state is real, but event producers are partial; feedback creates notifications, while comments/friend/system events are not broadly wired yet.
- Mail outbox rows are durable, but delivery is synchronous/dev-outbox style with no retry worker, dead-letter handling, or admin-visible resend path.
- AI title/summary persistence is closed, but provider output length is prompt-guided rather than enforced; the user observed summaries can exceed intended length.
- System page is honest but limited: CPU usage, DB size, DB connection count, swap, remote update, email, and Feishu remain explicit unsupported/disabled values.

Current technology gaps:
- Redis is not present in dependencies or docker-compose; no production code currently depends on it.
- Elasticsearch is not present; public search is DB-backed and accepted for v1.
- RabbitMQ/Kafka is not present; no queue dependency or service exists in the active stack.
- PostgreSQL CDC/Debezium is not present; docker-compose contains PostgreSQL/server/blog/admin only.
- A mail outbox table/service exists, but there is no async outbox worker/retry pipeline.
- Current backend is Java 21/Spring Boot/PostgreSQL/Flyway/JDBC and is sufficient for a low-cost personal deployment if remaining user-visible gaps are closed first.

Redis candidate flows and recommendation:
- Candidate flows: article view count aggregation, hot articles/read ranking, site stats cache, settings cache, visit collection idempotency/dedup/rate limiting, and a small Redis Pub/Sub integration where message loss is acceptable.
- Recommendation: Redis is a required roadmap capability for this project, but Batch 8 should first define correct PostgreSQL-backed article view/stat semantics so Redis accelerates a proven model rather than hiding an unclear counter model.
- Implementation direction: use PostgreSQL as source of truth, add Redis for ranking/cache/rate-limit with explicit reconciliation or fallback, and consider an `@RedisTopic`-style auto-subscription wrapper as a portfolio highlight for non-critical events.
- Boundary: Redis Pub/Sub is not durable; core mail/notification/search-index delivery should still use outbox/MQ where reliability matters.

Elasticsearch strategy recommendation:
- DB-backed search remains the online/default strategy for v1 and first deployment.
- ES should be implemented as optional strategy-pattern code rather than a required deployed dependency: `SearchPort` with DB default adapter and configurable `ElasticsearchSearchAdapter` disabled by default.
- ES scope must include index schema, article create/update/delete indexing, rebuild-index admin operation, failure visibility, fallback to DB search, and tests for selected strategy behavior.
- Recommendation: plan a bounded ES strategy-code batch after core functional honesty work; do not require Elasticsearch in docker-compose or production deployment until the user chooses to operate it.

PostgreSQL CDC / Debezium / RabbitMQ/Kafka recommendation:
- PostgreSQL counterpart to MySQL+Canal+MQ is pgoutput/logical decoding or Debezium plus MQ. Do a minimal real PG+Debezium+MQ flow now rather than leaving it as planning-only work.
- Scope should stay deliberately small: one real source table/event, one MQ topic/queue, one consumer side effect, observable retry/error state, and a simple manual verification path.
- Kafka is useful for high-throughput durable event streaming and replay, but it adds concepts and deployment cost. Keep Kafka as a later planning/proof item unless the RabbitMQ/simple-MQ path becomes insufficient.
- Any async pipeline must define idempotency keys, retry policy, dead-letter/error visibility, replay/rebuild path, and synchronous fallback or accepted degradation.

Privacy-aware visit detail recommendation:
- Detailed visit geo/browser/OS parsing can be implemented now and stored/read in admin-facing visit analysis.
- Public/frontend areas should not display privacy-adjacent details such as commenter location, browser, OS, or IP. It is acceptable to keep backend read capability for admin/audit use while omitting these fields from public UI.
- This intentionally differs from FlecBlog where needed: privacy is preferred over showing user environment details in public comment areas.

Must-fix-before-deploy list:
- Batch 8: pre-deployment core closure. Fix article view/statistics semantics, admin list filter/pagination honesty, import/export/settings honesty, and backend-only visit geo/browser/OS parsing with public privacy masking.
- Batch 9: Redis + PG/Debezium/MQ portfolio stack. Add Redis ranking/cache/rate-limit behavior and one minimal real PG+Debezium+MQ flow, with Kafka kept as planning/proof unless explicitly selected.
- Batch 10: search strategy and deployment hardening. Add ES Strategy code with DB default if still desired, then complete production-like deployment hardening.
- Batch 11: FlecBlog parity recheck and final defer/implement decisions.

Required portfolio-enhancement list:
- Redis-backed hot article/ranking/stat cache/settings cache/rate-limit behavior with PostgreSQL reconciliation and fallback.
- Minimal PG+Debezium+MQ flow comparable in spirit to MySQL+Canal+MQ, but scoped to the simplest useful ZBlog event.
- Search strategy abstraction and optional Elasticsearch adapter with rebuild/fallback tests, disabled by default for online deployment.

Optional or post-deploy deferred list:
- True OAuth provider callback integration until provider credentials/callback domains are known.
- Scheduled RSS refresh beyond the accepted manual refresh flow.
- Full remote/relative Markdown asset ingestion and HTML image rewriting beyond accepted `/uploads/**` support.
- Rich WeChat-specific article formatting beyond raw rendered HTML export.
- Kafka or a fuller CDC/event-stream platform until the minimal PG+Debezium+MQ path is understood and useful.
- More complete system observability: DB size/connection count, CPU sampling, update-source configuration, mail/Feishu real status.

Updated batch order:
- Batch 8: PRE-DEPLOYMENT-CORE-CLOSURE.
- Batch 9: REDIS-PG-DEBEZIUM-MQ-MINIMAL.
- Batch 10: SEARCH-STRATEGY-DEPLOYMENT-HARDENING.
- Batch 11: FLECBLOG-PARITY-RECHECK.
- Optional after the minimal MQ chain: KAFKA-CDC-PROOF.

## Closed Implementation Loops

These are closed according to the current audit notes, not because of their phase labels:

- P0 user/account/admin-user baseline: database-backed login, register/profile/password, admin user CRUD, tests.
- P1 feedback/subscription/RSS baseline: database-backed feedback tickets, email subscribers, admin RSS read state, tests.
- P2 import/export baseline: article import, WeChat export, zip download, comment import, tests.
- Compatibility endpoints for parts of system admin, notifications, admin tools, and AI utility APIs exist, but deeper business completeness still needs audit.

Always verify `docs/DELIVERY_AUDIT.md` before relying on this summary.

## Completion Method

Every remaining feature must use this loop:

1. Lock exactly one feature below as the Active Work Slot.
2. Write a failing backend integration test for the business behavior.
3. Run the test and record the expected RED failure.
4. Implement the minimum Java backend slice needed to pass.
5. Run the targeted backend test until GREEN.
6. Run the relevant frontend type-check or build if a contract changed.
7. Write the user-facing acceptance procedure under `docs/issues/batchN/02-acceptance.md`, then give the user concrete manual acceptance steps for the real page or admin screen in the running app.
8. Wait for the user to confirm manual acceptance before pushing code, reporting work, or preparing the next-step plan.
9. If manual acceptance fails, write or update `docs/issues/batchN/03-issues.md` with phenomenon, expected behavior, cause, solution, key code, verification, and residual risk; after fixing, write or update `docs/issues/batchN/04-fix-report.md` before asking for re-acceptance.
10. After acceptance, update `DELIVERY_AUDIT.md` with evidence and residual risks, write or update `docs/issues/batchN/05-final-report.md`, then push/report/prepare the next plan if requested.

No production backend code should be written for a new feature before the RED test exists and has been observed failing.

## Feature Completion Backlog

This backlog is the current missing-feature source of truth. It is based on the Java backend, current frontend API calls, and the FlecBlog reference backend. Code reality wins over old phase documents.

### 1. Upload and Public Asset Delivery

Status: closed, user accepted

Current facts:
- Admin upload exists at `POST /api/v1/admin/files`.
- Public upload exists at `POST /api/v1/upload` for the known frontend public upload types.
- Uploaded DB rows store URLs like `/uploads/<filename>`.
- Spring static mapping exposes `/uploads/**` from the backend origin.
- Admin Docker nginx and blog Nuxt now proxy `/uploads/**` so relative upload URLs resolve from `localhost:4000` and `localhost:3000` as well as `localhost:8080`.

Done means:
- Admin-uploaded files render through the public blog URL after reload.
- Public upload flow used by frontend comments/editor works or is intentionally disabled in UI.
- Tests prove upload creates a file row, stores the file, serves the URL, and delete behavior is consistent.
- User manually verifies an uploaded image renders in the editor/blog page.

### 2. Visit Collection and Real Statistics

Status: closed

Current facts:
- Blog tracker posts to `POST /api/v1/collect`.
- Java backend has no `/collect` endpoint.
- `StatsService` still returns zero or empty values for visitors, trend, monthly pageviews, online users, and visit logs.
- FlecBlog records visits and powers dashboard, trend, visitor, and visit-log views from those records.

Done means:
- `POST /api/v1/collect` persists pageview/duration/event data or a deliberately scoped subset.
- `/stats/site`, `/admin/stats/dashboard`, `/admin/stats/trend`, and `/admin/stats/visits` derive from real visit data.
- Tests prove a collect call changes dashboard/trend/visit-log output.
- User manually verifies dashboard/visit list changes after visiting the blog.

### 3. Notifications

Status: closed

Current facts:
- Admin notification controller currently returns an empty list and `ok(null)` for read operations.
- Blog API also expects public `/notifications`, `/notifications/{id}/read`, and `/notifications/read-all`.
- FlecBlog has public and admin notification flows.

Done means:
- Notifications have a real table/model, read state, unread count, and pagination.
- At least one real business event creates a notification, such as comment submission, friend application, feedback, or system event.
- Admin and public notification APIs read and mutate the same persisted state according to auth rules.
- Tests prove unread count decreases after marking read.
- User manually verifies the notification bell/list behavior.

### 4. Password Reset and OAuth Completion

Status: open, product decision needed before implementation

Current facts:
- Frontend calls `/auth/forgot-password` and `/auth/reset-password`.
- FlecBlog supports OAuth begin/callback and password reset.
- Current audit says true OAuth callbacks and mail delivery are out of scope until credentials and callback behavior are defined.

Done means:
- If kept in scope now: token persistence, mail delivery or local dev token exposure, reset validation, and OAuth provider callback handling are implemented and tested.
- If deferred: frontend must not present broken flows as working.
- User manually verifies either the working flow or the intentionally hidden/deferred UI.

### 5. System Information

Status: closed

Current facts:
- `/admin/system/static`, `/admin/system/dynamic`, and `/admin/system/check-update` exist.
- Several fields are hardcoded placeholders, including server IP, disk totals, db table count, email status, version update list, and CPU/load values.

Done means:
- Values are either real where practical or explicitly documented as unsupported.
- No UI claims fake operational status.
- Tests cover stable real values such as DB status/table count and version response shape.
- User manually verifies the System page is honest and useful.

### 6. Mail, Subscription Delivery, and Feedback Fan-Out

Status: open

Current facts:
- Feedback tickets and subscribers are persisted.
- Mail delivery, unsubscribe email links, and feedback notification fan-out remain follow-up work.

Done means:
- SMTP settings are configurable.
- Subscription confirmation/unsubscribe and feedback notification behavior are tested with a fake mail sender.
- User manually verifies local dev mail capture or logs.

### 7. RSS Feed Reader

Status: open

Current facts:
- Admin RSS read-state endpoints exist.
- External RSS fetch scheduling/parsing is still follow-up work.

Done means:
- Feed sources can be fetched, parsed, stored, listed, and marked read.
- Scheduled refresh is configurable or manually triggerable.
- Tests use a fake RSS response and prove persistence.

### 8. Import/Export Completeness

Status: partial

Current facts:
- Baseline article import, WeChat export, Markdown ZIP download, and comment import exist.
- Image asset rewriting during import and deeper Artalk relationship/user migration remain follow-up work.

Done means:
- Imported Markdown image assets are rewritten or copied into the local media model.
- Comment import preserves target relationships and useful user metadata.
- Tests prove imported assets/comments survive reload and render.

### 9. AI Article Metadata Persistence

Status: closed, user accepted

Current facts:
- Product decision: AI-generated summary text reuses the persisted article `summary`; no separate `ai_summary` article field is added.
- Admin AI summary, AI summary, and title generation tools call the saved OpenAI-compatible AI settings and return real provider text.
- Admin article create/update persists generated title and summary; admin detail and public article detail read the same persisted fields.
- Backend supports the admin settings key format used by the frontend and surfaces safe upstream AI provider errors instead of hiding them behind generic 400 text.

Done means:
- AI title/summary generation can be saved through the article editor, survives reopen, and public article detail consumes the persisted `summary`.

### 10. Search, SEO, and Elasticsearch

Status: partial, later backend depth

Current facts:
- Public search and XML feeds exist.
- Earlier architecture discussions included Elasticsearch, but the current Java backend does not yet prove ES-backed indexing/ranking.

Done means:
- Either database search is accepted for v1, or Elasticsearch indexing/search is implemented behind a clear interface.
- Tests prove article create/update/delete affects search results.
- User manually verifies search quality on the public site.

### 11. Deployment Hardening

Status: open

Current facts:
- Docker local stack exists.
- Production deployment still needs a verified path: env vars, reverse proxy/static uploads, database persistence, logs, backup, and health checks.

Done means:
- A clean machine can run the stack from documented commands.
- Uploads and database data survive container restart.
- Blog/admin/server URLs work behind the intended domain or local production-like proxy.

## Canonical Roadmap

This is the only forward roadmap. Historical P0/P1/P2 labels are useful as audit history, but future work must follow this roadmap unless the user explicitly changes priority.

Roadmap ordering is based on:

- confirmed frontend calls or visible pages;
- confirmed Java backend gaps, stubs, hardcoded values, or missing persistence;
- FlecBlog reference behavior;
- dependency and user-visible impact.
- previously discussed target architecture, including Java 21, PostgreSQL, Redis, RabbitMQ, optional Elasticsearch, PostgreSQL CDC/Debezium, lightweight DDD, Strategy/Adapter/Event/Outbox patterns, and real tests.

Architecture note:
- Technology stack is allowed when it closes real blog behavior or creates a defensible integration seam.
- Do not add Redis, Elasticsearch, MQ, Kafka, Debezium, or CDC only to make the README look richer.
- For this project, "MySQL + Canal + MQ" should map to "PostgreSQL + pgoutput/Debezium + RabbitMQ/Kafka" only if the batch proves a real event/indexing/notification workflow.

### Closed Baseline

- AUTH-USER-ACCOUNT: login, register, profile, password, account, and admin user CRUD baseline.
- FEEDBACK-SUBSCRIPTION-RSS-BASELINE: feedback tickets, email subscribers, unsubscribe state, admin RSS read-state baseline.
- IMPORT-EXPORT-BASELINE: article import, WeChat export, Markdown ZIP download, and comment import baseline.

These remain subject to regression tests. If a future audit finds fake behavior, move the specific gap back into the open roadmap.

### Batch Roadmap

This is the fixed batch plan for the remaining work. Future agents must not regroup these items ad hoc without updating this section first.

1. BACKEND-TRUTH-DATA-BATCH-001
   - Status: closed, user accepted.
   - Includes: VISIT-STATS-CLOSED-LOOP, NOTIFICATIONS-CLOSED-LOOP, SYSTEM-INFO-HONESTY-CLOSED-LOOP.

2. CONTENT-ASSET-CLOSED-LOOP-BATCH-002
   - Status: closed, user accepted.
   - Includes: UPLOAD-ASSET-CLOSED-LOOP and the asset-handling portion of IMPORT-EXPORT-DEEP-COMPLETION.

3. USER-TOUCH-CLOSED-LOOP-BATCH-003
   - Status: closed, user accepted.
   - Includes: MAIL-FANOUT-CLOSED-LOOP and PASSWORD-RESET-OAUTH-DECISION.

4. RSS-READER-CLOSED-LOOP-BATCH-004
   - Status: closed, user accepted.
   - Includes: RSS-READER-CLOSED-LOOP only.
   - Scope guard: do not include AI metadata, Search/SEO, deployment, or FlecBlog parity work in this batch.

5. AI-ARTICLE-METADATA-BATCH-005
   - Status: closed, user accepted.
   - Includes: AI-ARTICLE-METADATA-DECISION only.

6. SEARCH-SEO-DEPTH-BATCH-006
   - Status: closed, user accepted.
   - Includes: SEARCH-SEO-DEPTH only.

7. PRE-DEPLOYMENT-FEATURE-TECH-AUDIT-BATCH-007
   - Status: closed, user accepted.
   - Includes: code review, feature-gap review, technology-stack review, and roadmap correction before deployment hardening.
   - Scope guard: audit and planning only; do not implement Redis/ES/MQ/CDC/deployment code in this batch unless the user explicitly re-scopes it.
   - Required outcome: decide whether Redis, Elasticsearch, lightweight async/outbox/MQ, PostgreSQL CDC, and remaining base features belong before deployment.

8. PRE-DEPLOYMENT-CORE-CLOSURE-BATCH-008
   - Status: closed, user accepted.
   - Includes: article-level `view_count`, public/sidebar/admin stats semantics, duplicate pageview prevention, admin list filter/pagination honesty, import/export/settings honesty, and visit geo/browser/OS parsing.
   - Scope guard: backend/admin may store and read detailed visit environment data, but public/frontend areas must not display privacy-adjacent details such as commenter location, browser, OS, or IP.

9. REDIS-PG-DEBEZIUM-MQ-MINIMAL-BATCH-009
   - Status: closed, user accepted.
   - Includes: Redis hot article/read ranking, site-stats cache, visit dedup/rate-limit, and one minimal application outbox + RabbitMQ article-published event chain.
   - Scope guard: Debezium/CDC, Kafka, and Redis+Nginx+Lua are deferred; this batch intentionally keeps MQ tiny and verifiable.

10. SEARCH-STRATEGY-DEPLOYMENT-HARDENING-BATCH-010
   - Status: planned after core closure and the minimal portfolio stack are accepted.
   - Includes: optional `SearchPort`/ES Strategy code with DB default, production-like deployment, environment variables, persistence, reverse proxy/static uploads, logs, backup, and health checks.
   - Scope guard: do not require Elasticsearch service deployment for the default online path, and do not hide missing functional gaps behind deployment documentation.

11. FLECBLOG-PARITY-RECHECK-BATCH-011
   - Status: planned.
   - Includes: final FlecBlog parity recheck and explicit defer/implement decisions for any remaining gaps after pre-deployment functional and technology work.

Optional architecture proof after the minimal MQ chain:
- KAFKA-CDC-PROOF: explain Kafka first, then decide whether a Kafka-based CDC/event-stream proof is worth the extra deployment and conceptual cost.

### Open Roadmap

1. VISIT-STATS-CLOSED-LOOP
   - Automated verified and manually accepted.
   - `POST /api/v1/collect` writes `visit_events` rows, and site/admin stats, trend, and visit logs derive from those rows.
   - Evidence: `BackendTruthDataBatchTest#collectPageviewChangesStatsTrendAndVisitLog` proves a collect call changes stats and visit-log output.

2. NOTIFICATIONS-CLOSED-LOOP
   - Automated verified and manually accepted.
   - Admin/public notification APIs share persisted notifications with unread counts and read-state mutations.
   - Evidence: `BackendTruthDataBatchTest#feedbackCreatesNotificationAndReadOperationsUpdateUnreadCount` proves feedback creates a notification and read operations change unread count.

3. UPLOAD-ASSET-CLOSED-LOOP
   - Admin upload exists, but public `/api/v1/upload` is missing while blog comment/editor utilities call it.
   - Done when uploaded assets are stored, listed, served, deleted consistently, and render in the real UI after reload.

4. SYSTEM-INFO-HONESTY-CLOSED-LOOP
   - Automated verified and manually accepted.
   - System info now returns real runtime/DB/table/disk values where practical and explicit `unsupported`/`disabled` values where not.
   - Evidence: `BackendTruthDataBatchTest#systemInfoReturnsRealDbRuntimeAndHonestUnsupportedFields` covers non-placeholder DB/runtime fields and unsupported/disabled states.

5. PASSWORD-RESET-OAUTH-DECISION
   - Automated verified and manually accepted.
   - Forgot/reset password now creates expiring single-use tokens, writes reset mail records, updates the password, rejects the old password, and rejects token reuse.
   - OAuth begin endpoints now return explicit `501` unsupported responses when provider credentials are not configured; frontend OAuth buttons remain hidden by default config.
   - Evidence: `Batch3UserTouchClosedLoopTest#forgotPasswordResetChangesPasswordAndTokenIsSingleUse` and `Batch3UserTouchClosedLoopTest#oauthBeginIsExplicitlyUnsupportedWithoutProviderCredentials`.

6. MAIL-FANOUT-CLOSED-LOOP
   - Automated verified and manually accepted with unsubscribe-link caveat.
   - Feedback submit/admin reply and subscribe/unsubscribe now create durable `mail_outbox` rows through the mail abstraction/dev outbox sender.
   - Evidence: `Batch3UserTouchClosedLoopTest#feedbackSubscribeAndUnsubscribeCreateDurableMailRecords`.

7. RSS-READER-CLOSED-LOOP
   - Automated verified and manually accepted.
   - Friend `rss_url` sources can be manually refreshed, parsed as RSS/Atom, inserted into `rss_feed_articles`, listed in admin RSS, marked read, deduplicated on repeated refresh, and failed sources persist explicit error state.
   - Scheduled refresh remains deferred; manual refresh is the accepted Batch 4 scope.

8. IMPORT-EXPORT-DEEP-COMPLETION
   - Baseline import/export exists, but image asset rewriting and deeper comment/user relationship migration remain.
   - Done when imported assets/comments survive reload and render correctly.

9. AI-ARTICLE-METADATA-DECISION
   - Automated verified and manually accepted.
   - Product decision: reuse the existing persisted article `summary` field for AI-generated summary text; do not add a separate `ai_summary` article field.
   - Evidence: `Batch5AiArticleMetadataClosedLoopTest` proves AI-generated title/summary can be saved, reopened, and exposed through public article detail without a fake `ai_summary` field.

10. SEARCH-SEO-DEPTH
    - Automated verified and manually accepted.
    - Product decision: DB-backed search is accepted for v1; Elasticsearch is deferred as a later enhancement and must not be fake-configured or fake-indexed.
    - Evidence: `Batch6SearchSeoDepthTest` proves article create/update/unpublish/delete lifecycle changes affect public search, RSS, Atom, and Sitemap outputs.

11. PRE-DEPLOYMENT-FEATURE-TECH-AUDIT
    - Closed, user accepted.
    - Purpose: re-audit current feature completeness and target technology stack before deployment hardening.
    - Must verify user-visible base gaps such as per-article `view_count`, not only global visit stats.
    - Must reconcile current implementation with the old backend refactor direction: Redis, RabbitMQ, optional Elasticsearch, PostgreSQL CDC/Debezium, lightweight DDD, Strategy/Adapter/Event/Outbox patterns.
    - Done when the roadmap is corrected, must-fix-before-deploy items are listed, and the user accepts the next implementation order.

12. PRE-DEPLOYMENT-CORE-CLOSURE
    - Open, recommended next.
    - Combines the previously over-split article view/statistics, admin list honesty, import/export/settings honesty, and visit detail parsing work.
    - Current evidence: article pages send `article_id` pageviews, but `articles.view_count` is not updated; site stats also need clear semantics to avoid double-counting article views and pageview events.
    - Done means article/admin/sidebar/dashboard stats have one coherent counting model, visible filters are real or hidden, import/export/settings UI is honest, and visit geo/browser/OS details are stored/read in admin but not displayed publicly.

13. REDIS-PG-DEBEZIUM-MQ-MINIMAL
    - Required portfolio/architecture work.
    - Redis should support real ranking/cache/rate-limit behavior with PostgreSQL reconciliation and fallback.
    - PG+Debezium+MQ should implement one simplest real flow now, comparable in spirit to min-lj Blog's MySQL+Canal+MQ, rather than staying planning-only.
    - Kafka remains a later explain-and-proof item if the simple MQ path is not enough.

14. SEARCH-STRATEGY-DEPLOYMENT-HARDENING
    - Open after core closure and minimal portfolio stack.
    - Elasticsearch should be a configurable strategy with DB fallback, indexing, reindexing, and failure visibility, disabled by default for online deployment.
    - Deployment hardening proves a clean environment can run the stack and survive restart with data/uploads intact.

15. FLECBLOG-PARITY-RECHECK
    - Final audit pass against FlecBlog after the pre-deployment feature/technology work.
    - Done when every intentional difference is documented and every required missing capability is either implemented or explicitly deferred by the user.

16. KAFKA-CDC-PROOF
    - Optional proof after the minimal PG+Debezium+MQ chain is useful and the user understands whether Kafka is worth the added complexity.

## Next Locked Implementation Candidate

ID: PRE-DEPLOYMENT-CORE-CLOSURE-BATCH-008

Status: ready, not started

Reason:
- Batch 7 audit is closed and user accepted.
- The next implementation batch should close core user-visible gaps together before adding Redis/MQ/ES/deployment complexity.
- Batch 8 must use RED tests first and must not implement Redis, Elasticsearch, MQ, CDC, or deployment hardening.

Frontend/admin entry pages:
- Public article detail page: `/posts/[slug]`, article header `view_count`.
- Public site/sidebar stats that consume `GET /api/v1/stats/site`.
- Admin dashboard, visit list, article list, comment list, file list.
- Admin import/export and settings pages where UI may expose unsupported options.

Exact API calls:
- `GET /api/v1/articles/{slug}`
- `POST /api/v1/collect`
- `GET /api/v1/stats/site`
- `GET /api/v1/admin/stats/dashboard`
- `GET /api/v1/admin/stats/trend`
- `GET /api/v1/admin/stats/visits`
- Admin article/comment/file list endpoints used by the current UI.
- Admin import/export/settings endpoints used by the current UI.

Backend behavior to prove:
- Visiting an article has one coherent article-level and site-level counting model.
- `articles.view_count`, public article detail, admin article list, dashboard/sidebar/site stats, and visit events do not contradict each other.
- Pageview counting avoids double-counting generic route pageviews and article-specific pageviews.
- Visible admin filters either work against backend parameters or are hidden/disabled with honest UI.
- Import/export/settings UI does not claim unsupported capabilities.
- Visit geo/browser/OS details may be stored/read in admin, but public/frontend areas must not expose privacy-adjacent visitor details such as IP, location, browser, or OS.

Persistence or derived data path:
- Define whether article view count is a durable aggregate column, a query derived from `visit_events`, or a hybrid model.
- Preserve `visit_events` as the detailed event source.
- Keep privacy-sensitive details admin-only.

Automated RED test:
- Required before production code. At minimum prove current article visits do not update article-level `view_count` or currently cause ambiguous/double-counted stats.

Automated GREEN verification:
- Targeted Batch 8 tests.
- Full backend tests.
- Frontend type-check/build if UI contracts or visible filters/settings screens change.

Manual browser verification:
- User verifies an article visit updates the expected visible count after reload.
- User verifies sidebar/dashboard/admin article list statistics are coherent.
- User verifies admin filters/settings/import-export UI is honest.
- User verifies public pages do not expose visitor privacy details.

## Handoff Protocol

Every handoff should include only:

- Current active slot ID and status.
- Files changed in the current loop.
- Verification commands and outcomes.
- One next recommended slot from the open queue.
- Any user-provided assets, credentials, domains, or server information still needed.

If a future agent cannot explain the active slot in five sentences, stop and clean this file before coding.
