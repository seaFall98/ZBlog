# P2 Import/Export API Matrix

## Scope

P2 closes the backend contract used by the admin import/export screen:

- article Markdown/Hexo import;
- article WeChat HTML export;
- article Markdown ZIP download;
- Artalk-style comment JSON import.

## Closed endpoints

| Flow | Endpoint | Evidence |
| --- | --- | --- |
| Article import | `POST /api/v1/admin/articles/import` | Multipart files are parsed, categories/tags are ensured, Markdown is stored as `content_markdown`, and rendered HTML is stored separately. |
| WeChat export | `POST /api/v1/admin/articles/{id}/wechat/export` | Returns the article rendered HTML from the persisted article. |
| Markdown ZIP download | `GET /api/v1/admin/articles/{id}/download/zip` | Returns a ZIP containing the Markdown source for the persisted article. |
| Comment import | `POST /api/v1/admin/comments/import` | Multipart JSON import inserts real comments that are visible from the public comment list. |

## Verification

- `mvn -f server/pom.xml -DskipTests compile`: passed.
- `mvn -f server/pom.xml -Dtest=P2ImportExportApiTest test`: passed.
- `mvn -f server/pom.xml test`: passed, 31 tests.
- `npm --prefix admin run type-check`: passed.
- `docker compose up --build -d --wait`: server, blog, admin, and postgres became healthy.
- Docker stack real API validation: admin login, article import, admin article lookup, WeChat export, ZIP download, comment import, and public comment lookup all passed against `localhost:8080`.

## Remaining follow-up

- Image asset rewriting during article import is still not implemented.
- Comment import currently supports common JSON array, `{ "comments": [...] }`, and `{ "data": [...] }` shapes; deeper Artalk relationship/user migration can be expanded later if needed.
