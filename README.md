<div align="center">
  <h1>ZBlog</h1>

  <p>
    A modern full-stack blog system for writing, publishing, and managing long-form content.
  </p>

  <p>
    一个面向写作、发布和内容管理的现代化全栈博客系统。
  </p>

  <p>
    <img src="https://img.shields.io/badge/Server-Java%2021-007396?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/Blog-Nuxt%204-00DC82?style=flat-square&logo=nuxt&logoColor=white" alt="Nuxt 4" />
    <img src="https://img.shields.io/badge/Admin-Vue%203-42B883?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue 3" />
    <img src="https://img.shields.io/badge/Database-PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  </p>
</div>

## 中文介绍

ZBlog 是一个由前台博客、后台管理端和 Java 后端服务组成的全栈博客系统，适合用于个人博客、内容站点和长期写作型网站。

项目目标是提供完整的内容发布与管理体验：前台负责阅读体验、SEO 和订阅能力；后台负责文章、评论、反馈、文件、设置等管理；后端负责认证、业务 API、数据持久化和集成能力。

### 模块

| 模块 | 技术栈 | 说明 |
| --- | --- | --- |
| `blog` | Nuxt 4 / Vue 3 / SCSS | 前台博客、文章页、SSR、RSS/Atom、Sitemap 和 SEO 友好路由 |
| `admin` | Vue 3 / Vite / Element Plus | 后台管理端，支持内容、评论、反馈、文件、友链和站点设置管理 |
| `server` | Java 21 / Spring Boot / PostgreSQL | REST API、认证、内容管理、上传、订阅、通知和系统配置 |

### 功能特性

- Markdown 文章发布、摘要、封面、置顶、推荐和发布状态管理。
- 分类、标签、归档、搜索、RSS、Atom 和 Sitemap。
- 评论、反馈、订阅、通知和基础用户触达能力。
- 后台文件上传管理，支持公开资源访问。
- Markdown 导入与导出，支持已上传资源的打包导出。
- AI 辅助生成文章标题、摘要和总结，支持 OpenAI-compatible provider。
- 后台站点设置、系统信息、访问统计和健康检查。
- JWT 后台认证，Swagger/OpenAPI 文档。
- Docker Compose 本地一键启动，PostgreSQL 和上传文件使用持久化卷。

### 快速启动

推荐使用 Docker Compose 启动完整本地环境：

```bash
docker compose up --build -d
```

启动后访问：

| 服务 | 地址 |
| --- | --- |
| 前台博客 | `http://localhost:3000` |
| 后台管理 | `http://localhost:4000` |
| 后端 API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |

默认本地后台账号：

```text
Username: admin
Password: admin123456
```

停止服务：

```bash
docker compose down
```

如果需要清空本地数据库和上传文件卷：

```bash
docker compose down -v
```

### 本地开发

后端：

```bash
cd server
mvn test
mvn spring-boot:run
```

后台管理端：

```bash
cd admin
npm install
npm run dev
```

前台博客：

```bash
cd blog
npm install
npm run dev
```

常用环境变量：

```env
ZBLOG_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zblog
ZBLOG_DATASOURCE_USERNAME=zblog
ZBLOG_DATASOURCE_PASSWORD=zblog
ZBLOG_JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
ZBLOG_ADMIN_USERNAME=admin
ZBLOG_ADMIN_PASSWORD=admin123456
```

### 常用命令

| 模块 | 命令 | 说明 |
| --- | --- | --- |
| `server` | `mvn test` | 运行后端测试 |
| `server` | `mvn package` | 构建 Spring Boot 应用 |
| `admin` | `npm run dev` | 启动后台开发服务 |
| `admin` | `npm run type-check` | 运行 TypeScript 类型检查 |
| `admin` | `npm run build` | 构建后台管理端 |
| `blog` | `npm run dev` | 启动前台开发服务 |
| `blog` | `npm run type-check` | 运行 Nuxt 类型检查 |
| `blog` | `npm run build` | 构建前台博客 |

### 项目结构

```text
ZBlog/
├── admin/              # Vue 后台管理端
├── blog/               # Nuxt 前台博客
├── server/             # Java Spring Boot API 服务
├── docs/               # 项目文档
└── docker-compose.yml  # 本地完整环境
```

## English

ZBlog is a full-stack blog system composed of a public blog, an admin console, and a Java backend service. It is designed for personal blogs, content websites, and long-form writing workflows.

The project separates reading, management, and API responsibilities into independent modules: the public blog focuses on content presentation and SEO, the admin console focuses on content operations, and the backend provides authentication, persistence, business APIs, and integration capabilities.

### Modules

| Module | Stack | Purpose |
| --- | --- | --- |
| `blog` | Nuxt 4 / Vue 3 / SCSS | Public blog, article pages, SSR, RSS/Atom, sitemap, and SEO-friendly routes |
| `admin` | Vue 3 / Vite / Element Plus | Admin console for articles, comments, feedback, files, links, and site settings |
| `server` | Java 21 / Spring Boot / PostgreSQL | REST API, authentication, content management, uploads, subscriptions, notifications, and configuration |

### Features

- Markdown article publishing with summaries, covers, pinning, featured content, and publish-state management.
- Categories, tags, archives, search, RSS, Atom, and sitemap.
- Comments, feedback, subscriptions, notifications, and basic user-touch workflows.
- Admin-side file upload management with public static access.
- Markdown import and export with uploaded asset packaging.
- AI-assisted title, summary, and article-summary generation through OpenAI-compatible providers.
- Site settings, system information, visit statistics, and health checks.
- JWT-based admin authentication and Swagger/OpenAPI documentation.
- Docker Compose local stack with persistent PostgreSQL and upload volumes.

### Quick Start

The easiest way to run the full local stack is Docker Compose:

```bash
docker compose up --build -d
```

Open:

| Service | URL |
| --- | --- |
| Public blog | `http://localhost:3000` |
| Admin console | `http://localhost:4000` |
| Backend API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |

Default local admin account:

```text
Username: admin
Password: admin123456
```

Stop the stack:

```bash
docker compose down
```

Remove local database and upload volumes:

```bash
docker compose down -v
```

### Local Development

Server:

```bash
cd server
mvn test
mvn spring-boot:run
```

Admin:

```bash
cd admin
npm install
npm run dev
```

Blog:

```bash
cd blog
npm install
npm run dev
```

Common environment variables:

```env
ZBLOG_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zblog
ZBLOG_DATASOURCE_USERNAME=zblog
ZBLOG_DATASOURCE_PASSWORD=zblog
ZBLOG_JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
ZBLOG_ADMIN_USERNAME=admin
ZBLOG_ADMIN_PASSWORD=admin123456
```

### Scripts

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

### Project Structure

```text
ZBlog/
├── admin/              # Vue admin console
├── blog/               # Nuxt public blog
├── server/             # Java Spring Boot API service
├── docs/               # Project documentation
└── docker-compose.yml  # Local full-stack environment
```

## Acknowledgements

ZBlog's frontend foundation is based on ideas and implementation patterns from the open-source blog project [FlecBlog](https://github.com/talen8/FlecBlog).

## License

This project is intended for personal blog and content system development. License details will be completed before the first public release.
