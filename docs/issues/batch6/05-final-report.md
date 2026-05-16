# Batch 6 Final Report

## Status

Batch 6 is user accepted and ready to commit/push.

## Batch ID

`SEARCH-SEO-DEPTH-BATCH-006`

## Product decision

DB-backed search is accepted for v1.

Elasticsearch is deferred as a later product enhancement. This batch does not add fake Elasticsearch configuration, fake indexing, or placeholder search abstractions.

## Accepted behavior

- Creating an article with `is_publish=true` publishes it immediately.
- Published articles appear in public search.
- Draft articles do not appear in public search.
- Updating an article's title, summary, or Markdown body changes public search results.
- Published articles appear in `/rss.xml`, `/atom.xml`, and `/sitemap.xml`.
- Draft articles do not appear in `/rss.xml`, `/atom.xml`, or `/sitemap.xml`.
- Unpublishing removes an article from public search and SEO XML outputs.
- Deleting removes an article from public search and SEO XML outputs.
- Search and SEO XML derive from real `articles` data and published state, with no long-lived cache introduced in this batch.

## Implementation notes

- `ArticleService.create` now honors the existing admin article `is_publish` request flag by publishing the newly created article before returning it.
- Existing DB-backed public search remains the v1 strategy.
- Existing SEO XML generation remains DB-backed and fresh per request.
- No frontend code changed in this batch.

## Automated verification

- RED observed: `mvn -f server/pom.xml -Dtest=Batch6SearchSeoDepthTest test` failed after adding direct create-and-publish coverage because `POST /api/v1/admin/articles` with `is_publish=true` created a draft, leaving public search and SEO XML without the new article.
- PASS: `mvn -f server/pom.xml -Dtest=Batch6SearchSeoDepthTest test` - 2 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 55 tests, 0 failures, 0 errors.
- Frontend type-check was not required because no frontend code changed.

## Docker running-stack verification

- PASS: rebuilt the server container with `docker compose up --build -d server`.
- PASS: verified against real `localhost:8080` APIs: direct create-and-publish, public search, RSS, Atom, sitemap, update, draft exclusion, unpublish exclusion, republish, delete exclusion, and cleanup.
- Evidence keywords: `batch6-docker-initial-1778959750`, `batch6-docker-updated-1778959750`, `batch6-docker-draft-1778959750`.

## Manual acceptance

ACCEPTED by the user on 2026-05-17.

The user confirmed Batch 6 search and SEO acceptance passed.

## Follow-up suggestions outside Batch 6

- AI-generated summary length can exceed the current intended 300-character limit. Consider tightening default prompt wording or product length guidance in a later AI utilities batch.
- Elasticsearch remains deferred because it is heavy for the current local/personal-development scope. A future search enhancement can consider a strategy-style design where DB-backed search remains the default and Elasticsearch is enabled by configuration only when intentionally adopted.

## Commit and push

Pending at the time this file was written. Fill in after commit/push:

- Commit: pending
- Push target: `origin/main`
