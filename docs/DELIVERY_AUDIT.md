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

## Critical gaps

These are the highest-priority mismatches between frontend calls and backend reality.

### Upload and public asset delivery

- Admin upload exists at `POST /api/v1/admin/files`, stores file rows, and returns `/uploads/<filename>`.
- Static serving for `/uploads/**` exists, but the full rendered-page closed loop still needs proof.
- The public blog upload utility calls `POST /api/v1/upload`; Java currently has no matching public upload controller.
- This is the next recommended closed loop because it directly affects editor images, comment images, imported assets, and visible blog rendering.

### Visit collection and real statistics

- The blog tracker posts to `POST /api/v1/collect`.
- Java currently has no `/collect` endpoint.
- Several stats values are still zero, empty, or only partially derived: visitors, trend, visit logs, online users, daily/monthly pageviews.
- Completion requires a real persisted visit/event model and tests proving a collect call changes stats output.

### Notifications

- Admin notification endpoints exist but currently return an empty list and `ok(null)` read operations.
- Blog code also expects public notification endpoints.
- Completion requires real notification persistence, unread counts, read-state mutation, and at least one business event that creates a notification.

### Public auth/account/OAuth flows

- Database-backed login, register, refresh, profile update, password update, account deactivation, OAuth unbind, and admin user CRUD are now implemented and tested.
- True OAuth provider authorization callbacks are still not implemented and must remain explicitly out of scope until provider credentials and callback behavior are defined.
- Password reset endpoints are still expected by the frontend and need either a real token/mail flow or explicit UI deferral.

### Feedback and subscription flows

- Public feedback tickets, ticket lookup, admin feedback handling, email subscription, unsubscribe state, and admin subscriber management are now database-backed and tested.
- Mail delivery and feedback notification fan-out remain follow-up work; the current closure is the persistence and admin-observability baseline.

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

These areas exist, but still rely too much on placeholders or hardcoded values.

- Notifications: the current behavior is compatible enough for some UI flows, but it needs stronger proof of real data handling.
- Visits and stats: several metrics are still hardcoded, derived only partially, or not backed by a real analytics pipeline.
- System data: some endpoints are compatibility shims rather than full implementations.
- Article AI summary persistence: the AI endpoint returns text to the editor, but a separate persisted `ai_summary` article field is not yet implemented.
- Search and SEO: the main public endpoints exist, but some deeper analytics and ranking pieces still need follow-up if the product expects them.
- Test coverage: too many tests only assert that the envelope exists; too few assert the business effect.

## What to do next

1. Work only from the `Feature Completion Backlog` in `docs/EXECUTION_LOCK.md`.
2. Lock exactly one feature as the Active Work Slot.
3. Write and run the failing backend test before production code.
4. Implement the smallest Java backend slice needed to pass.
5. Verify the real UI manually before marking the loop closed.

## Acceptance rule for future work

A change is not done until the live page or admin screen proves it.

That means:

- open the actual UI;
- perform the real action;
- reload or reopen;
- confirm the data still exists or renders correctly;
- keep a regression test that would fail if the backend reverted to a stub.
