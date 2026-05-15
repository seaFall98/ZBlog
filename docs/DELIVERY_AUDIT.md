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
- Import/export baseline: admin article import, WeChat export, Markdown ZIP download, and comment import.
- Compatibility endpoints for system admin and notifications that the current frontend expects.
- Admin tools and AI utility endpoints: link metadata, video parsing, remote image download, AI config test, summary, AI summary, and title generation.
- Batch 1 backend truth data: visit collection/stat derivation, persisted notifications/read-state, and honest system info are automated-verified and user-accepted in the local running stack.

## Critical gaps

These are the highest-priority mismatches between frontend calls and backend reality.

### Upload and public asset delivery

- Admin upload exists at `POST /api/v1/admin/files`, stores file rows, and returns `/uploads/<filename>`.
- Static serving for `/uploads/**` exists, but the full rendered-page closed loop still needs proof.
- The public blog upload utility calls `POST /api/v1/upload`; Java currently has no matching public upload controller.
- This is the next recommended closed loop because it directly affects editor images, comment images, imported assets, and visible blog rendering.

### Visit collection and real statistics

- Status: automated verified and manually accepted.
- The blog tracker posts to `POST /api/v1/collect`, and Java now persists tracker payloads into `visit_events`.
- `/api/v1/stats/site`, `/api/v1/admin/stats/dashboard`, `/api/v1/admin/stats/trend`, and `/api/v1/admin/stats/visits` now derive visit-related values from persisted visit events.
- Evidence: `mvn -f server/pom.xml -Dtest=BackendTruthDataBatchTest test` passes and proves a collect call changes site/dashboard/trend/visit-log output.
- Deferred: visit-log geo location, browser parsing, and OS parsing return explicit `unsupported`.

### Notifications

- Status: automated verified and manually accepted.
- Admin and public notification endpoints now share persisted `notifications` rows with pagination, `unread_count`, single-read, and read-all mutations.
- Feedback submission now creates a real `feedback_new` notification.
- Evidence: `BackendTruthDataBatchTest#feedbackCreatesNotificationAndReadOperationsUpdateUnreadCount` proves unread count changes after read operations.

### Public auth/account/OAuth flows

- Database-backed login, register, refresh, profile update, password update, account deactivation, OAuth unbind, and admin user CRUD are now implemented and tested.
- True OAuth provider authorization callbacks are still not implemented and must remain explicitly out of scope until provider credentials and callback behavior are defined.
- Password reset endpoints are still expected by the frontend and need either a real token/mail flow or explicit UI deferral.

### Feedback and subscription flows

- Public feedback tickets, ticket lookup, admin feedback handling, email subscription, unsubscribe state, and admin subscriber management are now database-backed and tested.
- Feedback notification fan-out into the in-app notification table is now covered by Batch 1.
- Mail delivery remains follow-up work; the current closure is persistence, admin observability, and in-app notification fan-out.

### Admin tools and AI utilities

- Admin tools now have backend routes for link metadata fetch, Bilibili/YouTube video URL parsing, and remote image download, with authenticated access and business-effect tests.
- AI utilities now have backend routes for config testing, article summary, AI summary, and title generation via an OpenAI-compatible chat completions API using saved AI settings.
- Production defaults reject private-network URLs for remote fetches and AI base URLs; tests explicitly enable loopback for local fake-provider verification.
- AI-generated article summaries can be returned to the editor, but persisting a distinct `ai_summary` article field still needs schema/API work if the product requires it beyond the current article `summary` field.

### Public upload path consistency

- The public upload path used by the frontend must match the server's actual static or media path.
- If the UI can upload but the served asset path differs, the flow is functionally incomplete.
- The detailed execution source for this gap is now `UPLOAD-ASSET-CLOSED-LOOP` in `docs/EXECUTION_LOCK.md`.

## Medium gaps

These areas exist, but still rely too much on placeholders, hardcoded values, or deferred product decisions.

- System data: CPU usage percentage, swap total/usage, DB size, DB connection count, and remote update source are explicitly deferred as `unsupported`; email and Feishu integrations are `disabled` until configured.
- Article AI summary persistence: the AI endpoint returns text to the editor, but a separate persisted `ai_summary` article field is not yet implemented.
- Search and SEO: the main public endpoints exist, but some deeper analytics and ranking pieces still need follow-up if the product expects them.
- Test coverage: too many tests only assert that the envelope exists; too few assert the business effect.

## Batch 1 verification notes

- PASS: `mvn -f server/pom.xml -Dtest=BackendTruthDataBatchTest test` - 3 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 38 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- OBSERVED: `npm --prefix blog run type-check` emitted a local `vue-router/volar/sfc-route-blocks` dependency-resolution warning for `@vue/language-core`; no Batch 1 blog code was changed.
- ACCEPTED: user manually verified Batch 1 against the local running stack on 2026-05-15.

## What to do next

1. Start the next batch from the `Feature Completion Backlog` in `docs/EXECUTION_LOCK.md`.
2. Lock exactly one batch or feature group as the Active Work Slot.
3. Write and run failing backend tests before production code.
4. Implement the smallest backend/frontend slice needed to pass.
5. Verify the real UI manually before marking the batch closed.

## Acceptance rule for future work

A change is not done until the live page or admin screen proves it.

That means:

- open the actual UI;
- perform the real action;
- reload or reopen;
- confirm the data still exists or renders correctly;
- keep a regression test that would fail if the backend reverted to a stub.
