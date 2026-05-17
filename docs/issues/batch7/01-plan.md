# Batch 7 Plan - Pre-deployment Feature and Technology Audit

## Batch ID

`PRE-DEPLOYMENT-FEATURE-TECH-AUDIT-BATCH-007`

## Goal

Before deployment hardening, run an objective audit of remaining user-visible feature gaps and planned backend technology-stack gaps.

This batch is intentionally an audit/planning batch. It must not implement Redis, Elasticsearch, MQ, CDC, or deployment code unless the user explicitly changes the scope.

## Why This Batch Exists

Batch 1-6 closed many major loops, but manual use still revealed at least one visible base-feature gap: article `view_count` appears to remain `0` on the public article page.

The earlier backend refactor plan also included Redis, RabbitMQ, optional Elasticsearch, PostgreSQL CDC/Debezium, lightweight DDD, Strategy/Adapter/Event/Outbox patterns, and tests. Those ideas still have value, but they must be re-evaluated against current code and deployment cost instead of being added only for README richness.

## Reference Inputs

- Current repository code.
- `docs/EXECUTION_LOCK.md`.
- `docs/DELIVERY_AUDIT.md`.
- `docs/AI_CODING_PLAYBOOK.md`.
- FlecBlog reference behavior.
- Old backend refactor plan: `D:\MyCode\ZBlogProject\ZBlog_1\docs\06_BACKEND_REFACTOR_PLAN.md`.
- min-lj Blog may be used as a learning reference for a richer Java blog stack, but do not copy it blindly.

## Audit Scope

### User-visible feature audit

Check at least:

- public article detail and `view_count`;
- public site stats/sidebar stats;
- public search, RSS, Atom, and Sitemap;
- comments, feedback, subscriptions, notifications;
- admin dashboard, article list, visit list, settings, files, RSS reader, and system pages;
- import/export deferred gaps;
- AI summary length and saved-field behavior;
- any UI path that still displays placeholder, `0`, empty list, unsupported behavior, or fake success.

### Technology-stack audit

Evaluate:

- Redis candidates beyond article `view_count`;
- Elasticsearch strategy implementation with DB fallback;
- RabbitMQ/Kafka candidates;
- PostgreSQL logical decoding / pgoutput / Debezium feasibility;
- outbox/event pattern scope;
- deployment cost and whether the feature should happen before or after deployment.

## Required Output

Update `docs/EXECUTION_LOCK.md` and, if useful, `docs/DELIVERY_AUDIT.md` with:

- must-fix-before-deploy list;
- optional portfolio/architecture enhancement list;
- post-deploy deferred list;
- recommended Batch 8+ order;
- explicit product/architecture decisions for Redis, ES, MQ, and CDC.

## Decision Criteria

A technology is allowed before deployment only if:

- it closes a real blog behavior or operational requirement;
- it has a clear fallback/degradation story;
- it can be tested locally without relying on public networks;
- it does not make low-cost deployment unrealistic;
- it can be explained in an interview without hand-waving.

## Non-goals

- Do not implement Redis in this batch.
- Do not implement Elasticsearch in this batch.
- Do not implement RabbitMQ/Kafka/Debezium/CDC in this batch.
- Do not perform deployment hardening in this batch.
- Do not rewrite architecture just to match another open-source project.

## Acceptance

The user accepts this batch when the adjusted roadmap is understandable, concrete, and credible enough to guide the next implementation batch without random regrouping.
