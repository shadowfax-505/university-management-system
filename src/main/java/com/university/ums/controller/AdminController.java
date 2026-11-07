package com.university.ums.controller;

import com.university.ums.dto.AdminRequest;
import com.university.ums.dto.AdminResponse;
import com.university.ums.service.AdminService;
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
 * CRUD endpoints for admins. Who may call them is decided in SecurityConfig
 * (/api/admins/** rule), not here.
 */
@Tag(name = "Admins", description = "Manage admins (ADMIN only)")
@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/admins -> 200 OK + list
    @GetMapping
    public List<AdminResponse> getAll() {
        return adminService.findAll();
    }

    // GET /api/admins/{id} -> 200 OK, or 404 Not Found
    @GetMapping("/{id}")
    public AdminResponse getById(@PathVariable Long id) {
        return adminService.findById(id);
    }

    // POST /api/admins -> 201 Created + Location header pointing at the new resource
    @PostMapping
    public ResponseEntity<AdminResponse> create(@Valid @RequestBody AdminRequest request) {
        AdminResponse created = adminService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // PUT /api/admins/{id} -> 200 OK + updated resource
    @PutMapping("/{id}")
    public AdminResponse update(@PathVariable Long id, @Valid @RequestBody AdminRequest request) {
        return adminService.update(id, request);
    }

    // DELETE /api/admins/{id} -> 204 No Content
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adminService.delete(id);
    }
}
