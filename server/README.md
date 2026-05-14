# ZBlog2 Server

Java 21 + Spring Boot backend for ZBlog2.

## Run Checks

```powershell
mvn test
mvn package
```

## Development Login

The current Phase 2 foundation uses a temporary bootstrap admin configured in `application.yml`.

```text
username: admin
password: admin123456
```

Use this only for local development. Phase 3 will replace it with database-backed users and password hashing.

## Initial API

```text
GET  /api/v1/health
POST /api/v1/auth/login
GET  /api/v1/admin/ping
GET  /swagger-ui.html
```

## Phase 3 Content API

```text
GET    /api/v1/menus
GET    /api/v1/categories
GET    /api/v1/categories/{idOrSlug}
GET    /api/v1/tags
GET    /api/v1/tags/{idOrSlug}
GET    /api/v1/articles
GET    /api/v1/articles/{slug}
GET    /api/v1/articles/random

GET    /api/v1/admin/articles
POST   /api/v1/admin/articles
GET    /api/v1/admin/articles/{id}
PUT    /api/v1/admin/articles/{id}
DELETE /api/v1/admin/articles/{id}
POST   /api/v1/admin/articles/{id}/publish
POST   /api/v1/admin/articles/{id}/unpublish

GET    /api/v1/admin/categories
POST   /api/v1/admin/categories
PUT    /api/v1/admin/categories/{id}
DELETE /api/v1/admin/categories/{id}

GET    /api/v1/admin/tags
POST   /api/v1/admin/tags
PUT    /api/v1/admin/tags/{id}
DELETE /api/v1/admin/tags/{id}
```

## Phase 4 Interaction, Site And Media API

```text
GET    /api/v1/settings/{group}
PATCH  /api/v1/admin/settings/{group}
PUT    /api/v1/admin/settings/{group}
PUT    /api/v1/admin/settings/ai/mcp-secret/reset

GET    /api/v1/friends
POST   /api/v1/friends/apply
GET    /api/v1/admin/friends/types
POST   /api/v1/admin/friends/types
PUT    /api/v1/admin/friends/types/{id}
DELETE /api/v1/admin/friends/types/{id}
GET    /api/v1/admin/friends
POST   /api/v1/admin/friends
PUT    /api/v1/admin/friends/{id}
DELETE /api/v1/admin/friends/{id}

GET    /api/v1/comments
POST   /api/v1/comments
DELETE /api/v1/comments/{id}
GET    /api/v1/admin/comments
POST   /api/v1/admin/comments
PUT    /api/v1/admin/comments/{id}/toggle-status
DELETE /api/v1/admin/comments/{id}

GET    /api/v1/admin/files
POST   /api/v1/admin/files
DELETE /api/v1/admin/files/{id}
GET    /uploads/**
```
