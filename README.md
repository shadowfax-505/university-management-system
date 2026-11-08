# University Management System (CSE 3230 – Lab 4)

[![Java CI with Maven](https://github.com/shadowfax-505/university-management-system/actions/workflows/ci.yml/badge.svg)](https://github.com/shadowfax-505/university-management-system/actions/workflows/ci.yml)

Spring Boot 3.5 REST API with three roles (Admin, Teacher, Student), Spring Security
(session login/logout, BCrypt password hashing, role-based access control) and Docker Compose
(app + MySQL 8.4).

Full explanation of every file: `docs/UMS-Explained-Bangla.pdf` (Bangla, line by line), `docs/UMS-Explained.pdf` (English), testing & CI guide `docs/Testing-and-CI-Explained.pdf`, and lab reports `docs/LAB-REPORT.md` / `docs/LAB-REPORT-SHORT.md`.

## Where to find each requirement

| Requirement | File(s) |
|---|---|
| REST CRUD for Admin / Teacher / Student | `src/main/java/com/university/ums/controller/AdminController.java`, `TeacherController.java`, `StudentController.java` |
| JSON request/response shapes | `src/main/java/com/university/ums/dto/` |
| HTTP status codes + JSON errors | `src/main/java/com/university/ums/exception/GlobalExceptionHandler.java` |
| Business rules (unique username, hashing, last admin) | `src/main/java/com/university/ums/service/` |
| Database tables | `src/main/java/com/university/ums/model/` |
| Login | `src/main/java/com/university/ums/controller/AuthController.java` |
| Logout, role-based access rules | `src/main/java/com/university/ums/security/SecurityConfig.java` |
| Password hashing (BCrypt) | `SecurityConfig.java` (`passwordEncoder` bean), `service/UserAccountService.java` |
| How login finds users | `src/main/java/com/university/ums/security/CustomUserDetailsService.java` |
| First admin account | `src/main/java/com/university/ums/config/DataInitializer.java` |
| Swagger UI | `src/main/java/com/university/ums/config/OpenApiConfig.java` |
| Configuration | `src/main/resources/application.yml` |
| Dockerization | `Dockerfile`, `compose.yaml`, `.env.example`, `.dockerignore` |
| Automated unit tests | `src/test/java/com/university/ums/service/` (AdminServiceTest, StudentServiceTest) |
| Automated integration tests | `src/test/java/com/university/ums/` (TeacherIntegrationTest, UniversityApiTests) |
| Continuous Integration | `.github/workflows/ci.yml` |
| Lab report | `docs/LAB-REPORT.md` (Full), `docs/LAB-REPORT-SHORT.md` (Summary) |
| Testing & CI guide (PDF) | `docs/Testing-and-CI-Explained.pdf` |
| Live demo | `demo.sh` |

## Folder layout

```
university-management-system/
├── Dockerfile, compose.yaml      Docker (requirement 3)
├── pom.xml, mvnw, .mvn/          Maven build + wrapper
├── demo.sh                       curl walkthrough
├── docs/                         explanation PDFs (Bangla + English) + lab report
└── src/
    ├── main/java/com/university/ums/
    │   ├── controller/   REST endpoints (requirement 1)
    │   ├── security/     Spring Security (requirement 2)
    │   ├── service/      business rules
    │   ├── repository/   database access
    │   ├── model/        entities / tables
    │   ├── dto/          JSON shapes
    │   ├── exception/    error handling
    │   └── config/       first admin, Swagger UI
    ├── main/resources/application.yml
    └── test/             automated tests (H2)
```

## Run with Docker (recommended)

```bash
docker compose up --build
```

App: http://localhost:8080 . On a fresh database these demo accounts are created automatically
(ids are sequential because all roles share one `users` table):

| ID | Username | Password | Role |
|---|---|---|---|
| 1 | admin | admin123 | ADMIN |
| 2 | admin2 | admin123 | ADMIN |
| 3–5 | teacher1, teacher2, teacher3 | teacher123 | TEACHER |
| 6–8 | student1, student2, student3 | student123 | STUDENT |

Change the first admin via `.env` (see `.env.example`); set `APP_DEMO_DATA=false` to skip the demo users.
To get ids 1–8 again, reset the database: `docker compose down -v`, then `docker compose up --build`. Then, in another terminal:

```bash
./demo.sh
```

Stop: `docker compose down` (add `-v` to wipe the database).

## Try it in the browser (Swagger UI)

Open http://localhost:8080/swagger-ui.html . Log in either way:

1. Click **Authorize**, enter `admin` / `admin123` (HTTP Basic, sent with every request), or
2. Open **Authentication → POST /api/auth/login**, click **Execute** (the example body is the
   seeded admin). The browser keeps the session cookie for the next requests.

Then open any endpoint and click **Execute**. Use one login method at a time: while Authorize
holds credentials they take priority over the session (click Authorize → Logout to clear them).

## Run the tests (no MySQL needed, uses in-memory H2)

```bash
./mvnw test
```

## Endpoints

| Method | Path | Success | Who |
|---|---|---|---|
| POST | /api/auth/login | 200 + session cookie | everyone |
| POST | /api/auth/logout | 200 | everyone |
| GET | /api/auth/me | 200 | any logged-in user |
| GET | /swagger-ui.html, /v3/api-docs | 200 | everyone (API docs only) |
| GET / POST | /api/admins | 200 / 201 | ADMIN |
| GET / PUT / DELETE | /api/admins/{id} | 200 / 200 / 204 | ADMIN |
| GET / POST | /api/teachers | 200 / 201 | ADMIN, TEACHER |
| GET / PUT / DELETE | /api/teachers/{id} | 200 / 200 / 204 | ADMIN, TEACHER |
| GET / POST | /api/students | 200 / 201 | ADMIN, STUDENT |
| GET / PUT / DELETE | /api/students/{id} | 200 / 200 / 204 | ADMIN, STUDENT |

Errors: 400 validation, 401 not logged in / bad credentials, 403 wrong role,
404 unknown id, 409 duplicate username/email/studentId or deleting the last admin.

Every request can also use HTTP Basic auth instead of the session (`curl -u admin:admin123 ...`).

## Example bodies

```json
POST /api/teachers
{"name":"Dr. Karim","email":"karim@uni.edu","username":"karim","password":"secret123",
 "department":"CSE","designation":"Professor"}

POST /api/students
{"name":"Rahim","email":"rahim@uni.edu","username":"rahim","password":"secret123",
 "studentId":"2007001","department":"CSE","batch":2020}
```

On `PUT`, leave out `password` to keep the current one.
