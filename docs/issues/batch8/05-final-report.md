# Batch 8 Final Report: Pre-deployment Core Closure

## Final Status

Batch 8 is accepted by the user on 2026-05-18.

The batch closed the pre-deployment core gaps without adding Redis, Elasticsearch, MQ, CDC, or deployment hardening. Those remain intentionally scheduled for later batches.

## Delivered Scope

- Article pageviews now increment the persisted article `view_count`.
- Site PV now uses only persisted `visit_events` pageviews and does not double count article aggregate counters.
- Article page tracking avoids generic route double counting on `/posts/**`.
- Public article header now shows the post-increment shared article count instead of a stale SSR snapshot.
- Admin visit, article, comment, and file lists now honor the core visible filters and pagination contracts.
- Admin visit logs parse browser and OS from User-Agent; location remains explicitly `unsupported`.
- Public comment responses and public UI no longer expose location, browser, or OS.
- Upload settings and import/export UI now describe the real local-storage and import/export capability boundaries.
- File list usage status now dynamically reflects persisted references from articles, settings, comments, feedbacks, users, friends, and moments.

## Remediation Loop

Manual acceptance found four real issues:

- Issue 1: article pages could still show `view_count=0` because direct page open/refresh did not send the article-level pageview on initial mount.
- Issue 2: front article count could be one behind admin/database because the header used the SSR pre-count snapshot.
- Issue 3: admin visit log failed on default entry because SQL concatenated `where 1 = 1order by ...`.
- Issue 4: admin file list showed used images as unused because it read the static `files.status` field instead of deriving usage from business references.

All four issues were recorded in `03-issues.md` and fixed in `04-fix-report.md`.

## Verification Evidence

- PASS: `mvn -f server/pom.xml -Dtest=Batch8PreDeploymentCoreClosureTest test` - 5 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 60 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- PASS: `npm --prefix blog run type-check`; existing local `@vue/language-core` warning remains.
- PASS: `npm --prefix admin run build`; existing warning output only.
- PASS: `npm --prefix blog run build`; existing Nuxt/dependency warning output only.
- PASS: Docker running-stack checks for article view count, visit logs, and file usage status.
- ACCEPTED: user manually verified the final Batch 8 behavior on 2026-05-18.

## Residual Risks

- File usage status is computed dynamically from known persisted reference fields. If future features add new file reference fields, they must be added to the usage-status expression or moved into a normalized file-reference table.
- Location remains `unsupported` until a reliable IP geolocation source is selected.
- Redis, Elasticsearch, MQ, CDC, and deployment hardening are intentionally out of Batch 8 and should not be inferred as completed.
