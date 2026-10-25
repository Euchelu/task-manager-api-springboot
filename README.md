# Task Manager API

A REST API for managing personal tasks, built with **Spring Boot 3**, **Spring Data JPA / Hibernate** and **Spring Security (JWT)**. Each user registers an account, logs in to obtain a JWT access token, and can then create, list, update and delete their own tasks.

[![CI](https://github.com/<your-username>/task-manager-api/actions/workflows/ci.yml/badge.svg)](https://github.com/<your-username>/task-manager-api/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

## Tech stack

| Concern              | Technology                                   |
|----------------------|-----------------------------------------------|
| Language / runtime   | Java 17                                      |
| Framework            | Spring Boot 3.3 (Web, Validation, Actuator)  |
| Persistence          | Spring Data JPA, Hibernate, PostgreSQL       |
| Security             | Spring Security 6, JWT (jjwt)                |
| API documentation    | springdoc-openapi (Swagger UI)               |
| Testing              | JUnit 5, Mockito, Spring MockMvc, AssertJ    |
| Test database        | H2 (in-memory)                               |
| Build tool           | Maven (with Maven Wrapper)                   |
| Containerization     | Docker, Docker Compose                       |
| CI/CD                | GitHub Actions                               |
| Code coverage        | JaCoCo                                       |

## Features

- User registration and login with **JWT** authentication (stateless sessions, BCrypt password hashing).
- Full **CRUD** for tasks (title, description, status, priority, due date), scoped to the authenticated user.
- Pagination, sorting and filtering by status on the task listing endpoint.
- Centralized exception handling with consistent, structured JSON error responses.
- Request validation with Bean Validation (`jakarta.validation`).
- Interactive API documentation via Swagger UI (OpenAPI 3).
- Unit tests (service layer, Mockito) and integration tests (controllers with MockMvc, repositories with `@DataJpaTest`).
- Multi-stage Docker build and a ready-to-use `docker-compose.yml` (API + PostgreSQL).
- CI pipeline on GitHub Actions: build, test, coverage report, Docker image build.

## Project structure

```
src/main/java/com/taskmanager/api
├── config/          # Security and OpenAPI configuration
├── controller/      # REST controllers (Auth, Task)
├── dto/             # Request/response DTOs (records)
├── entity/          # JPA entities (User, Task) and enums
├── exception/       # Custom exceptions + global exception handler
├── repository/      # Spring Data JPA repositories
├── security/        # JWT service and authentication filter
└── service/         # Business logic (AuthService, TaskService)
```

## Getting started

### Prerequisites

- JDK 17+
- Docker and Docker Compose (recommended, no local PostgreSQL/Maven install needed)

The project ships with the **Maven Wrapper**, so a local Maven installation is not required — use `./mvnw` (Linux/macOS/Git Bash) or `mvnw.cmd` (Windows cmd/PowerShell).

### Option A — Run everything with Docker Compose

```bash
cp .env.example .env   # optional, defaults already work
docker compose up --build
```

This starts a PostgreSQL container and the API container. The API will be available at `http://localhost:8080`, and Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Option B — Run locally against a Dockerized database

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

### Option C — Full local setup without Docker

1. Create a PostgreSQL database and update the environment variables in `.env` (see `.env.example`) or export them in your shell.
2. Run the application:

```bash
./mvnw spring-boot:run
```

## Configuration

All configuration is environment-driven (see `src/main/resources/application.yml`):

| Variable             | Description                                | Default                                    |
|-----------------------|--------------------------------------------|---------------------------------------------|
| `DB_HOST`             | PostgreSQL host                            | `localhost`                                 |
| `DB_PORT`             | PostgreSQL port                            | `5432`                                      |
| `DB_NAME`             | Database name                              | `taskmanager`                               |
| `DB_USERNAME`         | Database user                              | `taskmanager`                               |
| `DB_PASSWORD`         | Database password                          | `taskmanager`                               |
| `JWT_SECRET`          | HMAC signing secret for JWTs (256-bit+)    | development-only placeholder, **change it** |
| `JWT_EXPIRATION_MS`   | Token lifetime in milliseconds             | `86400000` (24h)                            |
| `SERVER_PORT`         | HTTP port                                  | `8080`                                      |

## Running the tests

```bash
./mvnw test              # unit + integration tests
./mvnw verify             # tests + JaCoCo coverage report (target/site/jacoco/index.html)
```

The test suite includes:

- **Unit tests** for `AuthService` and `TaskService` using JUnit 5 + Mockito (repository/collaborator mocking, no Spring context).
- **Controller (slice) tests** for `AuthController` and `TaskController` using `MockMvc`, exercising the real security filter chain with a mocked service layer.
- **Repository tests** for `TaskRepository` using `@DataJpaTest` against an in-memory H2 database.
- **Context load test** to catch configuration/wiring regressions.

## API overview

Base path: `/api/v1`. Full interactive documentation is available at `/swagger-ui.html` once the app is running.

### Authentication

| Method | Endpoint              | Description                        | Auth required |
|--------|-----------------------|-------------------------------------|----------------|
| POST   | `/auth/register`      | Create a new account               | No             |
| POST   | `/auth/login`          | Authenticate and get a JWT          | No             |

### Tasks

| Method | Endpoint            | Description                                | Auth required |
|--------|----------------------|----------------------------------------------|----------------|
| POST   | `/tasks`             | Create a task                               | Yes            |
| GET    | `/tasks`             | List the current user's tasks (paginated, filterable by `status`) | Yes |
| GET    | `/tasks/{id}`        | Get a task by id                            | Yes            |
| PUT    | `/tasks/{id}`        | Update a task                               | Yes            |
| DELETE | `/tasks/{id}`        | Delete a task                               | Yes            |

Authenticated requests must include `Authorization: Bearer <token>`.

### Example

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Ada Lovelace","email":"ada@example.com","password":"SuperSecret123"}'

curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Write README","description":"Document the API","status":"PENDING","priority":"HIGH","dueDate":"2026-12-31"}'
```

## Postman collection

A ready-to-import Postman collection and environment are available in the [`postman/`](postman) folder:

- `Task-Manager-API.postman_collection.json`
- `Task-Manager-API.postman_environment.json`

The `Register`/`Login` requests automatically capture the returned JWT into the `token` collection variable, and `Create Task` captures the created task id into `taskId`, so the rest of the requests work out of the box.

## CI/CD

Every push and pull request to `main` triggers the GitHub Actions workflow in [`.github/workflows/ci.yml`](.github/workflows/ci.yml), which:

1. Sets up JDK 17.
2. Builds the project and runs the full test suite (`./mvnw verify`).
3. Publishes the test report and the JaCoCo coverage report as build artifacts.
4. Builds the Docker image to verify it packages correctly.

## License

MIT
