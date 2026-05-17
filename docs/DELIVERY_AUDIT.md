# Delivery Audit

> Goal: identify every place where the frontend already calls a backend API, but the backend is missing, stubbed, or not actually closed-loop yet.

## Audit standard

A flow is only marked complete when all of the following are true:

- the real frontend page or admin screen can call the real backend API;
- the backend persists or derives real data, not placeholder data;
- the result still holds after a reload or reopen;
- there is at least one concrete happy-path verification that checks the business effect, not only the response envelope.

Phase names, controller existence, and passing type-checks are not completion evidence.

## Already closed or largely closed

These areas have been brought to a real closed loop in the current repo state:

- Articles: Markdown-first storage, detail rendering, admin editing, and related queries.
- Menus: hierarchical tree data and admin CRUD.
- Comments: public submission and admin moderation flows.
- Friends: public display and admin management.
- Files: upload/list/delete flows.
- Moments: public list plus admin CRUD and filtering.
- Basic auth compatibility endpoints: refresh and logout.
- User/account baseline: database-backed login/register/profile/password/admin user CRUD.
- Feedback/subscription/RSS baseline: public feedback tickets, email subscribers, and admin RSS read-state management.
- Batch 4 RSS reader: manual friend RSS refresh fetches RSS/Atom sources, imports real articles, preserves read state, prevents duplicates, and records explicit source failures; automated verified and user-accepted in the local running stack.
- Import/export baseline: admin article import, WeChat export, Markdown ZIP download, and comment import.
- Batch 2 content assets: upload/public asset delivery and supported imported Markdown image links are automated-verified, Docker-verified, and user-accepted in the local running stack.
- Compatibility endpoints for system admin and notifications that the current frontend expects.
- Admin tools and AI utility endpoints: link metadata, video parsing, remote image download, AI config test, summary, AI summary, and title generation.
- Batch 1 backend truth data: visit collection/stat derivation, persisted notifications/read-state, and honest system info are automated-verified and user-accepted in the local running stack. Batch 7 clarified that this proves global visit/stat derivation, not article-level `articles.view_count` mutation.

## Critical gaps

These are the highest-priority mismatches between frontend calls and backend reality.

### Upload and public asset delivery

- Status: automated verified, Docker verified, and manually accepted.
- Admin upload at `POST /api/v1/admin/files` and public upload at `POST /api/v1/upload` both write real files under `uploads/`, create `files` rows, and return `/uploads/<filename>` URLs with `original_name` for frontend callers.
- Static serving for `/uploads/**` is covered by integration tests that fetch the returned URL over HTTP and verify the uploaded bytes.
- Docker verification confirms the same returned `/uploads/<filename>` URL is reachable through backend `localhost:8080`, admin nginx `localhost:4000`, and blog Nuxt `localhost:3000`.
- Admin delete removes the file from the list and deletes the physical asset so the old `/uploads/**` URL returns 404.
- Public upload accepts only the known frontend upload types: `用户头像`, `评论贴图`, and `反馈投诉`; unknown types fail instead of being silently relabeled.

### Visit collection and real statistics

- Status: closed for Batch 8 core semantics, automated verified and user accepted.
- The blog tracker posts to `POST /api/v1/collect`, and Java now persists tracker payloads into `visit_events`.
- `/api/v1/stats/site`, `/api/v1/admin/stats/dashboard`, `/api/v1/admin/stats/trend`, and `/api/v1/admin/stats/visits` derive visit-related values from persisted visit events.
- Evidence: `mvn -f server/pom.xml -Dtest=BackendTruthDataBatchTest test` passes and proves a collect call changes site/dashboard/trend/visit-log output.
- Batch 8 correction: public article pages send `article_id` pageviews and the backend increments `articles.view_count` for published articles.
- Batch 8 correction: `total_page_views` now means only `visit_events` pageview count and no longer adds article aggregate counters.
- Batch 8 correction: admin visit list supports real filters for keyword/url, visitor ID, IP, excluded IPs, browser, OS, date range, and location. Browser/OS are parsed from User-Agent; location remains explicit `unsupported`.
- Batch 8 manual issue 1 correction: direct article page open/refresh now sends an article-level pageview on initial mount; generic route pageviews skip `/posts/` paths to avoid double counting.
- Batch 8 manual issue 2 correction: public article header now consumes the shared current-article state after tracker acceptance, so the current page displays the post-count `view_count` instead of the SSR pre-count snapshot.

