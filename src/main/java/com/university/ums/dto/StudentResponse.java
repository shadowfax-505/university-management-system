package com.university.ums.dto;

import com.university.ums.model.Role;
import com.university.ums.model.Student;

import java.time.Instant;

public record StudentResponse(
        Long id,
        String name,
        String email,
        String phone,
        String username,
        Role role,
        String studentId,
        String department,
        Integer batch,
        Instant createdAt,
        Instant updatedAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getUsername(),
                student.getRole(),
                student.getStudentId(),
                student.getDepartment(),
                student.getBatch(),
                student.getCreatedAt(),
                student.getUpdatedAt());
    }
}
