package com.university.ums.service;

import com.university.ums.dto.TeacherRequest;
import com.university.ums.dto.TeacherResponse;
import com.university.ums.exception.ResourceNotFoundException;
import com.university.ums.model.Teacher;
import com.university.ums.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserAccountService accounts;

    public TeacherService(TeacherRepository teacherRepository, UserAccountService accounts) {
        this.teacherRepository = teacherRepository;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll() {
        return teacherRepository.findAll().stream().map(TeacherResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        return TeacherResponse.from(getOrThrow(id));
    }

    public TeacherResponse create(TeacherRequest request) {
        accounts.requirePassword(request.password());
        accounts.ensureUnique(request.username(), request.email(), null);

        Teacher teacher = new Teacher();
        apply(teacher, request);
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = getOrThrow(id);
        accounts.ensureUnique(request.username(), request.email(), id);

        apply(teacher, request);
        return TeacherResponse.from(teacherRepository.saveAndFlush(teacher));
    }

    public void delete(Long id) {
        teacherRepository.delete(getOrThrow(id));
    }

    private void apply(Teacher teacher, TeacherRequest request) {
        accounts.applyCommonFields(teacher, request.name(), request.email(), request.phone(),
                request.username(), request.password());
        teacher.setDepartment(request.department().trim());
        teacher.setDesignation(request.designation().trim());
    }

    private Teacher getOrThrow(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
    }
}