### Notifications

- Status: automated verified and manually accepted.
- Admin and public notification endpoints now share persisted `notifications` rows with pagination, `unread_count`, single-read, and read-all mutations.
- Feedback submission now creates a real `feedback_new` notification.
- Evidence: `BackendTruthDataBatchTest#feedbackCreatesNotificationAndReadOperationsUpdateUnreadCount` proves unread count changes after read operations.

### Public auth/account/OAuth flows

- Database-backed login, register, refresh, profile update, password update, account deactivation, OAuth unbind, and admin user CRUD are now implemented and tested.
- Forgot/reset password is now a real token loop: `forgot-password` creates an expiring token and durable reset mail record; `reset-password` updates the password, invalidates the old password, and rejects token reuse.
- True OAuth provider authorization callbacks remain out of scope until provider credentials and callback behavior are defined; OAuth begin endpoints now return explicit `501 unsupported` instead of missing-route or fake-provider behavior.
- Frontend OAuth buttons remain hidden by default config unless a provider is explicitly enabled.

### Feedback and subscription flows

- Public feedback tickets, ticket lookup, admin feedback handling, email subscription, unsubscribe state, and admin subscriber management are now database-backed and tested.
- Feedback notification fan-out into the in-app notification table is now covered by Batch 1.
- Feedback submit/admin reply and subscribe/unsubscribe now create durable `mail_outbox` records through the mail abstraction/dev outbox sender.

### RSS reader

- Status: automated verified and manually accepted.
- Friend `rss_url` is now the RSS source configuration for manual refresh.
- `POST /api/v1/admin/rssfeed/refresh` fetches configured RSS sources, parses RSS and Atom responses, inserts real `rss_feed_articles` rows, preserves existing read state, prevents duplicate imports by `(friend_id, link)`, and records explicit source failure state on `friends`.
- The admin RSS page now has a manual `刷新RSS` action and reports aggregate inserted/failed counts after refresh.
- Deferred: scheduled refresh remains out of Batch 4 scope, and friend admin source-list UI does not yet expose detailed `rss_status` / `rss_last_error` fields.

### Admin tools and AI utilities

- Admin tools now have backend routes for link metadata fetch, Bilibili/YouTube video URL parsing, and remote image download, with authenticated access and business-effect tests.
- AI utilities now have backend routes for config testing, article summary, AI summary, and title generation via an OpenAI-compatible chat completions API using saved AI settings.
- Production defaults reject private-network URLs for remote fetches and AI base URLs; tests explicitly enable loopback for local fake-provider verification.
- Batch 5 decision: AI-generated article summaries reuse the existing persisted article `summary` field; no separate `ai_summary` article field is added. The admin article editor writes both summary-generation variants into `summary`, and article reload/public detail read the same persisted field.

### Public upload path consistency

- The public upload path used by the frontend must match the server's actual static or media path.
- If the UI can upload but the served asset path differs, the flow is functionally incomplete.
- The detailed execution source for this gap is now `UPLOAD-ASSET-CLOSED-LOOP` in `docs/EXECUTION_LOCK.md`.

## Medium gaps

These areas exist, but still rely too much on placeholders, hardcoded values, partial backend support, or deferred product decisions.

- Article view/stat counters: Batch 8 automated tests, clean Chrome running-stack smoke, and user acceptance now prove article-level `view_count` mutates from article pageviews and site PV avoids double counting.
- Admin list filters and pagination: Batch 8 automated tests and user acceptance now prove visit, article, comment, and file filter contracts for the core UI parameters.
- System data: CPU usage percentage, swap total/usage, DB size, DB connection count, and remote update source are explicitly deferred as `unsupported`; email and Feishu integrations are `disabled` until configured.
- RSS reader: manual refresh is verified; scheduled refresh and detailed friend-list source status display remain deferred.
- Import/export assets: Markdown import supports already-uploaded `/uploads/**` image references; Batch 8 UI now states this boundary and disables the misleading remote-image auto-download switch.
- Upload settings and export labels: Batch 8 UI now shows only local storage as currently effective, states the fixed 10MB backend limit, and describes article HTML export as basic HTML rather than full WeChat-specific formatting.
- AI summary length: generated title/summary persistence is closed, but provider output length is prompt-guided rather than strictly enforced.
- Search and SEO: DB-backed search is accepted for the online/default path; Elasticsearch should be added as optional Strategy code with indexing/rebuild/fallback tests, disabled by default for deployment.
- Mail/notification reliability and MQ: notification persistence is real and feedback produces notifications; broader event producers and retryable async mail/outbox behavior remain future work and should become part of the minimal PG+Debezium+MQ portfolio chain.
- Visit privacy: geo/browser/OS parsing can be stored/read for admin analysis, but public/frontend areas should not display privacy-adjacent visitor details such as commenter location, browser, OS, or IP.
- Test coverage: too many tests only assert that the envelope exists; too few assert the business effect.

