package com.university.ums;

import com.university.ums.model.Teacher;
import com.university.ums.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real security filter chain, controllers, services and an H2 database.
 * Each test runs in a transaction that is rolled back, so tests do not affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UniversityApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TeacherRepository teacherRepository;

    private static final String TEACHER_JSON = """
            {"name":"Dr. Karim","email":"karim@uni.edu","username":"karim","password":"secret123",
             "department":"CSE","designation":"Professor"}""";

    private static final String STUDENT_JSON = """
            {"name":"Rahim","email":"rahim@uni.edu","username":"rahim","password":"secret123",
             "studentId":"2007001","department":"CSE","batch":2020}""";

    private static final String ADMIN_JSON = """
            {"name":"Second Admin","email":"admin2@uni.edu","username":"admin2","password":"secret123"}""";

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    // ---------- Authentication ----------

    @Test
    void requestWithoutLoginIsRejectedWith401Json() throws Exception {
        mvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginWithSeededAdminCreatesSessionAndMeWorks() throws Exception {
        MockHttpSession session = login("admin", "admin123");
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = login("admin", "admin123");
        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me").session(new MockHttpSession()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void httpBasicAuthenticationAlsoWorks() throws Exception {
        mvc.perform(get("/api/admins").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    // ---------- Swagger UI ----------

    @Test
    void swaggerUiAndApiDocsArePublic() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/teachers/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"));
    }

    // ---------- Password hashing ----------

    @Test
    void passwordIsStoredAsBcryptHashAndNeverReturned() throws Exception {
        mvc.perform(post("/api/teachers").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(TEACHER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());

        Teacher saved = teacherRepository.findAll().get(0);
        assertThat(saved.getPassword()).startsWith("$2a$").isNotEqualTo("secret123");

        // and the new teacher can log in with the original password
        login("karim", "secret123");
    }

    // ---------- CRUD + status codes ----------

    @Test
    void fullCrudLifecycleForStudent() throws Exception {
        MockHttpSession admin = login("admin", "admin123");

        MvcResult created = mvc.perform(post("/api/students").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(STUDENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/students/")))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andReturn();
        String location = created.getResponse().getHeader("Location");

        mvc.perform(get(location).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("2007001"));

        mvc.perform(put(location).session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(STUDENT_JSON.replace("\"batch\":2020", "\"batch\":2021")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch").value(2021));

        mvc.perform(delete(location).session(admin))
                .andExpect(status().isNoContent());

        mvc.perform(get(location).session(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/students").with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"username\":\"x y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.batch").exists());
    }

    @Test
    void createWithoutPasswordReturns400() throws Exception {
        mvc.perform(post("/api/teachers").with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEACHER_JSON.replace("\"password\":\"secret123\",", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateUsernameAcrossRolesReturns409() throws Exception {
        String studentNamedAdmin = STUDENT_JSON.replace("\"username\":\"rahim\"", "\"username\":\"Admin\"");
        mvc.perform(post("/api/students").with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(studentNamedAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownIdReturns404AndBadIdReturns400() throws Exception {
        mvc.perform(get("/api/teachers/9999").with(user("a").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/teachers/abc").with(user("a").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lastAdminCannotBeDeleted() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        MvcResult me = mvc.perform(get("/api/auth/me").session(admin)).andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(me.getResponse().getContentAsString(), "$.id").toString();

        mvc.perform(delete("/api/admins/" + id).session(admin))
                .andExpect(status().isConflict());
    }

    // ---------- Role-based access control ----------

    @Test
    void adminCanManageAllThreeResources() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        mvc.perform(post("/api/admins").session(admin).contentType(MediaType.APPLICATION_JSON).content(ADMIN_JSON))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/teachers").session(admin).contentType(MediaType.APPLICATION_JSON).content(TEACHER_JSON))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/students").session(admin).contentType(MediaType.APPLICATION_JSON).content(STUDENT_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void teacherCanManageTeachersOnly() throws Exception {
        mvc.perform(post("/api/teachers").with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON).content(TEACHER_JSON)).andExpect(status().isCreated());
        MockHttpSession teacher = login("karim", "secret123");

        mvc.perform(get("/api/teachers").session(teacher)).andExpect(status().isOk());
        mvc.perform(post("/api/teachers").session(teacher).contentType(MediaType.APPLICATION_JSON)
                        .content(TEACHER_JSON.replace("karim", "salma")))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/students").session(teacher)).andExpect(status().isForbidden());
        mvc.perform(post("/api/students").session(teacher).contentType(MediaType.APPLICATION_JSON).content(STUDENT_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admins").session(teacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void studentCanManageStudentsOnly() throws Exception {
        mvc.perform(post("/api/students").with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON).content(STUDENT_JSON)).andExpect(status().isCreated());
        MockHttpSession student = login("rahim", "secret123");

        mvc.perform(get("/api/students").session(student)).andExpect(status().isOk());
        mvc.perform(post("/api/students").session(student).contentType(MediaType.APPLICATION_JSON)
                        .content(STUDENT_JSON.replace("rahim", "karima").replace("2007001", "2007002")))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/teachers").session(student)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admins/1").session(student)).andExpect(status().isForbidden());
    }
}
