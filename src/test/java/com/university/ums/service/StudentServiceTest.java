package com.university.ums.service;

import com.university.ums.dto.StudentRequest;
import com.university.ums.dto.StudentResponse;
import com.university.ums.exception.ConflictException;
import com.university.ums.exception.ResourceNotFoundException;
import com.university.ums.model.Student;
import com.university.ums.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserAccountService accounts;

    @InjectMocks
    private StudentService studentService;

    @Test
    void create_validStudent_savesAndReturnsResponse() {
        StudentRequest request = new StudentRequest("Student One", "student@uni.edu", "01711111111",
                "student_one", "password123", "2007001", "CSE", 2020);
        Student savedStudent = new Student();
        ReflectionTestUtils.setField(savedStudent, "id", 5L);
        savedStudent.setName("Student One");
        savedStudent.setEmail("student@uni.edu");
        savedStudent.setUsername("student_one");
        savedStudent.setStudentId("2007001");
        savedStudent.setDepartment("CSE");
        savedStudent.setBatch(2020);

        when(studentRepository.existsByStudentId("2007001")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        StudentResponse response = studentService.create(request);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.studentId()).isEqualTo("2007001");
        verify(accounts).requirePassword("password123");
        verify(accounts).ensureUnique("student_one", "student@uni.edu", null);
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void create_duplicateStudentId_throwsConflictException() {
        StudentRequest request = new StudentRequest("Student One", "student@uni.edu", "01711111111",
                "student_one", "password123", "2007001", "CSE", 2020);

        when(studentRepository.existsByStudentId("2007001")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Student ID '2007001' is already registered");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void findById_nonExistingId_throwsResourceNotFoundException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student with id 99 not found");
    }
}
