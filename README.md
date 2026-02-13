# ApplyFlow

A production-ready RESTful API built with Spring Boot for tracking and managing job applications.

---

## Features

- **Authentication**: JWT-based register & login
- **Authorization**: Users can only access their own applications
- **CRUD**: Create, read, update, delete job applications
- **Filtering**: By status, company name, date range
- **Pagination & Sorting**: Configurable page size and sort direction
- **Status History**: Track every status change
- **Validation**: Input validation with detailed error messages
- **Exception Handling**: Global handler with standardized error responses
- **Rate Limiting**: IP-based request throttling
- **Email Notifications**: Status change and reminder emails
- **Reminder Scheduler**: Detects stale applications
- **Swagger UI**: Interactive API documentation
- **Docker**: Multi-stage build with PostgreSQL

---

## Tech Stack

- Java 21
- Spring Boot 3.4
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL (prod) / H2 (dev)
- Maven
- Swagger / OpenAPI 3
- Docker & Docker Compose

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+ (or use included `mvnw`)

### Run Locally (Dev Profile)

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080` with an in-memory H2 database.

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- H2 Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

### Run with Docker (Prod Profile)

```bash
cp .env.example .env
# Edit .env with your values
docker compose up --build
```

---

## API Endpoints

### Authentication

| Method | Endpoint             | Description         |
|--------|----------------------|---------------------|
| POST   | `/api/auth/register` | Register a new user |
| POST   | `/api/auth/login`    | Login, returns JWT  |

### Job Applications

| Method | Endpoint                       | Description                     |
|--------|--------------------------------|---------------------------------|
| POST   | `/api/applications`            | Create a new application        |
| GET    | `/api/applications`            | List all (paginated, filtered)  |
| GET    | `/api/applications/{id}`       | Get by ID                       |
| PUT    | `/api/applications/{id}`       | Update an application           |
| DELETE | `/api/applications/{id}`       | Delete an application           |
| GET    | `/api/applications/{id}/history` | Get status change history     |

### Query Parameters

```
GET /api/applications?status=INTERVIEW&companyName=Google&startDate=2025-01-01&page=0&size=10&sortBy=applicationDate&sortDir=desc
```

---

## Application Statuses

| Status      | Description                      |
|-------------|----------------------------------|
| `APPLIED`   | Initial application submitted    |
| `INTERVIEW` | Interview scheduled/completed    |
| `TECHNICAL` | Technical assessment stage       |
| `OFFER`     | Offer received                   |
| `REJECTED`  | Application rejected             |

---

## Project Structure

```
src/main/java/com/applyflow/
├── config/          # Security, OpenAPI, rate limit config
├── controller/      # REST controllers
├── dto/             # Request/response data transfer objects
├── entity/          # JPA entities
├── enums/           # ApplicationStatus, Role
├── exception/       # Custom exceptions & global handler
├── mapper/          # Entity ↔ DTO mappers
├── repository/      # Spring Data JPA repositories
├── scheduler/       # Scheduled tasks
├── security/        # JWT, auth filter, rate limiting
└── service/         # Business logic
```

---

## Environment Variables

| Variable              | Description                    | Default               |
|-----------------------|--------------------------------|-----------------------|
| `JWT_SECRET`          | Base64-encoded signing key     | Dev default (unsafe)  |
| `DATABASE_URL`        | JDBC connection string         | H2 in-memory          |
| `DATABASE_USERNAME`   | Database username              | `sa`                  |
| `DATABASE_PASSWORD`   | Database password              | (empty)               |
| `MAIL_ENABLED`        | Enable email sending           | `false`               |
| `MAIL_HOST`           | SMTP host                      | `smtp.gmail.com`      |
| `MAIL_USERNAME`       | SMTP username                  | (empty)               |
| `MAIL_PASSWORD`       | SMTP password                  | (empty)               |
| `REMINDER_ENABLED`    | Enable reminder scheduler      | `false`               |
| `REMINDER_STALE_DAYS` | Days before stale reminder     | `7`                   |

---

## Security Measures

- BCrypt password hashing
- Stateless JWT authentication
- Ownership-based access control
- IP-based rate limiting
- CORS configuration
- No sensitive data in responses
- Environment variables for all secrets
- Non-root Docker container

---

## License

This project is for educational and portfolio purposes.
