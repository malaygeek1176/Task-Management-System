# Task Management Application — Backend

A REST API for registering users, authenticating with JWT, and creating,
assigning, filtering, and managing tasks, with role-based access control for
regular `USER`s and `ADMIN`s.

## Features

- JWT-based authentication (register / login)
- Role-based authorization (`USER`, `ADMIN`) via Spring Security
- Full task CRUD plus dedicated status / priority / assignment endpoints
- Task search, filtering (status, priority, assignee, due date, title), sorting, and pagination
- User self-service profile (`/me`) endpoints and admin user management
- Centralized, consistent JSON error responses (`@RestControllerAdvice`)
- Swagger / OpenAPI UI with Bearer JWT auth wired in for live testing
- Idempotent startup seeder for the `USER` / `ADMIN` roles and an optional dev admin account
- Unit/slice tests with JUnit 5, Mockito, and MockMvc

## Technology Stack

| Layer          | Technology                              |
|----------------|------------------------------------------|
| Language       | Java 21                                   |
| Framework      | Spring Boot 4.0.8 (Spring Framework 7)    |
| Build tool     | Maven                                     |
| Web            | Spring Web MVC (`spring-boot-starter-webmvc`) |
| Persistence    | Spring Data JPA / Hibernate               |
| Database       | PostgreSQL                                |
| Security       | Spring Security + JWT (JJWT 0.12.x)       |
| Validation     | Jakarta Bean Validation                   |
| API docs       | springdoc-openapi (Swagger UI)            |
| Testing        | JUnit 5, Mockito, MockMvc, Spring Boot Test |

> **A note on the framework version.** Spring Boot 4.0 / Spring Framework 7
> is a recent major release with a few breaking changes from the 3.x line
> that this project relies on, most notably:
> - `spring-boot-starter-web` is deprecated in favor of **`spring-boot-starter-webmvc`**.
> - `@MockBean` is removed; tests use **`@MockitoBean`** (`org.springframework.test.context.bean.override.mockito.MockitoBean`).
> - `@WebMvcTest` / `@AutoConfigureMockMvc` now live in `org.springframework.boot.webmvc.test.autoconfigure`, and MVC slice tests pull in **`spring-boot-starter-webmvc-test`** instead of the old `spring-boot-starter-test`.
> - `DaoAuthenticationProvider` now takes the `UserDetailsService` via its **constructor** (`setUserDetailsService(...)` was removed).
>
> If your local Maven resolves a slightly different patch version and one of
> these has shifted again, the fix is almost always a one-line import or
> constructor change in the file the compiler points at.

## Architecture

```
Controller → Service → Repository → Database
```

- **Controllers** are thin: validation + delegating to services + wrapping the result in `ApiResponse`.
- **Services** hold all business logic and authorization decisions (ownership checks, role checks).
- **Repositories** are Spring Data JPA interfaces; task filtering uses `JpaSpecificationExecutor` so any combination of filters can be composed.
- **DTOs** are the only objects that cross the controller boundary — JPA entities are never serialized directly, and passwords are never returned.
- **Mappers** convert entities ↔ DTOs.
- Relationships are intentionally **unidirectional** (`Task → User`, never `User → List<Task>`) to avoid infinite JSON recursion and keep the fetch graph predictable.

## Database Overview

| Entity | Key fields | Notes |
|--------|-----------|-------|
| `User` | id, firstName, lastName, email (unique), password (BCrypt hash), enabled, createdAt, updatedAt, roles | Many-to-many with `Role` via `user_roles` join table |
| `Role` | id, name (`USER` \| `ADMIN`) | |
| `Task` | id, title, description, status, priority, dueDate, createdAt, updatedAt, assignedUser, createdBy | `assignedUser` / `createdBy` are `@ManyToOne` references to `User` |

`spring.jpa.hibernate.ddl-auto=update` is used for local development so
Hibernate creates/updates the schema automatically; switch to `validate` or
a migration tool (Flyway/Liquibase) before using this in production.

## Authentication Flow

1. `POST /api/auth/register` — creates a `User` with the `USER` role, BCrypt-hashed password.
2. `POST /api/auth/login` — verifies credentials via Spring Security's `AuthenticationManager` and returns a JWT.
3. Every subsequent request sends `Authorization: Bearer <token>`. `JwtAuthenticationFilter` validates the token and populates the `SecurityContext` for that request.
4. Authorization is then enforced either declaratively (`@PreAuthorize` on admin-only user-management endpoints) or in the service layer (task ownership/assignment rules).

### Authorization Rules

| Action | Who |
|--------|-----|
| View / update own profile (`/me`) | Any authenticated user |
| View / update / delete any user | `ADMIN` only |
| View a specific user by id | `ADMIN`, or the user themselves |
| Create a task | Any authenticated user (becomes `createdBy`) |
| View a task | `ADMIN`, the task's creator, or its assignee |
| List tasks | `ADMIN` sees all; others see only tasks they created or are assigned to |
| Update / delete / reassign / re-prioritize a task | `ADMIN` or the task's creator |
| Change a task's status | `ADMIN`, the task's creator, or its assignee |

## API Overview

Base path: `/api`

