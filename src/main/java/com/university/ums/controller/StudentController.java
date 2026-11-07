package com.university.ums.controller;

import com.university.ums.dto.StudentRequest;
import com.university.ums.dto.StudentResponse;
import com.university.ums.service.StudentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * CRUD endpoints for students. Who may call them is decided in SecurityConfig
 * (/api/students/** rule), not here.
 */
@Tag(name = "Students", description = "Manage students (ADMIN or STUDENT)")
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // GET /api/students -> 200 OK + list
    @GetMapping
    public List<StudentResponse> getAll() {
        return studentService.findAll();
    }

    // GET /api/students/{id} -> 200 OK, or 404 Not Found
    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    // POST /api/students -> 201 Created + Location header pointing at the new resource
    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentResponse created = studentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // PUT /api/students/{id} -> 200 OK + updated resource
    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request);
    }

    // DELETE /api/students/{id} -> 204 No Content
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }
}
