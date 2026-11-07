package com.university.ums.controller;

import com.university.ums.dto.TeacherRequest;
import com.university.ums.dto.TeacherResponse;
import com.university.ums.service.TeacherService;
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
 * CRUD endpoints for teachers. Who may call them is decided in SecurityConfig
 * (/api/teachers/** rule), not here.
 */
@Tag(name = "Teachers", description = "Manage teachers (ADMIN or TEACHER)")
@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    // GET /api/teachers -> 200 OK + list
    @GetMapping
    public List<TeacherResponse> getAll() {
        return teacherService.findAll();
    }

    // GET /api/teachers/{id} -> 200 OK, or 404 Not Found
    @GetMapping("/{id}")
    public TeacherResponse getById(@PathVariable Long id) {
        return teacherService.findById(id);
    }

    // POST /api/teachers -> 201 Created + Location header pointing at the new resource
    @PostMapping
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        TeacherResponse created = teacherService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // PUT /api/teachers/{id} -> 200 OK + updated resource
    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return teacherService.update(id, request);
    }

    // DELETE /api/teachers/{id} -> 204 No Content
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teacherService.delete(id);
    }
}
