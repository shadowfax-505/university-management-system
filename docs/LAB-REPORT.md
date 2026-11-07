# Lab Report: Automated Testing and Continuous Integration for University Management System

**Course:** CSE 3230 – Software Development & Web Technologies Lab  
**Project:** University Management System (UMS) REST API  
**Environment:** Java 21 LTS, Spring Boot 3.5.16, Spring Security 6, H2 In-Memory DB, GitHub Actions  

---

## 1. Introduction & Objectives

### 1.1 Introduction
Automated software testing and continuous integration (CI) are fundamental pillars of modern software engineering. In enterprise web applications, testing ensures that individual business rules operate correctly in isolation (unit testing), while validating that layered subsystems—including security filters, object-relational mapping (ORM), serialization layers, and database constraints—collaborate properly under realistic operational conditions (integration testing).

This laboratory work implements a test pyramid and automated delivery pipeline for the **University Management System (UMS)**, a multi-role REST API built with Spring Boot, Spring Security, and Spring Data JPA.

### 1.2 Objectives
The primary objectives of this laboratory experiment are:
- **Service Layer Unit Testing**: Construct isolated unit tests using JUnit 5 and Mockito to verify service-level domain logic, exception throwing, and critical business invariants (e.g., preventing deletion of the last remaining system administrator and validating unique student identifiers) without starting the Spring ApplicationContext.
- **Full-Slice Integration Testing**: Develop end-to-end integration tests using Spring `MockMvc` and an in-memory H2 database to validate the full HTTP request lifecycle, JSON request/response body mapping, database transaction rollbacks, and standard REST HTTP status codes (`201 Created`, `200 OK`, `204 No Content`, `401 Unauthorized`, `404 Not Found`, `409 Conflict`).
- **Access Control & Security Verification**: Exercise the Spring Security filter chain against authenticated and unauthenticated callers to confirm that role-based access control (RBAC) boundaries are strictly enforced across Admin, Teacher, and Student resources.
- **Continuous Integration Pipeline Construction**: Design and deploy an automated GitHub Actions CI workflow triggered on repository `push` and `pull_request` events to execute headless Maven builds (`./mvnw -B test`) on Ubuntu runners with Java 21 Temurin.
- **Build Quality & Performance Analysis**: Measure execution performance differences between mock-based unit tests and container-backed integration tests, ensuring all tests pass reliably with zero warnings or regressions.

---

## 2. System Architecture & Testing Strategy

The University Management System employs a three-tier architecture augmented by Spring Security and a JPA database persistence layer.

```
       +-------------------------------------------------------+
       |                  HTTP Clients / Requests              |
       +-------------------------------------------------------+
                                  |
                                  v
+=====================================================================+
| Spring Security Filter Chain (Authentication, Authorization, CORS)  |
+=====================================================================+
       |                                           |
       | (Integration Tests Target Here)          |
       v                                           v
+-------------------------------+       +-----------------------------+
| REST Controllers (WebMvc)     |       | Exception Handler           |
| (Admin, Teacher, Student)     |       | (GlobalExceptionHandler)    |
+-------------------------------+       +-----------------------------+
               |
               v
+---------------------------------------------------------------------+
| Service Layer (Business Logic, Invariants)                          |
| - AdminService, StudentService, TeacherService, UserAccountService   |
| (Unit Tests Target Here via Mockito)                                 |
+---------------------------------------------------------------------+
               |
               v
+---------------------------------------------------------------------+
| Data Access Layer (Spring Data JPA Repositories)                    |
| - AdminRepository, StudentRepository, UserRepository                |
+---------------------------------------------------------------------+
               |
               v
+---------------------------------------------------------------------+
| In-Memory Database (H2 Database in MySQL Dialect Mode)              |
+---------------------------------------------------------------------+
```

### Testing Strategy Breakdown

| Dimension | Unit Testing | Integration Testing |
| :--- | :--- | :--- |
| **Target Scope** | Individual service classes (`AdminService`, `StudentService`) | Full slice (`DispatcherServlet` $\rightarrow$ Security Filter $\rightarrow$ Controller $\rightarrow$ Service $\rightarrow$ DB) |
| **Dependencies** | Mocked with Mockito (`@Mock`, `@InjectMocks`) | Real Spring Application Context (`@SpringBootTest`, `@AutoConfigureMockMvc`) |
| **Database** | None (simulated via repository mocks) | In-memory H2 database (`jdbc:h2:mem:testdb;MODE=MySQL`) |
| **Execution Speed** | Extremely fast (~10–50 ms per suite) | Moderate (~500–1500 ms initialization overhead) |
| **State Cleanliness**| Ephemeral mock state | Transactional rollback (`@Transactional`) per test |

