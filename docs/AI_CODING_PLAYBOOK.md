# AI Coding Workflow Playbook

This playbook records the current AI-assisted development workflow learned from ZBlog. It is guidance, not permanent law. Update it when future projects reveal a better pattern.

The goal is to make AI coding work visible, verifiable, and sustainable without turning every task into a heavy release ceremony.

## 1. Positioning

AI coding does not remove project management. It makes missing project management fail faster.

Use this playbook when a project has enough complexity that simple prompting is no longer reliable:

- multiple frontend/backend/data flows;
- more than one AI agent or tool;
- manual acceptance is needed;
- false completion would be expensive;
- deployment or persistent data matters.

For small isolated fixes, use the spirit of this playbook without copying every document.

## 2. Core Lessons

### Phase is too broad

Large phases are easy for agents to overclaim. A phase can look complete when routes exist, type-check passes, or docs say done, while the real page still uses placeholders or broken persistence.

Avoid using phase names as completion evidence.

### Single-feature verification can be too expensive

Very small features are easier to reason about, but running Docker, manual acceptance, repair, and documentation after every tiny feature can exhaust both humans and tokens.

Do not turn every small feature into a full release loop.

### MVP must not mean toy

Reducing scope is healthy. Returning fake data, hiding broken flows behind `ok(null)`, or shipping UI that pretends unsupported features work is not healthy.

The better target is a production-quality slice: fewer capabilities, but every exposed capability is real.

### Human visibility matters

If an agent only receives instructions and returns final results, the project becomes a black box. Humans need to see enough of the agent's plan, failed attempts, root causes, and fixes to steer the engineering direction.

That is why `docs/issues/` exists.

## 3. Work Unit Model

Use four levels:

```text
Issue -> Feature/Task -> Batch -> Acceptance
```

### Issue

An issue is the human-facing work request or problem statement. It explains why the work exists.

Good issues include:

- user-visible problem or desired capability;
- affected pages or flows;
- expected behavior;
- constraints and non-goals;
- acceptance hints.

For AI agents, an issue should be specific enough that the agent can plan without guessing the product intent.

### Feature or task

A feature/task is the smallest meaningful engineering closure inside an issue.

Examples:

- implement password reset token lifecycle;
- make `/api/v1/collect` persist visits;
- expose uploaded files through all local origins;
- parse and store RSS items from a feed source.

Each feature/task should have at least one testable business effect.

### Batch

A batch groups related features/tasks to reduce heavy verification cost.

Batch exists to avoid repeating Docker rebuilds, manual browser acceptance, fix reports, and context handoffs after every tiny feature.

Recommended batch shape:

- 1 large feature, or 2-4 closely related smaller features;
- shared product goal;
- shared manual acceptance path;
- low file-conflict risk;
- clear out-of-scope guard.

Bad batch shape:

- unrelated backend features;
- “everything left in the roadmap”;
- one batch that requires many different product decisions;
- a batch so broad that tests cannot prove completion.

### Acceptance

Acceptance is where the work becomes real:

- automated tests prove business effects;
- running stack proves integration;
- human verifies the actual page or user flow;
- docs record both completed behavior and deferred items.

## 4. Documentation System

Use three document layers.

### Execution Lock

`docs/EXECUTION_LOCK.md` is the current execution source of truth.

It should answer:

- what is the active batch;
- what is included and excluded;
- which APIs/pages/data paths are involved;
- what has closed;
- what is next.

If an old plan conflicts with the execution lock, the execution lock wins.

### Delivery Audit

`docs/DELIVERY_AUDIT.md` is the delivery truth source.

It should answer:

- what is truly closed;
- what is partial;
- what is fake-completion risk;
- what evidence proves completion;
- what is explicitly deferred.

Do not mark a feature complete because code exists. Mark it complete because behavior is verified.

### Issues Directory

`docs/issues/` is the human-readable process layer.

It is not a replacement for the execution lock or delivery audit. It explains the work process:

- what the agent planned;
- what acceptance steps the human should run;
- what failed during acceptance;
- why it failed;
- how it was fixed;
- what remains risky or deferred.

Recommended layout:

```text
docs/issues/batchN/
  01-plan.md
  02-acceptance.md
  03-issues.md
  04-fix-report.md
  05-final-report.md
```

This keeps AI work legible to humans. It also lets humans correct the agent's engineering approach before the project becomes a black box.

## 5. Suggested Batch Workflow

Use this as the default loop:

1. Create or identify the issue.
2. Split the issue into concrete features/tasks.
3. Group related features/tasks into one batch.
4. Lock the batch in `docs/EXECUTION_LOCK.md`.
5. Write `docs/issues/batchN/01-plan.md`.
6. Write the first failing business test for each feature/task.
7. Implement the minimum production code to pass.
8. Run targeted tests during development.
9. Run full backend tests and relevant frontend checks at batch end.
10. Write `docs/issues/batchN/02-acceptance.md`.
11. Ask the human to manually verify the real UI/API flow.
12. If acceptance fails, record it in `03-issues.md`, fix it, and write `04-fix-report.md`.
13. After human acceptance, update `DELIVERY_AUDIT.md`, write `05-final-report.md`, then commit/push/report.

