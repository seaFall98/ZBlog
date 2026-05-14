# DATA_MODEL_PLAN.md

> 本文职责：定义第一阶段博客网站的数据模型草案，为 Java 后端 schema 和 Flyway 迁移提供依据。

## 核心表

### users

用途：后台用户、访客账号、评论身份。

关键字段：

```text
id, username, email, password_hash, nickname, avatar_url, role, status, token_version, created_at, updated_at
```

### articles

用途：文章真源、渲染快照、发布状态和 SEO。

关键字段：

```text
id, slug, title, summary, cover_url,
content_markdown, content_html, content_text, toc_json,
status, pinned, featured, allow_comment,
view_count, word_count, reading_minutes,
published_at, created_at, updated_at
```

索引：

```text
slug unique
status + published_at
title/content_text full text 后续扩展
```

### categories

```text
id, name, slug, description, sort_order, created_at, updated_at
```

### tags

```text
id, name, slug, color, created_at, updated_at
```

### article_tags

```text
article_id, tag_id
```

### comments

```text
id, article_id, parent_id, user_id, nickname, email, website,
content, status, ip, user_agent, created_at, updated_at
```

### friends

```text
id, name, url, avatar_url, description, type, status, sort_order, created_at, updated_at
```

### files

```text
id, original_name, stored_name, url, content_type, size, usage_type, uploader_id, created_at, deleted_at
```

### settings

```text
id, key, value_json, group_name, description, updated_at
```

### visits

```text
id, path, referer, ip, user_agent, browser, os, device, created_at
```

### operation_logs

```text
id, user_id, action, resource_type, resource_id, detail_json, ip, created_at
```

## 后续表

- projects；
- albums；
- moments；
- notifications；
- search_index_jobs；
- event_outbox。

这些不进入第一阶段核心 schema。

