package com.university.ums.dto;

import com.university.ums.model.Role;
import com.university.ums.model.Teacher;

import java.time.Instant;

public record TeacherResponse(
        Long id,
        String name,
        String email,
        String phone,
        String username,
        Role role,
        String department,
        String designation,
        Instant createdAt,
        Instant updatedAt
) {
    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getName(),
                teacher.getEmail(),
                teacher.getPhone(),
                teacher.getUsername(),
                teacher.getRole(),
                teacher.getDepartment(),
                teacher.getDesignation(),
                teacher.getCreatedAt(),
                teacher.getUpdatedAt());
    }
}