## Batch 1 verification notes

- PASS: `mvn -f server/pom.xml -Dtest=BackendTruthDataBatchTest test` - 3 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 38 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- OBSERVED: `npm --prefix blog run type-check` emitted a local `vue-router/volar/sfc-route-blocks` dependency-resolution warning for `@vue/language-core`; no Batch 1 blog code was changed.
- ACCEPTED: user manually verified Batch 1 against the local running stack on 2026-05-15.

## Batch 2 verification notes

- PASS: `mvn -f server/pom.xml -Dtest=Batch2ContentAssetBatchTest test` - 7 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml -Dtest=P2ImportExportApiTest test` - 2 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml -Dtest=Phase4InteractionApiTest test` - 7 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 45 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- OBSERVED: `npm --prefix blog run type-check` completed with the existing local `vue-router/volar/sfc-route-blocks` warning for missing `@vue/language-core`.
- PASS: `npm --prefix admin run build`.
- PASS: `npm --prefix blog run build`.
- PASS: `docker compose up --build -d server admin blog`.
- PASS: Docker upload proxy check uploaded `/uploads/1778865616634_docker-proxy-check.png`; `localhost:8080`, `localhost:4000`, and `localhost:3000` all returned 200 with the same 31-byte file.
- PASS: Docker publish check created `docker-publish-check`, saved `is_publish=true` through `PUT /api/v1/admin/articles/{id}`, and public article lookup returned 200.
- PASS: `问题清单2.md` avatar follow-up verified homepage and about-page owner images render direct `/uploads/...` URLs instead of Nuxt IPX `/_ipx/_/uploads/...`, and the real avatar/photo files return 200 from `localhost:3000`, `localhost:4000`, and `localhost:8080`.
- PASS: `问题清单2.md` Markdown ZIP follow-up added a RED/GREEN export test; ZIP download now rewrites `/uploads/<file>` Markdown links to `assets/<file>` and includes the image bytes in the ZIP.
- ACCEPTED: user manually verified Batch 2 remediation fixes and checklist screens against the local running stack on 2026-05-16.

## Batch 3 verification notes

- RED observed: `mvn -f server/pom.xml -Dtest=Batch3UserTouchClosedLoopTest test` initially failed because `mail_outbox` did not exist, forgot-password was not public/implemented, and OAuth begin returned `401` instead of explicit unsupported.
- PASS: `mvn -f server/pom.xml -Dtest=Batch3UserTouchClosedLoopTest test` - 3 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 48 tests, 0 failures, 0 errors.
- ACCEPTED: user manually verified feedback/admin reply mail records, subscribe mail record, forgot/reset password, and OAuth deferred UI against the running app on 2026-05-16.
- OBSERVED: FlecBlog has no standalone frontend unsubscribe entry; current user-visible unsubscribe path is the email link contract, so full unsubscribe-link verification is deferred until real production mail delivery is enabled.

## Batch 4 verification notes

- RED observed: `mvn -f server/pom.xml -Dtest=Batch4RssReaderClosedLoopTest test` initially failed after the test setup was made H2-compatible because `POST /api/v1/admin/rssfeed/refresh` returned 404 and no fetch/import/status loop existed.
- PASS: `mvn -f server/pom.xml -Dtest=Batch4RssReaderClosedLoopTest test` - 2 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 50 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- ACCEPTED: user manually verified Batch 4 against the local running stack on 2026-05-17.
- DEFERRED: scheduled RSS refresh and detailed source status display in friend management.