This order can be lightened for small work, but do not skip the core idea: test before implementation, human acceptance before final reporting.

## 6. Testing Strategy

Use TDD as a truth mechanism, not as ceremony.

Each feature/task should start with a minimal failing test that proves the current gap. The test should fail because behavior is missing, not because of typos or setup noise.

Good tests prove business effects:

- a collect request changes stats;
- marking a notification read decreases unread count;
- uploading a file creates a DB row and serves real bytes;
- resetting a password invalidates the old password;
- refreshing an RSS source stores items and avoids duplicates.

Weak tests only prove surface shape:

- HTTP 200;
- non-null response;
- empty array accepted;
- controller route exists;
- envelope has `code: 0`.

During implementation, run targeted tests. At batch end, run the heavier checks once:

- relevant targeted tests;
- full backend test suite;
- frontend type-check/build if contracts changed;
- Docker/running-stack validation when integration matters;
- manual acceptance.

## 7. Human Acceptance

Manual acceptance should not make the human act as CI.

The agent should provide exact steps:

- URL or page;
- setup data;
- user action;
- expected result;
- reload/reopen check;
- known deferred items.

The human should focus on product reality:

- does the page work;
- does saved data survive;
- does the UI tell the truth;
- is the solution acceptable;
- is the deferred item really okay.

If manual acceptance fails, record the failure before moving on. A useful failure record includes:

- phenomenon;
- expected behavior;
- confirmed cause;
- solution;
- key code;
- verification;
- residual risk.

## 8. Agent Collaboration Modes

No single agent pattern is mandatory.

### Single-agent mode

One agent plans, implements, tests, and reports.

Good for:

- small projects;
- isolated bugs;
- low-risk changes;
- short-lived context.

Risk:

- the same agent may miss its own false assumptions.

### Two-agent mode

One agent controls roadmap/audit, another implements code.

Good for:

- medium-complexity projects;
- high false-completion risk;
- humans who want stronger review separation.

ZBlog currently used:

```text
Codex: global control, audit, prompts, docs, acceptance framing.
Claude Code: implementation.
Human: product judgment and manual acceptance.
```

This is an exploration pattern, not a universal rule.

### Parallel-agent mode

Multiple agents work at the same time.

Use only when:

- tasks are independent;
- write sets do not overlap;
- docs and active batch are locked;
- tests can catch integration mistakes;
- one agent is not silently changing the other agent's assumptions.

Avoid parallel coding when the project still lacks a stable execution lock or when multiple agents would edit the same backend domains and docs.

## 9. False Completion Guardrails

Before accepting a batch, check:

- Is there a real page or API entry?
- Is the backend behavior real, not a shell?
- Is data persisted or derived from real persisted data?
- Does reload/reopen still work?
- Does a test prove the business effect?
- Are unsupported paths explicit instead of hidden?
- Are deferred items documented?
- Did the human manually accept the flow?

Red flags:

- `ok(null)` for user-facing actions;
- empty arrays used as success;
- hardcoded counts;
- fake dashboard values;
- route exists but no persistence;
- generated content exists only in frontend state;
- docs say complete but code cannot prove it.

## 10. Commit and Safety Rules

Keep commits intentional.

Recommended:

- commit after a batch is accepted;
- include tests and docs that prove the batch;
- keep local scratch files out of commits;
- push only after human acceptance if the project uses that flow.

Never commit:

- API keys;
- tokens;
- passwords;
- cookies;
- private logs;
- `.claude/` or local tool settings unless intentionally shared;
- temporary issue notes before they are cleaned and redacted.

If secrets were used during manual validation, record only the behavior, not the secret.

## 11. Prompt Template

Use this shape for implementation prompts:

```text
Continue the project. Pull latest main first.

Read:
1. docs/README.md
2. docs/EXECUTION_LOCK.md
3. docs/DELIVERY_AUDIT.md
4. docs/issues/README.md
5. CLAUDE.md or project agent instructions

Current batch:
<BATCH-ID>

Goal:
<one clear goal>

Scope:
- <included feature/task>
- <included feature/task>

Out of scope:
- <explicit non-goal>
- <explicit non-goal>

Requirements:
1. Write RED tests first.
2. Prove business effects, not only HTTP 200.
3. Update only the approved docs.
4. Provide manual acceptance steps before final push/report.

Verification:
- targeted tests
- full backend tests
- frontend checks if contract changed
- Docker/running-stack check if integration matters

Do not claim completion until user acceptance passes.
```

## 12. Continuous Improvement

This playbook should evolve.

After each project or major batch, ask:

- Did the batch size feel too big or too small?
- Did tests catch real problems?
- Did manual acceptance find issues automation missed?
- Did issues records help humans understand the agent?
- Did docs stay slim enough?
- Did agent prompts leave too much room for guessing?
- Did any rule create unnecessary friction?

Update the playbook when the answer reveals a better workflow.

The current lesson from ZBlog is:

```text
AI coding works best when humans design a visible, testable, batchable work system around the agent.
```