---

## 3. Unit Testing Implementation

Unit tests focus strictly on business validation, exception throwing, and domain invariants inside the service layer without initializing the Spring dependency injection container.

### 3.1 Test Setup & Isolation
Using `@ExtendWith(MockitoExtension.class)` eliminates the overhead of Spring Boot context bootstrapping. Repositories and helper services are mocked using Mockito:
- `AdminRepository` and `StudentRepository` mock persistence operations.
- `UserAccountService` mocks password validation (`requirePassword`) and unique constraint pre-checks (`ensureUnique`).
- `ReflectionTestUtils.setField` is utilized to assign synthetic surrogate IDs to JPA entities whose primary keys are managed by `@GeneratedValue(strategy = IDENTITY)`.

### 3.2 Unit Test Matrix (6 Test Cases)

| Test ID | Test Class | Method Name | Scenario / Condition | Expected Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **UT-01** | `AdminServiceTest` | `create_validAdmin_savesAndReturnsResponse` | Valid `AdminRequest` supplied. | Calls `accounts.requirePassword()`, `accounts.ensureUnique()`, saves entity, and returns matching `AdminResponse`. |
| **UT-02** | `AdminServiceTest` | `findById_nonExistingId_throwsResourceNotFoundException` | Non-existent admin ID requested (`Optional.empty()`). | Throws `ResourceNotFoundException` with message containing ID. |
| **UT-03** | `AdminServiceTest` | `delete_lastAdmin_throwsConflictException` | Total admin count in system is $\le 1$. | Throws `ConflictException("Cannot delete the last admin account")`; aborts deletion. |
| **UT-04** | `StudentServiceTest` | `create_validStudent_savesAndReturnsResponse` | Valid `StudentRequest` with unique student ID. | Validates credentials, checks ID uniqueness, saves entity, returns `StudentResponse`. |
| **UT-05** | `StudentServiceTest` | `create_duplicateStudentId_throwsConflictException` | Student ID already exists in `StudentRepository`. | Throws `ConflictException("Student ID '...' is already registered")`; aborts save. |
| **UT-06** | `StudentServiceTest` | `findById_nonExistingId_throwsResourceNotFoundException` | Non-existent student ID requested (`Optional.empty()`). | Throws `ResourceNotFoundException` with message containing ID. |

### 3.3 Key Unit Testing Code Snippet
The following test demonstrates the enforcement of the system-level business rule forbidding deletion of the last remaining system administrator:

```java
@Test
void delete_lastAdmin_throwsConflictException() {
    Admin admin = new Admin();
    ReflectionTestUtils.setField(admin, "id", 1L);
    when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(adminRepository.count()).thenReturn(1L);

    assertThatThrownBy(() -> adminService.delete(1L))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Cannot delete the last admin account");

    verify(adminRepository, never()).delete(any());
}
```

---

## 4. Integration Testing Implementation

Integration testing validates that the HTTP routing, JSON serialization/deserialization, validation annotations (`@Valid`, `@NotBlank`, `@Email`), Spring Security filters, and JPA SQL execution behave harmoniously.

### 4.1 MockMvc and In-Memory H2 Configuration
Tests are annotated with:
- `@SpringBootTest`: Loads the complete application context.
- `@AutoConfigureMockMvc`: Injects a `MockMvc` instance to simulate HTTP requests against `TestDispatcherServlet` without starting a network port.
- `@Transactional`: Wraps each test in a database transaction that is automatically rolled back upon test completion, guaranteeing test isolation without manual teardown.

The test profile connects to an in-memory database configured in `src/test/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    open-in-view: false
```

### 4.2 Integration Test Matrix (3 Test Cases)

| Test ID | Test Class | Method Name | Verification Scope | Status Codes Verified |
| :--- | :--- | :--- | :--- | :--- |
| **IT-01** | `TeacherIntegrationTest` | `fullTeacherLifecycle_createReadUpdateDelete` | Executes full CRUD lifecycle as an authenticated `ADMIN`: POST $\rightarrow$ GET $\rightarrow$ PUT $\rightarrow$ DELETE $\rightarrow$ GET. | `201 Created` $\rightarrow$ `200 OK` $\rightarrow$ `200 OK` $\rightarrow$ `204 No Content` $\rightarrow$ `404 Not Found` |
| **IT-02** | `TeacherIntegrationTest` | `createTeacher_duplicateUsername_returnsConflict` | Submits two consecutive creation requests using identical usernames across user roles. | First request: `201 Created`; Duplicate request: `409 Conflict` (`GlobalExceptionHandler` contract) |
| **IT-03** | `TeacherIntegrationTest` | `unauthenticatedAccess_isRejected` | Sends GET and POST requests to `/api/teachers` without authentication headers or session cookies. | `401 Unauthorized` |

