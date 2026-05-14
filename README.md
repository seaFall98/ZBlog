<div align="center">
  <h1>ZBlog</h1>

  <p>
    A modern full-stack blog system for writing, publishing, and managing long-form content.
  </p>

  <p>
    Clean reading experience, focused content management, and a Java backend designed for long-term evolution.
  </p>

  <p>
    <img src="https://img.shields.io/badge/Server-Java%2021-007396?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/Blog-Nuxt%204-00DC82?style=flat-square&logo=nuxt&logoColor=white" alt="Nuxt 4" />
    <img src="https://img.shields.io/badge/Admin-Vue%203-42B883?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue 3" />
    <img src="https://img.shields.io/badge/Database-PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  </p>
</div>

## About

ZBlog is a three-part blog system built around content creation and daily publishing workflows.

It separates the public blog, admin console, and backend service into independent modules, keeping the reading experience expressive while leaving the management and API layers clear, testable, and maintainable.

| Module | Stack | Purpose |
| --- | --- | --- |
| `blog` | Nuxt 4 / Vue 3 / SCSS | Public blog, article pages, SSR, feeds, and SEO-friendly routes |
| `admin` | Vue 3 / Vite / Element Plus | Content management, article editing, comments, links, files, and site settings |
| `server` | Java 21 / Spring Boot / PostgreSQL | REST API, authentication, content, taxonomy, comments, links, files, and configuration |

## Features

- Article publishing with Markdown rendering, summaries, covers, pinning, and featured content.
- Category and tag management for organizing long-form writing.
- Public blog pages for home, articles, archives, categories, tags, friends, messages, and profile-style content.
- Comment submission, listing, moderation, and basic status management.
- Friend link display, grouping, and application workflow.
- File upload management with static resource access.
- Site settings for public information and admin-side configuration.
- JWT-based admin authentication.
- OpenAPI documentation through Swagger UI.
- SSR-ready blog frontend with sitemap, RSS, Atom, and PWA-related frontend foundations.

## Tech Stack

### Server

- Java 21
- Spring Boot 3
- Spring Security
- Spring JDBC
- Flyway
- PostgreSQL
- H2 for integration tests
- springdoc-openapi

### Admin

- Vue 3
- Vite
- TypeScript
- Element Plus
- Vue Router
- Axios
- CodeMirror
- ECharts

### Blog

- Nuxt 4
- Vue 3
- TypeScript
- SCSS
- markdown-it
- Highlight.js
- Mermaid
- Nuxt SEO modules
- Vite PWA

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 20+
- PostgreSQL 12+

### Server

```bash
cd server
mvn test
mvn spring-boot:run
```

The default API base URL is:

```text
http://localhost:8080/api/v1
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

For local development, configure PostgreSQL with the following defaults or override them through environment variables:

```env
ZBLOG_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zblog
ZBLOG_DATASOURCE_USERNAME=zblog
ZBLOG_DATASOURCE_PASSWORD=zblog
ZBLOG_JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
ZBLOG_ADMIN_USERNAME=admin
ZBLOG_ADMIN_PASSWORD=admin123456
```

### Admin

```bash
cd admin
npm install
npm run dev
```

The admin console runs at:

```text
http://localhost:4000
```

### Blog

```bash
cd blog
npm install
npm run dev
```

The public blog runs at:

```text
http://localhost:3000
```

## Scripts

| Module | Command | Description |
| --- | --- | --- |
| `server` | `mvn test` | Run backend tests |
| `server` | `mvn package` | Build the Spring Boot application |
| `admin` | `npm run dev` | Start the admin dev server |
| `admin` | `npm run type-check` | Run TypeScript checks |
| `admin` | `npm run build` | Build the admin app |
| `blog` | `npm run dev` | Start the blog dev server |
| `blog` | `npm run type-check` | Run Nuxt type checks |
| `blog` | `npm run build` | Build the blog app |

## Project Structure

```text
ZBlog/
├── admin/     # Vue admin console
├── blog/      # Nuxt public blog
├── server/    # Java Spring Boot API service
└── docs/      # Project documentation
```

## Acknowledgements

ZBlog's frontend foundation is based on ideas and implementation patterns from the open-source blog project FlecBlog.

## License

This project is intended for personal blog and content system development. License details will be completed before the first public release.
