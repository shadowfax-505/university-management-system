package com.university.ums.repository;

import com.university.ums.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Queries across all three roles at once (the shared "users" table). */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // "...AndIdNot": ignore the user being updated, so keeping your own username is not a conflict
    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
