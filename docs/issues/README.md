# Issues and Acceptance Records

This directory keeps human-facing delivery records for each batch. It is not a replacement for `docs/EXECUTION_LOCK.md` or `docs/DELIVERY_AUDIT.md`.

## Purpose

Use this directory to preserve the full loop that is easy to read later:

1. plan;
2. implementation evidence;
3. manual acceptance steps;
4. acceptance failure records;
5. fix records;
6. final acceptance and work report.

`docs/EXECUTION_LOCK.md` remains the current execution source of truth. `docs/DELIVERY_AUDIT.md` remains the audit/evidence source. Files here are narrative records for review, handoff, and learning from failed manual acceptance.

## Required batch layout

For each batch, create or maintain:

```text
docs/issues/batchN/
  01-plan.md
  02-acceptance.md
  03-issues.md        # required if manual acceptance fails
  04-fix-report.md    # required after fixing a failed acceptance
  05-final-report.md
```

Existing older files may keep their original names, but new batches should use the numbered layout.

## Workflow

### 1. Plan

Before implementation, record the scoped batch plan in `01-plan.md`:

- batch ID and scope;
- product decision needed, if any;
- frontend/admin entry pages;
- backend API calls;
- persistence or derived data path;
- tests to write first;
- out-of-scope items.

### 2. Implementation and automated verification

During implementation, keep authoritative status in `docs/EXECUTION_LOCK.md`. When automated verification is ready, record the user-facing acceptance procedure in `02-acceptance.md`:

- exact local stack commands or URLs;
- setup/configuration values, excluding secrets;
- UI actions to perform;
- expected results after save, reload, or reopen;
- pass/fail criteria;
- known deferred items that are not acceptance failures.

### 3. If manual acceptance fails

If the user reports a failed acceptance, create or update `03-issues.md` before continuing to the next batch. Each issue must use this structure:

```md
## Issue <number>: <short title>

### Phenomenon

What the user saw, including page, action, visible error, screenshot references, and exact endpoint if known.

### Expected behavior

What should have happened according to the acceptance steps.

### Cause

The confirmed technical root cause. If still investigating, mark it as `Investigating` and update it once proven.

### Solution

What changed to fix the cause, not just how the symptom disappeared.

### Key code

- `path/to/file.ext:line` — important function, endpoint, component, test, or config mapping.

### Verification

Commands and/or real UI checks that prove the fix.

### Residual risk or deferred work

Anything intentionally not solved in this fix.
```

### 4. Fix report

After fixing a failed acceptance, create or update `04-fix-report.md`:

- list fixed issues;
- summarize root causes and fixes;
- list changed files by area;
- record targeted tests, full tests, type-check/build, and running-stack checks;
- provide the updated manual acceptance steps.

### 5. Final report

Only after the user confirms manual acceptance, create or update `05-final-report.md`:

- commit hash after commit;
- push target after push;
- final accepted behavior;
- automated verification evidence;
- manual acceptance confirmation date;
- remaining deferred items;
- next batch prompt or next concrete step, if requested.

## Rules

- Never store API keys, tokens, passwords, cookies, or private credentials in this directory.
- Do not paste full secret-bearing request/response logs. Redact secrets and keep only the diagnostic fields needed.
- Do not mark acceptance as passed until the user explicitly confirms it.
- If acceptance fails, do not skip the issue record. The failure record is part of the deliverable.
- Keep file names and sections stable so future agents can quickly scan the history.
- Keep root `docs/` slim; detailed issue narratives belong here.
