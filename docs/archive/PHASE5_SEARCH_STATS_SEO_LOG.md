# Phase 5 Search, Stats And SEO Log

## Scope

- Add public article search for the blog search modal.
- Add public site stats and archive stats for sidebar/about/statistics pages.
- Add admin dashboard, trend, category, tag, contribution and visit stats endpoints.
- Add RSS, Atom and sitemap XML endpoints for SEO and subscriptions.
- Keep the implementation database-backed first, leaving Elasticsearch and real PV/UV analytics as later enhancements.

## Completed

- Added `GET /api/v1/articles/search` with published-article filtering and keyword matching across title, summary and text content.
- Added `GET /api/v1/stats/site` and `GET /api/v1/stats/archives`.
- Added admin stats endpoints under `/api/v1/admin/stats/**`.
- Added `GET /rss.xml`, `GET /atom.xml` and `GET /sitemap.xml`.
- Updated security rules so public search, public stats and SEO feeds are accessible without JWT.
- Added Phase 5 integration tests covering public search, public stats, admin stats and XML feeds.

## Verification

```text
mvn -Dtest=Phase5SeoSearchStatsApiTest test
Result: 4 tests, 0 failures

mvn test
Result: 15 tests, 0 failures

blog: npm run type-check
Result: success, with existing Vue Router/Volar route-block warning

admin: npm run type-check
Result: success
```

## Follow-Up

- Replace LIKE search with Elasticsearch once content volume justifies it.
- Add real visit tracking persistence for PV, UV, trend and visit-log endpoints.
- Generate absolute sitemap/feed URLs after the production domain is confirmed.
- Add robots.txt and Open Graph/Twitter-card metadata when final site identity assets are ready.
