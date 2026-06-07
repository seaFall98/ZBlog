# ZBlog

ZBlog V2 is a blog-first full-stack content system. The V2 mainline uses the React `front/` app as the public frontend, with `admin/` as the CMS and `server/` as the Spring Boot API.

## Version boundary

- `v1.0.0`: legacy ZBlog before React `front/`; use this tag / GitHub Release to reference the old Nuxt `blog/`, old `docker-compose.yml`, `admin/`, and `server/` source.
- V2 mainline: `front/` + `admin/` + `server/`; the legacy Nuxt `blog/` directory is no longer tracked by Git.
- Local migration reference: `blog/` may remain locally as an untracked directory so `front/` can reference old routes, components, and API contracts during the V2 migration.

## Tech Stack

| Module | Stack |
| --- | --- |
| `front/` | React 19, Vite, TypeScript |
| `admin/` | Vue 3, Vite, TypeScript, Element Plus, ECharts, CodeMirror |
| `server/` | Java 21, Spring Boot 3.3, Spring Security, MyBatis, Flyway, Redis, RabbitMQ, PostgreSQL |
| Runtime | Docker Compose, PostgreSQL 16, Redis 7, RabbitMQ 3, optional Elasticsearch |

## Repository Structure

```text
ZBlog/
|-- front/                # V2 public frontend
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
|-- docker-compose.yml    # V2 local stack
`-- .env.example          # Safe local defaults
```

The backend API is under `/api/v1`. Admin APIs are under `/api/v1/admin`. Responses use a common `{ code, message, data }` envelope.

## Quick Start

### Requirements

- Docker Desktop / Docker Compose
- Git

Docker Compose is the recommended local startup path because it prepares PostgreSQL, Redis, RabbitMQ, the backend, the admin console, and the V2 React frontend together.

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

### 3. Start the V2 Stack

```bash
docker compose up --build -d
```

### 4. Open Services

| Service | URL |
| --- | --- |
| V2 front | `http://localhost:5173` |
| Admin console | `http://localhost:4000` |
| Backend API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| RabbitMQ Console | `http://localhost:15672` |

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

### V2 Frontend

```bash
npm --prefix front install
npm --prefix front run dev
npm --prefix front run build
```

The V2 front calls `/api/v1` in the browser. In the Docker stack, Vite proxies `/api` and `/uploads` to the `server` container.

### Legacy Nuxt Blog Reference

The legacy Nuxt `blog/` app is preserved by the `v1.0.0` tag / GitHub Release. During V2 migration, a local untracked `blog/` directory may be kept for reference, but it must not be recommitted to the V2 mainline.

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
ZBLOG_REDIS_HOST=redis
ZBLOG_RABBITMQ_HOST=rabbitmq
ZBLOG_SEARCH_STRATEGY=db
VITE_ZBLOG_API_BASE_URL=/api/v1
VITE_ZBLOG_API_PROXY_TARGET=http://localhost:8080
API_URL=http://localhost:8080/api/v1
```

Do not commit real API keys, SMTP credentials, database credentials, tokens, cookies, private logs, or local tool configuration.

## Quality Checks

```bash
mvn -f server/pom.xml test
npm --prefix admin run type-check
npm --prefix admin run build
npm --prefix front run build
```

For user-visible changes, also verify that rendered pages call the real backend, read and write real data, and keep correct behavior after reload/reopen.

## Roadmap

- Stabilize the V2 `front/` + `admin/` + `server/` stack.
- Keep PostgreSQL search as the default online path while retaining Elasticsearch as an optional strategy.
- Improve mail, notification, outbox, and async retry operations visibility.
- Add more end-to-end business-effect tests beyond response-envelope checks.
- Continue migrating public blog capabilities from the legacy Nuxt reference into the React V2 frontend while preserving the Paico-generated mood and content baseline.

## Attribution

ZBlog's frontend direction references parts of the open-source [FlecBlog](https://github.com/talen8/FlecBlog) project. ZBlog's backend is implemented with Java/Spring Boot and is designed around this project's current goals.

## License

ZBlog is licensed under the [Apache License 2.0](./LICENSE).