## Batch 5 verification notes

- DECISION: reuse existing article `summary` for AI-generated summary text; do not add a separate `ai_summary` article field.
- RED observed: `mvn -f server/pom.xml -Dtest=Batch5AiArticleMetadataClosedLoopTest test` first failed after the test compiled because admin/blog frontend contracts still referenced the fake `ai_summary` article field.
- PASS: `mvn -f server/pom.xml -Dtest=Batch5AiArticleMetadataClosedLoopTest test` - 3 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 53 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- OBSERVED: `npm --prefix blog run type-check` completed with the existing local `vue-router/volar/sfc-route-blocks` warning for missing `@vue/language-core`.
- PASS: Docker running-stack AI title endpoint returned a real generated title using the saved DeepSeek-compatible AI settings; no API key was written to code, docs, tests, or logs.
- ACCEPTED: user manually verified DeepSeek AI config, AI title generation, summary generation, save/reopen persistence, and public article summary behavior in the running stack on 2026-05-17.

## Batch 6 verification notes

- DECISION: DB-backed search is accepted for v1; Elasticsearch is deferred as a later product enhancement and must not be fake-configured or fake-indexed.
- RED observed: `mvn -f server/pom.xml -Dtest=Batch6SearchSeoDepthTest test` failed after adding direct create-and-publish coverage because `POST /api/v1/admin/articles` with `is_publish=true` created a draft, leaving public search and SEO XML without the new article.
- PASS: `mvn -f server/pom.xml -Dtest=Batch6SearchSeoDepthTest test` - 2 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 55 tests, 0 failures, 0 errors.
- PASS: Docker running-stack verification against `localhost:8080` proved direct create-and-publish, public search, RSS, Atom, sitemap, update, draft exclusion, unpublish exclusion, republish, delete exclusion, and cleanup.
- ACCEPTED: user manually verified Batch 6 search and SEO behavior in the running stack on 2026-05-17.
- FOLLOW-UP: AI summary length prompt guidance and optional future search strategy design are deferred outside Batch 6.

## Batch 7 verification notes

- DECISION: Batch 7 was documentation-only audit/planning; no production code was changed and no full test suite was required.
- FINDING: article-level `view_count` is not closed. Article pages send `article_id` pageviews, but `VisitCollectionService` only inserts `visit_events` and does not update `articles.view_count`.
- FINDING: public/sidebar/admin stats are real enough for global visit data, but pageview semantics must be corrected to avoid duplicate article pageview tracking and double-counting `articles.view_count` plus `visit_events`.
- FINDING: several admin list pages expose filters/pagination that the backend does not fully honor yet, especially visits, articles, comments, and files.
- FINDING: import/export and upload settings are honest at baseline but still partial; UI labels/settings should be narrowed or backend behavior expanded.
- FINDING: AI summary length is prompt-guided, not enforced; this is a follow-up AI utility batch candidate.
- FINDING: Redis, Elasticsearch, RabbitMQ/Kafka, and Debezium/CDC are not present in the active stack; Redis and a minimal PG+Debezium+MQ flow should be implemented as required portfolio capabilities, while ES should be optional Strategy code with DB as default online search.
- RECOMMENDATION: next implementation batch should be `PRE-DEPLOYMENT-CORE-CLOSURE-BATCH-008`, combining article stats, admin filter honesty, import/export/settings honesty, and backend/admin-only visit geo/browser/OS parsing with public privacy masking.
- RECOMMENDATION: implement `REDIS-PG-DEBEZIUM-MQ-MINIMAL-BATCH-009` soon after core closure; Kafka should stay as a later explanation/proof item if it remains too complex.
- PENDING: user manual review and acceptance of the adjusted roadmap.

## Batch 8 verification notes

