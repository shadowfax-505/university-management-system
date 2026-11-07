package com.university.ums.service;

import com.university.ums.dto.StudentRequest;
import com.university.ums.dto.StudentResponse;
import com.university.ums.exception.ConflictException;
import com.university.ums.exception.ResourceNotFoundException;
import com.university.ums.model.Student;
import com.university.ums.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserAccountService accounts;

    public StudentService(StudentRepository studentRepository, UserAccountService accounts) {
        this.studentRepository = studentRepository;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        return studentRepository.findAll().stream().map(StudentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return StudentResponse.from(getOrThrow(id));
    }

    public StudentResponse create(StudentRequest request) {
        accounts.requirePassword(request.password());
        accounts.ensureUnique(request.username(), request.email(), null);
        if (studentRepository.existsByStudentId(request.studentId().trim())) {
            throw new ConflictException("Student ID '" + request.studentId().trim() + "' is already registered");
        }

        Student student = new Student();
        apply(student, request);
        return StudentResponse.from(studentRepository.save(student));
    }

    public StudentResponse update(Long id, StudentRequest request) {
        Student student = getOrThrow(id);
        accounts.ensureUnique(request.username(), request.email(), id);
        if (studentRepository.existsByStudentIdAndIdNot(request.studentId().trim(), id)) {
            throw new ConflictException("Student ID '" + request.studentId().trim() + "' is already registered");
        }

        apply(student, request);
        return StudentResponse.from(studentRepository.saveAndFlush(student));
    }

    public void delete(Long id) {
        studentRepository.delete(getOrThrow(id));
    }

    private void apply(Student student, StudentRequest request) {
        accounts.applyCommonFields(student, request.name(), request.email(), request.phone(),
                request.username(), request.password());
        student.setStudentId(request.studentId().trim());
        student.setDepartment(request.department().trim());
        student.setBatch(request.batch());
    }

    private Student getOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }
}
