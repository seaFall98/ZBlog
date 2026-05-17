# Batch 10 Plan: Search Strategy + Outbox-Driven ES Indexing

## Summary

Batch 10 closes the search architecture gap without increasing default deployment cost. The default public search path remains PostgreSQL-backed. The batch adds a clean search strategy boundary, optional Elasticsearch adapter code disabled by default, and a lightweight application-outbox-driven article indexing chain.

This is intentionally honest: it provides the business effect of "database article changes flow asynchronously into a search index", comparable to the MySQL+Canal+ES idea at the workflow level, but it does not claim PostgreSQL WAL CDC/Debezium unless that is implemented in a later batch.

## Scope

- Add `SearchPort` / search adapter boundary.
- Keep DB search as the default adapter and online fallback.
- Add optional Elasticsearch adapter code behind config, disabled by default.
- Reuse or extend `event_outbox` for article search-index events.
- Emit search-index work from article create/update/publish/unpublish/delete when public search visibility changes.
- Add a consumer that applies outbox search events to the selected search indexer.
- Add a rebuild endpoint to reconstruct the derived search index from PostgreSQL.
- Add status/failure visibility so index problems are not silent.

## Out Of Scope

- No deployment hardening.
- No Redis+Nginx+Lua implementation.
- No Kafka.
- No Debezium or PostgreSQL WAL CDC.
- No FlecBlog parity recheck.
- No requirement to run Elasticsearch in the default local or online deployment.

## API Contracts

- Existing public search API remains backward compatible.
- `POST /api/v1/admin/search/reindex` rebuilds the derived search index from published PostgreSQL articles.
- `GET /api/v1/admin/search/status` returns selected strategy, fallback/default state, and recent indexing failure information if available.
- Existing article mutation APIs create durable search-index outbox events when they change public search visibility.

## Test Plan

- Add `Batch10SearchStrategyOutboxEsTest`.
- RED first: prove the search strategy/outbox indexing/reindex/status behavior is currently missing.
- Prove DB-backed search remains default and does not require Elasticsearch.
- Prove direct create-and-publish emits a search-index outbox event.
- Prove article update emits an index update event and public search eventually reflects changed searchable text.
- Prove unpublish/delete emits removal work and public search excludes the article.
- Prove reindex rebuilds only published searchable articles.
- Prove indexing failure is visible and retryable.

## Verification Commands

- `mvn -f server/pom.xml -Dtest=Batch10SearchStrategyOutboxEsTest test`
- `mvn -f server/pom.xml test`
- `npm --prefix blog run type-check` if the public search API contract changes.
- `npm --prefix admin run type-check` if admin search status/reindex UI is added.
- `docker compose up --build -d server blog admin` if UI or running-stack behavior changes.

## Manual Acceptance

- Search for an existing published article on the public blog; DB default search works without Elasticsearch.
- Create and publish an article, drain or wait for indexing work, then search for it.
- Update the article title/content, drain or wait for indexing work, then search by the new keyword.
- Unpublish or delete the article, drain or wait for indexing work, then confirm it disappears from public search.
- Trigger search reindex and confirm the public search result set remains correct.
- Confirm the system does not require an Elasticsearch container unless the optional strategy is explicitly enabled.

## Residual Decision

If the current admin UI has no natural place for search maintenance, Batch 10 may keep reindex/status as authenticated API-only endpoints and document manual API acceptance. A UI can be added later during deployment/admin polishing if the endpoint proves useful.
