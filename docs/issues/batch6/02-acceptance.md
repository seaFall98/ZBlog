# Batch 6 Acceptance: Search and SEO Depth

## Status

Automated backend verification, Docker running-stack verification, and user manual acceptance passed.

## Product decision

DB-backed search is accepted for v1.

Elasticsearch is deferred as a later product enhancement. This batch does not add fake Elasticsearch configuration, fake indexing, or placeholder search abstractions.

## Automated verification

- RED observed: `mvn -f server/pom.xml -Dtest=Batch6SearchSeoDepthTest test` failed after adding direct create-and-publish coverage because `POST /api/v1/admin/articles` with `is_publish=true` created a draft, so public search and SEO XML did not include the newly created article.
- PASS: `mvn -f server/pom.xml -Dtest=Batch6SearchSeoDepthTest test` - 2 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 55 tests, 0 failures, 0 errors.
- Frontend type-check was not required because no frontend code changed.

## Docker running-stack verification

- PASS: rebuilt the server container with `docker compose up --build -d server`.
- PASS: verified against real `localhost:8080` APIs: direct create-and-publish, public search, RSS, Atom, sitemap, update, draft exclusion, unpublish exclusion, republish, delete exclusion, and cleanup.
- Evidence keywords: `batch6-docker-initial-1778959750`, `batch6-docker-updated-1778959750`, `batch6-docker-draft-1778959750`.

## Manual acceptance steps

Use a unique keyword, for example `batch6-manual-<timestamp>`.

1. Start or refresh the local Docker stack.
2. Open the admin article editor.
3. Create and publish a new article whose title, summary, and body all contain the unique keyword.
4. Open the public blog search page and search the unique keyword.
5. Confirm the newly published article appears in search results.
6. Edit the same article and replace the unique keyword in title, summary, and body with a second unique keyword.
7. Search the old keyword and confirm the article no longer appears.
8. Search the second keyword and confirm the article appears with the updated title/content.
9. Open `/rss.xml`, `/atom.xml`, and `/sitemap.xml` in the browser.
10. Confirm the updated published article appears in the XML outputs.
11. Save another article as a draft with a third unique keyword.
12. Confirm the draft keyword does not appear in public search, `/rss.xml`, `/atom.xml`, or `/sitemap.xml`.
13. Unpublish or delete the published test article.
14. Confirm the unpublished/deleted article no longer appears in public search, `/rss.xml`, `/atom.xml`, or `/sitemap.xml`.
15. Confirm the v1 decision: search is DB-backed for now, and Elasticsearch is deferred.

## Manual acceptance result

ACCEPTED by the user on 2026-05-17.

User confirmed Batch 6 search and SEO acceptance passed. Two non-blocking follow-up suggestions were raised:

- AI-generated summary length can exceed the current intended 300-character limit; consider tightening default prompts or product length guidance in a later AI utilities batch.
- Elasticsearch remains deferred because it is heavy for the current local/personal-development scope. A future search enhancement can consider a strategy-style design where DB-backed search remains the default and Elasticsearch is enabled by configuration only when intentionally adopted.
