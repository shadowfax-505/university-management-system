package com.university.ums.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.ums.dto.MessageResponse;
import com.university.ums.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security errors happen in filters, before any controller runs, so @RestControllerAdvice
 * cannot see them. These handlers write the JSON responses instead of Spring's default
 * HTML login page / browser pop-up.
 */
@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler, LogoutSuccessHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Not logged in (no session, no/invalid Basic credentials) -> 401 Unauthorized
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ApiError.of(401, "Unauthorized",
                "Authentication required. Log in at POST /api/auth/login", request.getRequestURI()));
    }

    // Logged in, but the role is not allowed for this URL -> 403 Forbidden
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ApiError.of(403, "Forbidden",
                "Your role is not allowed to access this resource", request.getRequestURI()));
    }

    // POST /api/auth/logout finished (session invalidated, cookie deleted) -> 200 OK
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        write(response, HttpStatus.OK, new MessageResponse("Logged out successfully"));
    }

    private void write(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