### Auth
| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

### Users
| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/users/me` | Authenticated |
| PUT | `/api/users/me` | Authenticated |
| PUT | `/api/users/me/password` | Authenticated |
| GET | `/api/users/{id}` | Admin, or self |
| GET | `/api/users?page=&size=` | Admin |
| PUT | `/api/users/{id}` | Admin |
| DELETE | `/api/users/{id}` | Admin |

### Tasks
| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/api/tasks` | Authenticated |
| GET | `/api/tasks/{id}` | Creator / assignee / admin |
| GET | `/api/tasks?status=&priority=&assignedUserId=&dueDate=&search=&page=&size=&sortBy=&direction=` | Authenticated (scoped) |
| PUT | `/api/tasks/{id}` | Creator / admin |
| DELETE | `/api/tasks/{id}` | Creator / admin |
| PATCH | `/api/tasks/{id}/status` | Creator / assignee / admin |
| PATCH | `/api/tasks/{id}/priority` | Creator / admin |
| PATCH | `/api/tasks/{id}/assign` | Creator / admin |

All successful responses are wrapped as:
```json
{ "success": true, "message": "...", "data": { } }
```

All errors are wrapped as:
```json
{
  "timestamp": "2026-09-18T10:30:00",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Task not found with id: 10",
  "path": "/api/tasks/10"
}
```

## Setup Instructions

### 1. Prerequisites
- JDK 21
- Maven 3.9+ (or use IntelliJ's bundled Maven)
- PostgreSQL 14+ running locally (or reachable over the network)

### 2. Create the database
```sql
CREATE DATABASE taskmanagement;
```
No manual table creation is needed — Hibernate creates the schema on first run (`ddl-auto=update`).

### 3. Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/taskmanagement` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | `postgres` | DB password |
| `DDL_AUTO` | `update` | Hibernate schema strategy |
| `JWT_SECRET` | *(dev default in properties — change this!)* | HMAC signing key, 32+ chars |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_FIRST_NAME` / `ADMIN_LAST_NAME` | `admin@taskmanagement.com` / `Admin@12345` / `System` / `Administrator` | Seeded dev admin account |
| `ADMIN_SEED_ENABLED` | `true` | Set to `false` to skip seeding the admin account |
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |

Set these as actual OS/IDE environment variables, or a local `.env`-style
mechanism of your choice — do **not** commit real credentials.

### 4. Running in IntelliJ IDEA

1. `File → Open` and select the project's root folder (the one containing `pom.xml`). IntelliJ will detect it as a Maven project and import dependencies automatically.
2. Make sure Project SDK is set to **Java 21** (`File → Project Structure → Project`).
3. Set environment variables for the run configuration if you need non-default DB credentials (`Run → Edit Configurations → Environment variables`).
4. Run `TaskManagementApplication.main()` directly, or use the Maven tool window (`Lifecycle → spring-boot:run`).

### 5. Running from the command line
```bash
mvn clean install
mvn spring-boot:run
```

### 6. Swagger UI

Once running:
- Swagger UI: **http://localhost:8080/swagger-ui.html**
- OpenAPI JSON: **http://localhost:8080/v3/api-docs**

Click **Authorize**, paste a JWT obtained from `POST /api/auth/login` (just
the raw token — the UI adds the `Bearer` prefix), and every protected
endpoint becomes testable directly from the browser.

### 7. Running tests
```bash
mvn test
```
Tests are MockMvc slice tests (`@WebMvcTest`) with the service layer mocked
via Mockito — no database is required to run them.

## Sample Workflow: Login → JWT → Task

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@example.com","password":"Password@123"}'

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"Password@123"}'
# -> { "success": true, "data": { "token": "<JWT>", "tokenType": "Bearer", "expiresIn": 86400000 } }

# 3. Create a task using the token
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Complete Spring Boot project","description":"Finish backend implementation","priority":"HIGH","dueDate":"2026-10-01"}'

# 4. List your tasks
curl -H "Authorization: Bearer <JWT>" \
  "http://localhost:8080/api/tasks?status=TODO&sortBy=dueDate&direction=asc"
```

## Project Structure

```
task-management-application/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/taskmanagement/
    │   │   ├── TaskManagementApplication.java
    │   │   ├── config/          # OpenApiConfig, DataSeeder
    │   │   ├── controller/      # AuthController, UserController, TaskController
    │   │   ├── dto/             # auth/, user/, task/, common/
    │   │   ├── entity/          # User, Role, Task
    │   │   ├── enums/           # RoleName, TaskStatus, TaskPriority
    │   │   ├── exception/       # custom exceptions + GlobalExceptionHandler
    │   │   ├── mapper/          # UserMapper, TaskMapper
    │   │   ├── repository/      # UserRepository, RoleRepository, TaskRepository, TaskSpecification
    │   │   ├── security/        # JWT + Spring Security configuration
    │   │   └── service/         # interfaces + impl/
    │   └── resources/
    │       ├── application.properties
    │       └── application-dev.properties
    └── test/
        └── java/com/taskmanagement/
            ├── auth/AuthControllerTest.java
            ├── user/UserControllerTest.java
            └── task/TaskControllerTest.java
```
