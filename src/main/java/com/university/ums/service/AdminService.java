package com.university.ums.service;

import com.university.ums.dto.AdminRequest;
import com.university.ums.dto.AdminResponse;
import com.university.ums.exception.ConflictException;
import com.university.ums.exception.ResourceNotFoundException;
import com.university.ums.model.Admin;
import com.university.ums.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final UserAccountService accounts;

    public AdminService(AdminRepository adminRepository, UserAccountService accounts) {
        this.adminRepository = adminRepository;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> findAll() {
        return adminRepository.findAll().stream().map(AdminResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminResponse findById(Long id) {
        return AdminResponse.from(getOrThrow(id));
    }

    public AdminResponse create(AdminRequest request) {
        accounts.requirePassword(request.password());
        accounts.ensureUnique(request.username(), request.email(), null);

        Admin admin = new Admin();
        apply(admin, request);
        return AdminResponse.from(adminRepository.save(admin));
    }

    public AdminResponse update(Long id, AdminRequest request) {
        Admin admin = getOrThrow(id);
        accounts.ensureUnique(request.username(), request.email(), id);

        apply(admin, request);
        // saveAndFlush runs the UPDATE now, so the response already shows the new updatedAt
        return AdminResponse.from(adminRepository.saveAndFlush(admin));
    }

    public void delete(Long id) {
        Admin admin = getOrThrow(id);
        // Without at least one admin nobody could ever create admins again
        if (adminRepository.count() <= 1) {
            throw new ConflictException("Cannot delete the last admin account");
        }
        adminRepository.delete(admin);
    }

    private void apply(Admin admin, AdminRequest request) {
        accounts.applyCommonFields(admin, request.name(), request.email(), request.phone(),
                request.username(), request.password());
    }

    private Admin getOrThrow(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", id));
    }
}