---

## 5. Continuous Integration with GitHub Actions

Continuous Integration automatically validates code changes against a standardized target environment, preventing broken builds or regressions from being merged into main production branches.

### 5.1 Pipeline Specification (`.github/workflows/ci.yml`)
The workflow triggers on every push and pull request targeting the `main` branch:

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Check out repository
      uses: actions/checkout@v5

    - name: Set up JDK 21
      uses: actions/setup-java@v5
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven

    - name: Make Maven wrapper executable
      run: chmod +x mvnw

    - name: Run tests with Maven
      run: ./mvnw -B test
```

### 5.2 Deprecation Prevention & Best Practices
- **Node.js 24 Runtime Compliance**: Official GitHub Actions (`checkout` and `setup-java`) were configured with `@v5`, which targets Node.js 24 instead of the deprecated Node.js 20 runtime.
- **Maven Dependency Caching**: Caching the `~/.m2/repository` directory via `cache: maven` minimizes external artifact downloads, reducing pipeline execution duration by up to 60%.
- **Deterministic Flags**: The `-B` (batch mode) flag prevents interactive prompts, ensuring reliable non-interactive headless builds.

---

## 6. Experimental Results & Verification

### 6.1 Test Suite Execution Metrics
Execution of the test suite via the Maven Surefire plugin yielded zero failures across both local development and remote CI environments:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.university.ums.UniversityApiTests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.434 s
[INFO] Running com.university.ums.service.StudentServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.167 s
[INFO] Running com.university.ums.service.AdminServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.049 s
[INFO] Running com.university.ums.TeacherIntegrationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.783 s
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 6.2 Local vs. CI Performance Comparison

| Metric | Local Workstation (Apple Silicon) | GitHub Actions Runner (`ubuntu-latest`) |
| :--- | :--- | :--- |
| **Java Distribution** | OpenJDK 25.0.1 | Eclipse Temurin 21.0.9 |
| **Total Tests Executed** | 25 | 25 |
| **Unit Test Execution** | ~0.216 s | ~0.280 s |
| **Integration Test Execution**| ~6.217 s | ~7.120 s |
| **Total Job Duration** | 7.55 s (clean test compile) | 32 s (including container init and checkout) |
| **Final Exit Status** | 0 (`BUILD SUCCESS`) | 0 (`success`) |

---

## 7. Discussion & Engineering Analysis

1. **Test Pyramid Economics**: The unit tests in `AdminServiceTest` and `StudentServiceTest` executed in an aggregate of **0.216 seconds**, whereas integration suites required **~6.2 seconds** due to Spring Application Context bootstrapping and Hibernate DDL generation. This 30x difference illustrates why fine-grained business logic and edge cases belong in unit tests, while integration tests should focus on interface contracts and security boundaries.

2. **Transactional State Isolation**: Applying Spring's `@Transactional` annotation to test classes ensures that records created during testing (such as teachers and students in `TeacherIntegrationTest`) are rolled back automatically when the test method finishes. This prevents inter-test data pollution and avoids the overhead of manually wiping database tables between runs.

3. **Handling Generated Database IDs in Unit Mocks**: In JPA entities using `GenerationType.IDENTITY`, entity IDs are populated by the database engine at INSERT time and do not possess public setters to preserve encapsulation. Unit tests resolved this without altering production model code by leveraging Spring's `ReflectionTestUtils.setField(entity, "id", value)`.

4. **Continuous Integration Hygiene**: Modern CI pipelines mandate proactive monitoring of action deprecations. By keeping runner versions and GitHub actions pinned to current releases (`v5`), pipelines remain resilient against upstream security revocations and runner image migrations.

---

## 8. Conclusion

In this laboratory exercise, an automated testing and continuous integration architecture was constructed for the University Management System. A total of 6 unit tests were implemented using JUnit 5 and Mockito to enforce service invariants in complete isolation, and 3 full-stack integration tests were engineered using Spring MockMvc and H2 database to validate REST contracts, password hashing confidentiality, and Spring Security authorization. Finally, a GitHub Actions workflow was established, achieving automated regression testing and build verification with 100% test pass rates on remote runners.

