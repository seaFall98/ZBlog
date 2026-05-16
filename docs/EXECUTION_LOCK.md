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

ID: AI-ARTICLE-METADATA-BATCH-005

Status: closed, user accepted

Scope:
- Batch 5 only: AI article metadata decision and persistence closed loop.
- Product decision: reuse existing `articles.summary` / article `summary` as the AI-generated summary persistence target; do not add a separate `ai_summary` column or fake article field.
- Keep `/api/v1/admin/ai/summary`, `/api/v1/admin/ai/ai-summary`, and `/api/v1/admin/ai/title` as generation tools; generated text must be saved through the real article create/update summary/title fields.
- Do not include Search/SEO, deployment, FlecBlog parity, or unrelated AI feature work.

Frontend/admin entry pages:
- Admin article editor page uses AI title/summary generation controls and saves through admin article create/update APIs.
- Public article page consumes the persisted article `summary` for SEO/metadata and any summary display required by the current UI.

Exact API calls:
- `POST /api/v1/admin/ai/summary`
- `POST /api/v1/admin/ai/ai-summary`
- `POST /api/v1/admin/ai/title`
- `POST /api/v1/admin/articles`
- `PUT /api/v1/admin/articles/{id}`
- `GET /api/v1/admin/articles/{id}`
- `GET /api/v1/articles/{slug}`

Current backend implementation:
- AI utility endpoints call an OpenAI-compatible chat-completions provider using saved AI settings.
- Article create/update/detail already persists and returns `summary` and `title`.
- There is no backend `ai_summary` persistence field, and Batch 5 keeps that as an explicit product decision.

Missing persistence or derived data:
- No separate AI metadata persistence remains in Batch 5 scope; `summary` is the accepted persistence field for AI-generated summary text.
- The AI settings prompt key `ai_summary_prompt` remains as a generation prompt setting for the `/admin/ai/ai-summary` tool, not as an article data field.

User-visible expected behavior:
- Admin can generate a title or summary using AI, save the article, reopen or refresh the editor, and see the generated title/summary still present.
- Article responses expose the real persisted `summary`; no fake `ai_summary` article field is required.
- If AI provider config is missing or invalid, generation fails explicitly rather than pretending to save metadata.

Automated RED test:
- PASS: `mvn -f server/pom.xml -Dtest=Batch5AiArticleMetadataClosedLoopTest test` first failed after the test compiled because admin/blog frontend contracts still referenced the fake `ai_summary` article field.

Automated GREEN verification:
- PASS: `mvn -f server/pom.xml -Dtest=Batch5AiArticleMetadataClosedLoopTest test` - 3 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 53 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- OBSERVED: `npm --prefix blog run type-check` completed with the existing local `vue-router/volar/sfc-route-blocks` warning for missing `@vue/language-core`.
- PASS: Docker running-stack AI title endpoint returned a real generated title using the saved DeepSeek-compatible AI settings; no API key was written to code, docs, tests, or logs.

Manual browser verification:
- ACCEPTED: user manually verified DeepSeek AI config, AI title generation, summary generation, save/reopen persistence, and public article summary behavior in the running stack on 2026-05-17.

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
   - Status: planned.
   - Includes: AI-ARTICLE-METADATA-DECISION only.

6. SEARCH-SEO-DEPTH-BATCH-006
   - Status: planned.
   - Includes: SEARCH-SEO-DEPTH only.

7. DEPLOYMENT-HARDENING-BATCH-007
   - Status: planned.
   - Includes: DEPLOYMENT-HARDENING only.

8. FLECBLOG-PARITY-RECHECK-BATCH-008
   - Status: planned.
   - Includes: final FlecBlog parity recheck and explicit defer/implement decisions for any remaining gaps.

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
   - Automated verified, awaiting manual acceptance.
   - Friend `rss_url` sources can be manually refreshed, parsed as RSS/Atom, inserted into `rss_feed_articles`, listed in admin RSS, marked read, deduplicated on repeated refresh, and failed sources persist explicit error state.
   - Scheduled refresh remains deferred; manual refresh is the accepted Batch 4 scope.

8. IMPORT-EXPORT-DEEP-COMPLETION
   - Baseline import/export exists, but image asset rewriting and deeper comment/user relationship migration remain.
   - Done when imported assets/comments survive reload and render correctly.

9. AI-ARTICLE-METADATA-DECISION
   - AI endpoints return text, but dedicated `ai_summary` persistence is undecided.
   - Done when the product either accepts the current `summary` field or implements a separate persisted field.

10. SEARCH-SEO-DEPTH
    - Public search and feeds exist, but Elasticsearch/indexing/ranking depth is not complete.
    - Done when the accepted v1 search strategy is explicit and tests prove create/update/delete affects results.

11. DEPLOYMENT-HARDENING
    - Local Docker exists, but production-like deployment, persistence, reverse proxy, upload serving, logs, backup, and health checks need proof.
    - Done when a clean environment can run the stack and survive restart with data/uploads intact.

12. FLECBLOG-PARITY-RECHECK
    - Final audit pass against FlecBlog after items 1-11.
    - Done when every intentional difference is documented and every required missing capability is either implemented or explicitly deferred by the user.

## Next Locked Implementation Candidate

ID: AI-ARTICLE-METADATA-BATCH-005

Status: ready, not started

Reason:
- Batch 4 is automated verified and user accepted.
- The fixed Batch Roadmap marks AI-ARTICLE-METADATA-BATCH-005 as the next batch after RSS acceptance.
- This next batch must stay limited to the AI article metadata decision and must not include Search/SEO, deployment, or FlecBlog parity work.

Before coding the next batch, fill this section:
- Frontend/admin entry pages:
- Exact API calls:
- Current backend implementation:
- Missing metadata persistence or product-decision behavior:
- User-visible expected behavior:
- Automated RED test:
- Automated GREEN verification:
- Manual browser verification:

## Handoff Protocol

Every handoff should include only:

- Current active slot ID and status.
- Files changed in the current loop.
- Verification commands and outcomes.
- One next recommended slot from the open queue.
- Any user-provided assets, credentials, domains, or server information still needed.

If a future agent cannot explain the active slot in five sentences, stop and clean this file before coding.
