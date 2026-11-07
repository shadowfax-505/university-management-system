package com.university.ums.service;

import com.university.ums.exception.BadRequestException;
import com.university.ums.exception.ConflictException;
import com.university.ums.model.AppUser;
import com.university.ums.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Login-account logic shared by the admin, teacher and student services. */
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Usernames and emails are stored lower-case so "Rahim" and "rahim" are the same account. */
    public static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BadRequestException("Password is required when creating a user");
        }
    }

    /**
     * Throws 409 if the username or email already belongs to someone else (in any role).
     * currentUserId is null on create, or the id of the user being updated.
     */
    public void ensureUnique(String username, String email, Long currentUserId) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);

        boolean usernameTaken = currentUserId == null
                ? userRepository.existsByUsername(normalizedUsername)
                : userRepository.existsByUsernameAndIdNot(normalizedUsername, currentUserId);
        if (usernameTaken) {
            throw new ConflictException("Username '" + normalizedUsername + "' is already taken");
        }

        boolean emailTaken = currentUserId == null
                ? userRepository.existsByEmail(normalizedEmail)
                : userRepository.existsByEmailAndIdNot(normalizedEmail, currentUserId);
        if (emailTaken) {
            throw new ConflictException("Email '" + normalizedEmail + "' is already in use");
        }
    }

    /** Copies the common fields onto the entity. The password is hashed with BCrypt, never stored raw. */
    public void applyCommonFields(AppUser user, String name, String email, String phone,
                                  String username, String rawPassword) {
        user.setName(name.trim());
        user.setEmail(normalize(email));
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        user.setUsername(normalize(username));
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
    }
}
