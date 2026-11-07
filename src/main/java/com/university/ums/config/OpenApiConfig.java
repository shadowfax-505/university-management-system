package com.university.ums.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Describes the API for Swagger UI (http://localhost:8080/swagger-ui.html).
 * springdoc scans the controllers for endpoints; this bean adds the title, the
 * "Authorize" button (HTTP Basic) and the logout endpoint, which is a Spring Security
 * filter rather than a controller method, so the scanner cannot find it.
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI universityOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("University Management System API")
                        .version("1.0.0")
                        .description("""
                                CSE 3230 Lab 4. Log in either way:
                                1. Click **Authorize** and enter a username/password (HTTP Basic), or
                                2. Call **POST /api/auth/login**; the browser keeps the session cookie.

                                Seeded admin: `admin` / `admin123`."""))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
                .path("/api/auth/logout", new PathItem().post(new Operation()
                        .tags(List.of("Authentication"))
                        .summary("Log out: invalidates the session and deletes the JSESSIONID cookie")
                        .responses(new ApiResponses().addApiResponse("200",
                                new ApiResponse().description("Logged out successfully")))));
    }
}