- DECISION: keep PostgreSQL as the source of truth for Batch 8 counters. Redis remains deferred to Batch 9 and must build on the now-explicit PostgreSQL semantics.
- RED observed: `mvn -f server/pom.xml -Dtest=Batch8PreDeploymentCoreClosureTest test` initially failed because article view count did not increment, admin visit/article/comment/file filters were not fully honored, and public comment privacy fields were exposed.
- PASS: `mvn -f server/pom.xml -Dtest=Batch8PreDeploymentCoreClosureTest test` - 5 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 60 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- PASS: `npm --prefix blog run type-check`; observed the existing local `@vue/language-core` warning from vue-router/volar.
- PASS: `npm --prefix admin run build`; observed existing large-chunk/config-script warnings.
- PASS: `npm --prefix blog run build`; observed existing Nuxt/dependency sourcemap and deprecation warnings.
- PASS: `docker compose up --build -d server admin blog`.
- PASS: Docker running-stack smoke verified `POST /api/v1/collect` returned 200 for article `hello-zblog`, `articles.view_count` increased from 2 to 3, and persisted `visit_events` pageviews increased from 190 to 191.
- MANUAL ISSUE 1: user observed that most article pages still showed `view_count=0`; only the previously smoked `hello-zblog` article showed `3`.
- ROOT CAUSE: the public article page only sent article-level pageviews when `route.params.slug` changed, not on direct open/refresh.
- PASS after fix: `npm --prefix blog run type-check`; existing `@vue/language-core` warning only.
- PASS after fix: `npm --prefix blog run build`; existing Nuxt/dependency warnings only.
- PASS after fix: `docker compose up --build -d blog`.
- PASS after fix: clean Chrome headless opened `http://localhost:3000/posts/test2`; `articles.view_count` increased 0 -> 1, then 1 -> 2 on the second open, while site persisted pageviews increased by exactly 1 for the second open.
- MANUAL ISSUE 2: user observed front article view count could stay one lower than admin/database after refresh.
- ROOT CAUSE: article header displayed its independent SSR pre-count article snapshot while the client-side tracker increment happened after render.
- PASS after issue 2 fix: `npm --prefix blog run type-check`; existing `@vue/language-core` warning only.
- PASS after issue 2 fix: `npm --prefix blog run build`; existing Nuxt/dependency warnings only.
- PASS after issue 2 fix: `docker compose up --build -d blog`.
- PASS after issue 2 fix: clean Chrome CDP opened `http://localhost:3000/posts/test2`; database `articles.view_count` increased 3 -> 4 and the front page DOM displayed `浏览量: 4`.
- MANUAL ISSUE 3: user observed admin visit log page showed "获取访问日志失败" on initial load.
- ROOT CAUSE: visit-list SQL concatenation produced `where 1 = 1order by ...` when no filters were supplied.
- PASS after issue 3 fix: `mvn -f server/pom.xml -Dtest=Batch8PreDeploymentCoreClosureTest test`; 5 tests, 0 failures, 0 errors.
- PASS after issue 3 fix: `mvn -f server/pom.xml test`; 60 tests, 0 failures, 0 errors.
- PASS after issue 3 fix: `docker compose up --build -d server`.
- PASS after issue 3 fix: running-stack authenticated `GET /api/v1/admin/stats/visits?page=1&page_size=20` returned 200 with `data.list`.
- MANUAL ISSUE 4: user observed admin file list showed all files as unused, including images visibly used by public pages.
- ROOT CAUSE: file list read static `files.status`; business references in settings/articles/comments/etc. did not update that field.
- PASS after issue 4 fix: file list dynamically computes effective usage status from persisted references in articles, settings, comments, feedbacks, users, friends, and moments.
- PASS after issue 4 fix: `mvn -f server/pom.xml -Dtest=Batch8PreDeploymentCoreClosureTest test`; 5 tests, 0 failures, 0 errors.
- PASS after issue 4 fix: `mvn -f server/pom.xml test`; 60 tests, 0 failures, 0 errors.
- PASS after issue 4 fix: running-stack authenticated `GET /api/v1/admin/files` for a real settings-referenced `站长形象` upload returned `status=1`.
- ACCEPTED: user manually verified Batch 8 after the issue 1-4 remediation loop on 2026-05-18.

## What to do next

1. Commit and push the accepted Batch 8 work.
2. Next portfolio batch should include Redis ranking/cache/rate-limit plus one minimal real PG+Debezium+MQ flow.

## Acceptance rule for future work

A change is not done until the live page or admin screen proves it.

That means:

- open the actual UI;
- perform the real action;
- reload or reopen;
- confirm the data still exists or renders correctly;
- keep a regression test that would fail if the backend reverted to a stub.
