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
- Compatibility endpoints for system admin and notifications that the current frontend expects.

## Critical gaps

These are the highest-priority mismatches between frontend calls and backend reality.

### Public auth/account/OAuth flows

- The frontend still expects account-related and OAuth-related behavior that is not fully closed end to end.
- Login/binding flows need route-by-route verification against the actual backend, not just the presence of auth screens.
- Any account UI that can render but cannot complete its backend round-trip remains open.

### Feedback and subscription flows

- Public feedback/subscribe-style features are not fully closed.
- If a page lets users submit data, the backend must persist it and make it observable after reload or in the admin area.
- If the backend only returns a placeholder or an empty acknowledgment, the flow is still open.

### Admin management areas that still need true implementations

The audit showed frontend calls or screens for these areas that still need backend closure or stronger verification:

- users
- feedback
- rssfeed
- subscribers
- tools
- AI-related admin utilities
- article import/export
- comment import

If a screen is present but the data path is only partially wired, it should stay on the open list until the real persistence or derivation is proven.

### Public upload path consistency

- The public upload path used by the frontend must match the server's actual static or media path.
- If the UI can upload but the served asset path differs, the flow is functionally incomplete.

## Medium gaps

These areas exist, but still rely too much on placeholders or hardcoded values.

- Notifications: the current behavior is compatible enough for some UI flows, but it needs stronger proof of real data handling.
- Visits and stats: several metrics are still hardcoded, derived only partially, or not backed by a real analytics pipeline.
- System data: some endpoints are compatibility shims rather than full implementations.
- Search and SEO: the main public endpoints exist, but some deeper analytics and ranking pieces still need follow-up if the product expects them.
- Test coverage: too many tests only assert that the envelope exists; too few assert the business effect.

## What to do next

1. Start from the frontend route or screen.
2. List the exact API calls it makes.
3. Check the backend for a real route, real persistence, and real reload behavior.
4. Split the result into:
   - closed loop
   - partial stub
   - missing backend route
   - UI-only placeholder
5. Fix the highest-impact open loops first.

## Acceptance rule for future work

A change is not done until the live page or admin screen proves it.

That means:

- open the actual UI;
- perform the real action;
- reload or reopen;
- confirm the data still exists or renders correctly;
- keep a regression test that would fail if the backend reverted to a stub.
