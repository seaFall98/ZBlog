# API_PLAN.md

> 鏈枃鑱岃矗锛氬畾涔?ZBlog Java 鍚庣 API 鑽夋锛岀‘淇?FlecBlog 鍓嶇杩佺Щ鏃舵湁鏄庣‘濂戠害鏂瑰悜銆?

## API 鍓嶇紑

```text
/api/v1
```

## 鍏紑 API

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

## 绠＄悊 API

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

## 鍝嶅簲绾﹀畾

缁熶竴鍝嶅簲锛?

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

鍒嗛〉鍝嶅簲锛?

```json
{
  "items": [],
  "page": 1,
  "pageSize": 10,
  "total": 0
}
```

## 閫傞厤鍘熷垯

- 鍏堝榻?FlecBlog 鍓嶇瀹為檯闇€瑕佺殑瀛楁锛?
- Java API 鍙互淇濈暀鏇存竻鏅扮殑鍛藉悕锛屼絾杩佺Щ鏈熷厑璁稿湪鍓嶇 adapter 涓仛瀛楁杞崲锛?
- 涓嶄负鍏煎 Go 鍚庣鍐呴儴瀹炵幇鐗虹壊 Java 鍚庣妯″瀷娓呮櫚搴︺€?

