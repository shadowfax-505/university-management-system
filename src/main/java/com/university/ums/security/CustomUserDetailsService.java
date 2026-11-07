package com.university.ums.security;

import com.university.ums.model.AppUser;
import com.university.ums.repository.UserRepository;
import com.university.ums.service.UserAccountService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tells Spring Security how to find a user by username during login.
 * Spring then compares the typed password against the stored BCrypt hash itself.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(UserAccountService.normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // roles("TEACHER") becomes the authority "ROLE_TEACHER", which hasRole("TEACHER") checks
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
