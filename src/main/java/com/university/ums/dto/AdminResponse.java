package com.university.ums.dto;

import com.university.ums.model.Admin;
import com.university.ums.model.Role;

import java.time.Instant;

/** What the API sends back. No password field, so the hash can never leak. */
public record AdminResponse(
        Long id,
        String name,
        String email,
        String phone,
        String username,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminResponse from(Admin admin) {
        return new AdminResponse(
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getPhone(),
                admin.getUsername(),
                admin.getRole(),
                admin.getCreatedAt(),
                admin.getUpdatedAt());
    }
}
