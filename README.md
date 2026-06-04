# ZBlog

Blog-first full-stack content system built with Nuxt 4, Vue 3, Spring Boot 3, and PostgreSQL.

ZBlog combines a public blog, an admin console, and a Java API service. It is designed for long-form writing, content publishing, and self-hosted blog operations.

## Highlights

- Markdown-first article model with rendered HTML/text derived from source content.
- Public Nuxt blog with SSR, SEO routes, RSS/Atom feeds, sitemap, and responsive pages.
- Vue admin console for articles, comments, users, files, albums, links, guestbook messages, settings, statistics, and operations views.
- Spring Boot backend organized around business modules with application, domain, infrastructure, and port boundaries.
- Local production-like stack with PostgreSQL, Redis, RabbitMQ, Flyway, Docker Compose, health checks, and persistent volumes.
- PostgreSQL search by default, with an optional Elasticsearch strategy and reindex/status management.
- OpenAI-compatible AI writing support for titles, summaries, and article metadata.

## Tech Stack

| Module | Stack |
| --- | --- |
| `blog/` | Nuxt 4, Vue 3, TypeScript, SCSS, Markdown-it, Nuxt SEO |
| `admin/` | Vue 3, Vite, TypeScript, Element Plus, ECharts, CodeMirror |
| `server/` | Java 21, Spring Boot 3.3, Spring Security, JDBC/MyBatis, Flyway, Redis, RabbitMQ, PostgreSQL |
| Runtime | Docker Compose, PostgreSQL 16, Redis 7, RabbitMQ 3, optional Elasticsearch |

## Repository Structure

```text
ZBlog/
|-- blog/                 # Nuxt public blog, SSR and SEO-oriented pages
|-- admin/                # Vue admin console for content operations
|-- server/               # Java Spring Boot API service
|   `-- src/main/java/com/zblog/
|       |-- content/      # Article lifecycle and content management
|       |-- identity/     # Users, auth, password and OAuth boundaries
|       |-- comment/      # Comments and moderation
|       |-- media/        # Uploads and file management
|       |-- stats/        # Visit collection and statistics read models
|       |-- search/       # DB/Elasticsearch search strategy boundary
|       |-- event/        # Event outbox, scheduler and broker adapters
|       |-- mail/         # Mail outbox boundary
|       `-- ...
|-- docker-compose.yml    # Local production-like stack
`-- .env.example          # Safe local defaults
```

The backend API is under `/api/v1`. Admin APIs are under `/api/v1/admin`. Responses use a common `{ code, message, data }` envelope.

## Quick Start

### Requirements

- Docker Desktop / Docker Compose
- Git

Docker Compose is the recommended local startup path because it prepares PostgreSQL, Redis, RabbitMQ, the backend, the public blog, and the admin console together.

### 1. Clone

```bash
git clone https://github.com/seaFall98/ZBlog.git
cd ZBlog
```

### 2. Configure Environment

```bash
cp .env.example .env
```

The local defaults are suitable for development. Replace default passwords and JWT secrets before using the project on a public network or in production.

### 3. Start the Stack

```bash
docker compose up --build -d
```

### 4. Open Services

| Service | URL |
| --- | --- |
| Public blog | `http://localhost:3000` |
| Admin console | `http://localhost:4000` |
| Backend API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| RabbitMQ Console | `http://localhost:15672` |

Default local admin account:

```text
Username: admin
Password: admin123456
```

### 5. Stop the Stack

```bash
docker compose down
```

To clear database, Redis, RabbitMQ, and upload volumes:

```bash
docker compose down -v
```

## Local Development

The backend uses system Maven. This repository does not include a Maven wrapper.

### Backend

```bash
mvn -f server/pom.xml test
mvn -f server/pom.xml package
mvn -f server/pom.xml spring-boot:run
```

Run one test class:

```bash
mvn -f server/pom.xml -Dtest=UserAccountApiTest test
```

### Admin Console

