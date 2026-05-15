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

ID: CONTENT-ASSET-CLOSED-LOOP-BATCH-002

Status: closed

Scope:
- Batch 2 only: content-asset-closed-loop.
- UPLOAD-ASSET-CLOSED-LOOP: prove admin/public uploads write real files, create `files` rows, return publicly served URLs, and behave consistently after deletion.
- IMPORT-EXPORT-DEEP-COMPLETION: prove imported/exported article content does not silently keep broken image paths, and comment import either binds to a target article or reports failed items.

Frontend/admin entry pages:
- Admin file page uses `POST /api/v1/admin/files`, `GET /api/v1/admin/files`, and `DELETE /api/v1/admin/files/{id}`.
- Admin article editor inserts uploaded media URLs into Markdown through `/api/v1/admin/files`.
- Public blog comment, feedback, and avatar upload utilities post to `/api/v1/upload` and then use returned `file_url` in public content.
- Admin import/export page calls `POST /api/v1/admin/articles/import`, `POST /api/v1/admin/articles/{id}/wechat/export`, `GET /api/v1/admin/articles/{id}/download/zip`, and `POST /api/v1/admin/comments/import`.

Exact API calls:
- `POST /api/v1/upload`
- `POST /api/v1/admin/files`
- `GET /api/v1/admin/files`
- `DELETE /api/v1/admin/files/{id}`
- `GET /uploads/{filename}`
- `POST /api/v1/admin/articles/import`
- `GET /api/v1/admin/articles/{id}`
- `POST /api/v1/admin/articles/{id}/wechat/export`
- `GET /api/v1/admin/articles/{id}/download/zip`
- `POST /api/v1/admin/comments/import`
- `GET /api/v1/comments?target_type=article&target_key={slug}`

Current backend implementation:
- Admin upload and public upload both write under local `uploads/` and store rows in `files`.
- Static resource mapping exposes `/uploads/**` from the local upload directory.
- File deletion currently soft-deletes the DB row.
- Article import stores Markdown and renders a basic HTML snapshot.
- Markdown ZIP download currently exports the Markdown entry only.
- Comment import accepts Artalk-like JSON and inserts comments.

Missing persistence or derived data:
- Tests must prove uploaded files are actually served over HTTP and that delete behavior does not leave a misleading listed/served state.
- Public upload type and response shape must match frontend expectations for comment/avatar/feedback upload callers.
- Imported Markdown image handling must support a real, accessible Markdown image link path and reject or report unsupported local/relative image paths instead of silently importing broken references.
- Comment import must not silently attach comments to a fake default article when the import item lacks a target.

User-visible expected behavior:
- Uploaded images can be inserted into an article/comment and opened through the returned URL after reload.
- Deleted media no longer appears in the admin file list and its public availability is consistent with the delete semantics.
- Importing Markdown with a supported public image URL preserves a usable image reference in the article and ZIP export.
- Importing Markdown with unsupported local image paths reports failure instead of pretending success.
- Comment import binds to the intended article target or reports a failed item.

Automated RED test:
- Add or extend backend integration tests before production code, then run targeted tests and observe failures for unproven asset delivery/delete/import semantics.

Automated GREEN verification:
- PASS: `mvn -f server/pom.xml -Dtest=Batch2ContentAssetBatchTest test` - 7 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml -Dtest=P2ImportExportApiTest test` - 2 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml -Dtest=Phase4InteractionApiTest test` - 7 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 45 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- OBSERVED: `npm --prefix blog run type-check` completed with the existing local `vue-router/volar/sfc-route-blocks` warning for missing `@vue/language-core`.
- PASS: `npm --prefix admin run build`.
- PASS: `npm --prefix blog run build`.
- PASS: `docker compose up --build -d server admin blog`.
- PASS: Docker upload proxy check uploaded `/uploads/1778865616634_docker-proxy-check.png`; `http://localhost:8080`, `http://localhost:4000`, and `http://localhost:3000` all returned 200 with the same 31-byte file.
- PASS: Docker publish check created article `docker-publish-check`, updated it through `PUT /api/v1/admin/articles/{id}` with `is_publish=true`, and `GET /api/v1/articles/docker-publish-check` returned 200.
- PASS: `问题清单2.md` avatar follow-up verified that homepage and about-page owner images render direct `/uploads/...` URLs instead of Nuxt IPX `/_ipx/_/uploads/...`, and the actual avatar/photo files return 200 from `localhost:3000`, `localhost:4000`, and `localhost:8080`.
- PASS: `问题清单2.md` Markdown ZIP follow-up added a RED/GREEN export test; ZIP download now rewrites `/uploads/<file>` Markdown links to `assets/<file>` and includes the image bytes in the ZIP.

Manual browser verification:
- Local Docker verification confirms the previously broken relative `/uploads/**` URLs now work from backend, admin nginx, and blog Nuxt origins.
- Local Docker verification confirms article edit-page publish persistence through the same `PUT /api/v1/admin/articles/{id}` path used by the admin UI.
- ACCEPTED: user manually verified Batch 2 remediation fixes against the local running stack on 2026-05-16.
- ACCEPTED: exact browser checklist screens were re-accepted by the user after remediation.

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
7. Verify the real page or admin screen manually in the running app.
8. Update `DELIVERY_AUDIT.md` with evidence and residual risks.
9. Ask the user for manual acceptance before moving to the next feature.

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

Status: partial

Current facts:
- AI endpoints return summary/title text.
- A dedicated persisted `ai_summary` article field is not implemented.

Done means:
- Product decides whether `summary` is enough or `ai_summary` is a separate field.
- If separate: migration, DTOs, admin editor binding, and tests are added.

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
   - Frontend exposes forgot/reset password and OAuth UI paths.
   - Done when the product either implements token/mail/OAuth provider flows or hides/marks unsupported flows honestly.

6. MAIL-FANOUT-CLOSED-LOOP
   - Feedback/subscription persistence exists, but mail delivery and notification fan-out are not complete.
   - Done when SMTP or dev mail capture proves subscription/feedback delivery behavior.

7. RSS-READER-CLOSED-LOOP
   - Admin RSS read state exists, but external feed fetching/parsing/scheduling is not complete.
   - Done when feed sources are fetched, parsed, stored, listed, and marked read.

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

ID: VISIT-STATS-CLOSED-LOOP

Status: ready, not started

Reason:
- It is a confirmed broken frontend/backend contract: the blog sends `POST /api/v1/collect`, but Java has no route.
- It is also a confirmed fake-completion area: visitors, trend, visit logs, and several dashboard values are zero/empty placeholders.
- Completing it first gives us a measurable TDD loop: one collect call must change real dashboard/trend/visit-log output.

Before coding, fill this section:
- Frontend/admin entry pages:
- Exact API calls:
- Current backend implementation:
- Missing persistence or derived data:
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
