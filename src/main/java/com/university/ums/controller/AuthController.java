package com.university.ums.controller;

import com.university.ums.dto.CurrentUserResponse;
import com.university.ums.dto.LoginRequest;
import com.university.ums.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login and "who am I". Logout is handled by Spring Security's LogoutFilter
 * at POST /api/auth/logout (see SecurityConfig), so there is no method for it here.
 */
@Tag(name = "Authentication", description = "Log in, see who is logged in, log out")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userRepository = userRepository;
    }

    // POST /api/auth/login {"username": "...", "password": "..."} -> 200 + JSESSIONID cookie, or 401
    @PostMapping("/login")
    public CurrentUserResponse login(@Valid @RequestBody LoginRequest request,
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        // 1. Check username + password (loads user via CustomUserDetailsService, compares BCrypt hash).
        //    Wrong credentials throw BadCredentialsException -> 401 in GlobalExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

        // 2. Session fixation protection: never keep a session id that existed before login.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        // 3. Store the logged-in user in the HTTP session so later requests are authenticated
        //    just by sending the JSESSIONID cookie.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return currentUser(authentication);
    }

    // GET /api/auth/me -> 200 with the current user's id, name, username and role
    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        return currentUser(authentication);
    }

    private CurrentUserResponse currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(CurrentUserResponse::from)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }
}
