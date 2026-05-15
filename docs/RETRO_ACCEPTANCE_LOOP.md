# Retrospective: Acceptance Loop Incident

## What happened

Phase labels and partial endpoint work were treated as stronger evidence of completion than they really were. That made some work look done even though the real frontend page could not always complete its business flow against the backend.

## Why this happened

- The project used phase progress as a proxy for delivery.
- Some checks focused on code shape, route presence, or envelope format instead of real user flows.
- Several tests verified that a response existed, but not that the result persisted or remained correct after reload.
- Frontend/backend contract drift was allowed to accumulate across admin and public screens.

## What failed in the process

- No hard rule forced a live UI round-trip before calling a task complete.
- Stub responses and compatibility shims were not clearly separated from real implementations in reporting.
- Documentation described intended state, but the repo was not required to prove that state at runtime.
- Completion reporting was too optimistic when only one side of a contract had been updated.

## What this incident taught us

A feature is only real when the frontend can use it end to end and the data survives a refresh.

That applies to:

- content editing and rendering;
- admin CRUD screens;
- public submission flows;
- derived data like stats, feeds, and summaries;
- compatibility endpoints that were added to keep the UI moving.

## Safeguards now required

- Define the exact page or screen before starting implementation.
- Trace the exact backend route and data path it needs.
- Verify the happy path in the running app.
- Verify the state after reload or reopen.
- Add or update a test that checks the business effect, not only the response structure.
- Do not mark a phase, ticket, or subtask complete while any part of the flow still depends on a placeholder.

## Outcome

The repo guidance now treats real UI/API/data closure as the completion bar. That is the rule that should prevent another phase from being reported as done before it is actually usable.
