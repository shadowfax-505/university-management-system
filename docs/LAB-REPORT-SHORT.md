# Lab Report: Automated Testing and Continuous Integration (Executive Summary)

**Course:** CSE 3230 – Software Development & Web Technologies Lab  
**Project:** University Management System (UMS) REST API  
**Environment:** Java 21, Spring Boot 3.5.16, Spring Security 6, H2 Database, GitHub Actions  

---

## 1. Objectives

- **Unit Testing:** Implement isolated service-layer unit tests using JUnit 5 and Mockito to enforce domain rules and business invariants without booting the Spring container.
- **Integration Testing:** Build full-slice API integration tests using Spring `MockMvc` and an in-memory H2 database to validate HTTP contracts, JSON serialization, and database transactions.
- **Security & Access Control:** Verify authentication filter chains and role-based access control (RBAC) across Admin, Teacher, and Student endpoints.
- **Continuous Integration (CI):** Establish an automated GitHub Actions CI pipeline executing `./mvnw -B test` against JDK 21 on all `push` and `pull_request` events to `main`.
- **Performance Evaluation:** Measure and analyze execution performance differences between unit tests and container-backed integration tests.

---

## 2. Testing Methodology

| Dimension | Unit Testing | Integration Testing |
| :--- | :--- | :--- |
| **Target** | `AdminService`, `StudentService` | `TeacherController` (full slice) |
| **Tooling** | JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`) | `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Transactional` |
| **Dependencies** | Mock repositories & services (`@Mock`, `@InjectMocks`) | Real Spring context, H2 in-memory DB (MySQL mode) |
| **Execution Speed**| ~0.216 s across all 6 tests (~36 ms/test) | ~6.217 s (context bootstrap + Hibernate DDL) |
| **State Handling** | Stateless mock expectations | Automatic transactional rollback per test |

---

## 3. Unit Test Specifications (6 Tests)

| Test ID | Class | Method | Scenario & Validation |
| :--- | :--- | :--- | :--- |
| **UT-01** | `AdminServiceTest` | `create_validAdmin_savesAndReturnsResponse` | Validates password requirement check, unique constraint verification, entity save, and response mapping. |
| **UT-02** | `AdminServiceTest` | `findById_nonExistingId_throwsResourceNotFoundException` | Asserts `ResourceNotFoundException` is thrown when querying a non-existent admin ID. |
| **UT-03** | `AdminServiceTest` | `delete_lastAdmin_throwsConflictException` | Enforces the system invariant: deleting the final administrator throws `ConflictException` and aborts deletion. |
| **UT-04** | `StudentServiceTest` | `create_validStudent_savesAndReturnsResponse` | Validates student entity fields, unique student ID check, and response DTO mapping. |
| **UT-05** | `StudentServiceTest` | `create_duplicateStudentId_throwsConflictException` | Asserts `ConflictException` is raised when registering an already existing institutional student ID. |
| **UT-06** | `StudentServiceTest` | `findById_nonExistingId_throwsResourceNotFoundException` | Asserts `ResourceNotFoundException` is thrown when a requested student ID is not found. |

---

## 4. Integration Test Specifications (3 Tests)

| Test ID | Class | Method | Scope & Expected HTTP Codes |
| :--- | :--- | :--- | :--- |
| **IT-01** | `TeacherIntegrationTest` | `fullTeacherLifecycle_createReadUpdateDelete` | Full CRUD lifecycle as `ADMIN`: `POST /api/teachers` (`201 Created` + `Location`) $\rightarrow$ `GET` (`200 OK`) $\rightarrow$ `PUT` (`200 OK`) $\rightarrow$ `DELETE` (`204 No Content`) $\rightarrow$ `GET` (`404 Not Found`). |
| **IT-02** | `TeacherIntegrationTest` | `createTeacher_duplicateUsername_returnsConflict` | Submitting duplicate usernames across roles triggers `GlobalExceptionHandler` returning `409 Conflict`. |
| **IT-03** | `TeacherIntegrationTest` | `unauthenticatedAccess_isRejected` | Unauthenticated GET and POST requests are rejected with `401 Unauthorized`. |

---

## 5. Continuous Integration Pipeline

The CI pipeline is defined in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml):
- **Trigger:** Automatic execution on `push` and `pull_request` targeting `main`.
- **Runner Environment:** `ubuntu-latest` (Ubuntu 24.04 LTS).
- **Tooling Versions:** `actions/checkout@v5` and `actions/setup-java@v5` (Node 24 runtime, zero deprecation warnings).
- **JDK Distribution:** Eclipse Temurin 21 with automatic Maven cache (`~/.m2/repository`).
- **Command:** `./mvnw -B test` (deterministic, headless execution).

---

## 6. Verification & Experimental Results

```
Results:
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| Metric | Local Environment (macOS ARM64) | Remote CI Runner (GitHub Actions) |
| :--- | :--- | :--- |
| **Total Test Suite** | 25 tests (16 API + 6 Unit + 3 Integration) | 25 tests |
| **Pass Rate** | 100% (0 failures, 0 errors) | 100% (0 failures, 0 errors) |
| **Unit Test Duration** | ~0.22 s | ~0.28 s |
| **Integration Test Duration** | ~6.22 s | ~7.12 s |
| **Total Job Execution Time** | 7.55 s (test phase) | 32 s – 35 s (end-to-end container lifecycle) |
| **Warnings** | 0 warnings | 0 warnings |

---

## 7. Conclusion

The testing pyramid and CI pipeline for the University Management System were successfully designed and verified. Mock-based unit tests execute in under 250 milliseconds to safeguard domain business rules, while Spring MockMvc integration tests validate HTTP routing, security enforcement, and transactional integrity. With GitHub Actions CI operational, every code revision undergoes automated regression verification, ensuring reliable delivery.
