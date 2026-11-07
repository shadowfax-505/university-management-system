package com.university.ums.repository;

import com.university.ums.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentId(String studentId);

    boolean existsByStudentIdAndIdNot(String studentId, Long id);
}
