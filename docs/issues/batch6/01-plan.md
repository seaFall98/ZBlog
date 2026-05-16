# Batch 6 Plan: Search and SEO Depth

## Batch ID

`SEARCH-SEO-DEPTH-BATCH-006`

## Scope

Close the v1 search and SEO data-change loop for real article data.

In scope:

- Public article search.
- `rss.xml`.
- `atom.xml`.
- `sitemap.xml`.
- Article create, update, publish, unpublish, and delete effects on search/SEO output.
- Explicit v1 decision that DB search is accepted for now.

Out of scope:

- Deployment hardening.
- FlecBlog parity recheck.
- Elasticsearch implementation.
- Reworking Batch 1-5 unless a direct regression is found.

## Product decision

DB-backed search is accepted for v1. Elasticsearch is deferred as a later product enhancement. Do not add fake ES config, fake indexing, or placeholder search abstractions.

## Frontend/admin entry pages

- Admin article editor/list creates, updates, publishes, unpublishes, and deletes articles through admin article APIs.
- Public blog search page consumes `GET /api/v1/articles/search`.
- Browser-accessible XML outputs consume `/rss.xml`, `/atom.xml`, and `/sitemap.xml`.

## Backend API calls

- `POST /api/v1/admin/articles`
- `PUT /api/v1/admin/articles/{id}`
- `POST /api/v1/admin/articles/{id}/publish`
- `POST /api/v1/admin/articles/{id}/unpublish`
- `DELETE /api/v1/admin/articles/{id}`
- `GET /api/v1/articles/search?keyword=...`
- `GET /rss.xml`
- `GET /atom.xml`
- `GET /sitemap.xml`

## Data path

- Articles are stored in `articles`.
- Public search should query only `status = 'PUBLISHED'` rows and match real article title, summary, and rendered text.
- SEO XML outputs should query only `status = 'PUBLISHED'` rows.
- No long-lived SEO/search cache is introduced in this batch; outputs derive fresh DB state on each request.

## Tests first

Create a focused integration test class for Batch 6 that proves:

1. Draft articles are not searchable and do not appear in RSS, Atom, or sitemap.
2. Publishing an article makes it searchable and adds it to RSS, Atom, and sitemap.
3. Updating title, summary, and body changes search results and SEO XML output.
4. Unpublishing removes the article from public search and SEO XML output.
5. Deleting removes the article from public search and SEO XML output.
6. SEO XML includes only real published article URLs/content, not hardcoded/fake records.

The initial RED should expose whichever behavior is currently insufficient, without asserting only HTTP 200.

## Verification commands

- `mvn -f server/pom.xml -Dtest=Batch6SearchSeoDepthTest test`
- `mvn -f server/pom.xml test`
- If frontend code changes: `npm --prefix admin run type-check` and/or `npm --prefix blog run type-check`
- Docker running-stack verification for search and XML outputs.

## Acceptance handoff

After automated verification, create `docs/issues/batch6/02-acceptance.md` and give the user concrete manual acceptance steps. Do not push or report the batch as final until the user confirms acceptance.
