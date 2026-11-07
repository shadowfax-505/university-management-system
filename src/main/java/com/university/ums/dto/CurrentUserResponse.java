package com.university.ums.dto;

import com.university.ums.model.AppUser;
import com.university.ums.model.Role;

/** Returned by /api/auth/login and /api/auth/me: who is logged in and with which role. */
public record CurrentUserResponse(
        Long id,
        String name,
        String username,
        Role role
) {
    public static CurrentUserResponse from(AppUser user) {
        return new CurrentUserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole());
    }
}
