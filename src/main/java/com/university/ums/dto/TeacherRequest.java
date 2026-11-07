package com.university.ums.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * JSON body for POST /api/teachers and PUT /api/teachers/{id}.
 * password is required on create, optional on update (omit it to keep the old one).
 */
public record TeacherRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "may contain only letters, digits, '.', '_' and '-'")
        String username,
        @Size(min = 6, max = 72) String password,
        @NotBlank @Size(max = 100) String department,
        @NotBlank @Size(max = 50) String designation
) {
}
