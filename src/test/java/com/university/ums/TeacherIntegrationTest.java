package com.university.ums;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeacherIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private static final String TEACHER_PAYLOAD = """
            {
                "name": "Dr. Hasan",
                "email": "hasan@uni.edu",
                "phone": "01722222222",
                "username": "hasan",
                "password": "teacherPass123",
                "department": "CSE",
                "designation": "Assistant Professor"
            }
            """;

    @Test
    void fullTeacherLifecycle_createReadUpdateDelete() throws Exception {
        MvcResult result = mvc.perform(post("/api/teachers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEACHER_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/teachers/")))
                .andExpect(jsonPath("$.username").value("hasan"))
                .andExpect(jsonPath("$.department").value("CSE"))
                .andExpect(jsonPath("$.designation").value("Assistant Professor"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        mvc.perform(get(location).with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("hasan@uni.edu"));

        String updatePayload = """
                {
                    "name": "Dr. Hasan",
                    "email": "hasan@uni.edu",
                    "phone": "01722222222",
                    "username": "hasan",
                    "department": "CSE",
                    "designation": "Associate Professor"
                }
                """;
        mvc.perform(put(location)
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designation").value("Associate Professor"));

        mvc.perform(delete(location).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());

        mvc.perform(get(location).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTeacher_duplicateUsername_returnsConflict() throws Exception {
        mvc.perform(post("/api/teachers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEACHER_PAYLOAD))
                .andExpect(status().isCreated());

        String duplicatePayload = """
                {
                    "name": "Another Teacher",
                    "email": "another@uni.edu",
                    "phone": "01733333333",
                    "username": "hasan",
                    "password": "newPass123",
                    "department": "EEE",
                    "designation": "Lecturer"
                }
                """;

        mvc.perform(post("/api/teachers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicatePayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'hasan' is already taken"));
    }

    @Test
    void unauthenticatedAccess_isRejected() throws Exception {
        mvc.perform(get("/api/teachers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEACHER_PAYLOAD))
                .andExpect(status().isUnauthorized());
    }
}