```bash
npm --prefix admin install
npm --prefix admin run dev
npm --prefix admin run type-check
npm --prefix admin run build
```

### Public Blog

```bash
npm --prefix blog install
npm --prefix blog run dev
npm --prefix blog run type-check
npm --prefix blog run build
```

## Common Operations

Container status:

```bash
docker compose ps
```

Backend logs:

```bash
docker compose logs -f server
```

PostgreSQL backup:

```bash
mkdir -p backup
docker compose exec -T postgres pg_dump -U zblog -d zblog > backup/zblog.sql
```

PostgreSQL restore:

```bash
cat backup/zblog.sql | docker compose exec -T postgres psql -U zblog -d zblog
```

Upload backup:

```bash
# Linux/macOS
docker run --rm -v zblog_zblog_uploads:/data -v "$PWD/backup:/backup" alpine sh -c "cd /data && tar -czf /backup/uploads.tar.gz ."

# Windows Git Bash
docker run --rm -v zblog_zblog_uploads:/data -v "$(pwd -W)/backup:/backup" alpine sh -c "cd /data && tar -czf /backup/uploads.tar.gz ."
```

## Search Strategy

PostgreSQL search is the default strategy and is intended for low-operations self-hosting.

Optional Elasticsearch startup:

```bash
docker compose --profile search up -d elasticsearch
```

Then enable it with environment variables:

```env
ZBLOG_SEARCH_STRATEGY=elasticsearch
ZBLOG_SEARCH_FALLBACK_TO_DB=true
```

Elasticsearch is not a default production dependency. Before using it in production, add deployment-specific security, resource, and index operations controls.

## Environment Variables

See `.env.example` for the full local template. Common values:

```env
ZBLOG_DATASOURCE_URL=jdbc:postgresql://postgres:5432/zblog
ZBLOG_DATASOURCE_USERNAME=zblog
ZBLOG_DATASOURCE_PASSWORD=zblog
ZBLOG_REDIS_HOST=redis
ZBLOG_RABBITMQ_HOST=rabbitmq
ZBLOG_SEARCH_STRATEGY=db
ZBLOG_JWT_SECRET=replace-with-a-strong-secret
ZBLOG_ADMIN_USERNAME=admin
ZBLOG_ADMIN_PASSWORD=replace-with-a-strong-password
NUXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NUXT_API_SERVER_URL=http://server:8080/api/v1
API_URL=http://localhost:8080/api/v1
```

## Quality Checks

```bash
mvn -f server/pom.xml test
npm --prefix admin run type-check
npm --prefix admin run build
npm --prefix blog run type-check
npm --prefix blog run build
```

For user-visible changes, also verify that rendered pages call the real backend, read and write real data, and keep correct behavior after reload/reopen.

## Roadmap

- Keep PostgreSQL search as the default online path while retaining Elasticsearch as an optional strategy.
- Improve mail, notification, outbox, and async retry operations visibility.
- Add more end-to-end business-effect tests beyond response-envelope checks.
- Continue improving public blog UX, admin operations screens, and self-hosting guidance.

## Contributing

Issues and discussions are welcome. Before submitting a change:

1. Explain the problem, reproduction steps, or design motivation.
2. Add verification for user-visible behavior where relevant.
3. Verify actual pages when frontend and backend contracts change together.
4. Do not commit `.env`, tokens, cookies, private logs, or local tool configuration.

## Security

- Replace default accounts, passwords, and JWT secrets from `.env.example` before production use.
- Keep remote fetch, RSS, and AI base URL access constrained to reduce SSRF risk.
- Do not commit real API keys, SMTP credentials, or database credentials.

## Attribution

ZBlog's frontend direction references parts of the open-source [FlecBlog](https://github.com/talen8/FlecBlog) project. ZBlog's backend is implemented with Java/Spring Boot and is designed around this project's current goals.

## License

ZBlog is licensed under the [Apache License 2.0](./LICENSE).
