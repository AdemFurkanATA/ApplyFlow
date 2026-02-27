# ApplyFlow

![Build](https://github.com/AdemFurkanATA/ApplyFlow/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED)

A production-ready RESTful API built with **Spring Boot** for tracking and managing job applications. Designed with enterprise-level architecture, security best practices, and full CI/CD pipeline.

---

## Features

### Core
- **JWT Authentication** with refresh token rotation and replay attack prevention
- **Role-Based Access Control** (USER / ADMIN) with ownership validation
- **CRUD Operations** for job applications with pagination, filtering, and sorting
- **Status History Tracking** for every status change with audit trail

### Security
- BCrypt password hashing
- Stateless JWT authentication (15min access + 7d refresh tokens)
- IP-based rate limiting (Redis-backed with fail-open fallback)
- CORS configuration
- Nginx reverse proxy with security headers (production)

### Infrastructure
- **Redis Caching** with configurable TTL and JSON serialization
- **Rate Limiting** with Redis atomic counters and `Retry-After` headers
- **Event-Driven Audit Logging** with async processing (`@EventListener` + `@Async`)
- **Email Notifications** for status changes and stale application reminders
- **Docker** multi-stage build with health checks and JVM container flags
- **Production Deployment** with Nginx, Docker Compose, and resource limits
- **CI/CD** via GitHub Actions (build → test → Docker push to GHCR)

### Developer Experience
- Swagger UI / OpenAPI 3 interactive documentation
- 48 automated tests (unit + integration)
- Test profile with H2 in-memory database
- Environment-based configuration (dev / prod / test)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Security | Spring Security + JWT (jjwt) |
| Database | PostgreSQL (prod) / H2 (dev/test) |
| Cache | Redis |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Docs | Swagger / OpenAPI 3 |
| CI/CD | GitHub Actions |
| Container | Docker & Docker Compose |
| Reverse Proxy | Nginx |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+ (or use included `mvnw`)
- Docker & Docker Compose (for containerized setup)

### Run Locally (Dev Profile)

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080` with an in-memory H2 database.

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

### Run with Docker

```bash
cp .env.example .env
# Edit .env with your values
docker compose up --build
```

### Run Production Stack

```bash
cp .env.example .env
# Edit .env with production values
docker compose -f docker-compose.prod.yml up --build
```

This starts the full production stack: **App + PostgreSQL + Redis + Nginx**.

### Run Tests

```bash
./mvnw clean test
```

---

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login, returns JWT |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Revoke refresh token |

### Job Applications

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/applications` | Create a new application |
| GET | `/api/applications` | List all (paginated, filtered) |
| GET | `/api/applications/{id}` | Get by ID |
| PUT | `/api/applications/{id}` | Update an application |
| DELETE | `/api/applications/{id}` | Delete an application |
| GET | `/api/applications/{id}/history` | Get status change history |

### Admin (Requires ADMIN role)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/audit-logs` | View audit logs (filtered, paginated) |

### Query Parameters

```
GET /api/applications?status=INTERVIEW&companyName=Google&startDate=2025-01-01&page=0&size=10&sortBy=applicationDate&sortDir=desc
```

---

## Application Statuses

| Status | Description |
|--------|-------------|
| `APPLIED` | Initial application submitted |
| `INTERVIEW` | Interview scheduled/completed |
| `TECHNICAL` | Technical assessment stage |
| `OFFER` | Offer received |
| `REJECTED` | Application rejected |

---

## Project Structure

```
src/main/java/com/applyflow/
├── config/          # Security, OpenAPI, cache, rate limit config
├── controller/      # REST controllers
├── dto/             # Request/response data transfer objects
├── entity/          # JPA entities
├── enums/           # ApplicationStatus, Role, AuditEventType
├── event/           # Domain events and async listeners
├── exception/       # Custom exceptions & global handler
├── mapper/          # Entity ↔ DTO mappers
├── repository/      # Spring Data JPA repositories
├── scheduler/       # Scheduled tasks
├── security/        # JWT, auth filter, rate limiting
└── service/         # Business logic
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_PASSWORD` | Database password | (required) |
| `JWT_SECRET` | Base64-encoded signing key | Dev default (unsafe) |
| `MAIL_ENABLED` | Enable email sending | `false` |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username | (empty) |
| `MAIL_PASSWORD` | SMTP password | (empty) |
| `REMINDER_ENABLED` | Enable reminder scheduler | `false` |
| `REMINDER_STALE_DAYS` | Days before stale reminder | `7` |
| `RATE_LIMIT_GLOBAL` | Global requests/min | `100` |
| `RATE_LIMIT_AUTH` | Auth requests/min | `5` |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `CACHE_TTL` | Cache TTL in seconds | `60` |
| `CACHE_ENABLED` | Enable/disable caching | `true` |

---

## Security Measures

- BCrypt password hashing
- Stateless JWT authentication with refresh token rotation
- Replay attack prevention for refresh tokens
- Ownership-based access control (users see only their data)
- IP-based rate limiting with Redis (fail-open fallback)
- Event-driven audit logging for security events
- Nginx security headers (X-Frame-Options, CSP, HSTS)
- Non-root Docker container
- No sensitive data in responses
- Environment variables for all secrets

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
