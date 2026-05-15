# API_PLAN.md

> 本文职责：定义 ZBlog Java 后端 API 草案，确保 FlecBlog 前端迁移时有清晰契约方向。

## API 前缀

```text
/api/v1
```

## 公共 API

```text
GET    /site/config
GET    /articles
GET    /articles/{slug}
GET    /articles/{slug}/comments
POST   /articles/{slug}/comments
GET    /categories
GET    /categories/{slug}/articles
GET    /tags
GET    /tags/{slug}/articles
GET    /archive
GET    /search
GET    /friends
POST   /friends/apply
GET    /pages/{slug}
GET    /sitemap.xml
GET    /feed.xml
```

## 管理 API

```text
POST   /auth/login
POST   /auth/logout
POST   /auth/refresh
GET    /admin/dashboard

GET    /admin/articles
POST   /admin/articles
GET    /admin/articles/{id}
PUT    /admin/articles/{id}
DELETE /admin/articles/{id}
POST   /admin/articles/{id}/publish
POST   /admin/articles/{id}/unpublish

GET    /admin/categories
POST   /admin/categories
PUT    /admin/categories/{id}
DELETE /admin/categories/{id}

GET    /admin/tags
POST   /admin/tags
PUT    /admin/tags/{id}
DELETE /admin/tags/{id}

GET    /admin/comments
POST   /admin/comments/{id}/approve
POST   /admin/comments/{id}/reject
DELETE /admin/comments/{id}

GET    /admin/friends
POST   /admin/friends
PUT    /admin/friends/{id}
DELETE /admin/friends/{id}

GET    /admin/files
POST   /admin/files
DELETE /admin/files/{id}

GET    /admin/settings
PUT    /admin/settings

GET    /admin/stats/overview
GET    /admin/stats/visits
```

## 响应约定

统一响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

分页响应：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 10,
  "total": 0
}
```

## 适配原则

- 先对齐 FlecBlog 前端真正需要的字段；
- Java API 可以保留更清晰的命名，但迁移期允许在前端 adapter 中做字段转换；
- 不为了兼容 Go 后端内部实现而牺牲 Java 后端模型清晰度。
