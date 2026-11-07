package com.university.ums.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JsonSecurityHandlers jsonHandlers,
                                                   SecurityContextRepository securityContextRepository) throws Exception {
        http
                // REST API used by Postman/curl/JS, not HTML forms. The session cookie is SameSite=Lax
                // (application.yml), so browsers do not send it on cross-site POST/PUT/DELETE.
                .csrf(AbstractHttpConfigurer::disable)

                // RBAC: rules are checked top to bottom, first match wins
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Swagger UI page and the OpenAPI description it reads (docs only, no data)
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admins/**").hasRole("ADMIN")
                        .requestMatchers("/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                        .anyRequest().authenticated())

                // Logged-in user is kept in the HTTP session (JSESSIONID cookie)
                .securityContext(context -> context.securityContextRepository(securityContextRepository))

                // Also accept "Authorization: Basic ..." on every request (handy for Postman/curl)
                .httpBasic(basic -> basic.authenticationEntryPoint(jsonHandlers))

                // We use our own JSON login endpoint (AuthController), not the HTML login page
                .formLogin(AbstractHttpConfigurer::disable)

                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.POST, "/api/auth/logout"))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(jsonHandlers))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonHandlers)   // 401
                        .accessDeniedHandler(jsonHandlers));      // 403

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // BCrypt: slow, salted one-way hash. Same password -> different hash every time.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Built by Spring Boot from CustomUserDetailsService + PasswordEncoder; used by AuthController
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
