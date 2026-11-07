package com.university.ums.service;

import com.university.ums.dto.AdminRequest;
import com.university.ums.dto.AdminResponse;
import com.university.ums.exception.ConflictException;
import com.university.ums.exception.ResourceNotFoundException;
import com.university.ums.model.Admin;
import com.university.ums.repository.AdminRepository;
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
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private UserAccountService accounts;

    @InjectMocks
    private AdminService adminService;

    @Test
    void create_validAdmin_savesAndReturnsResponse() {
        AdminRequest request = new AdminRequest("Admin User", "admin@uni.edu", "01700000000", "admin_user", "password123");
        Admin savedAdmin = new Admin();
        ReflectionTestUtils.setField(savedAdmin, "id", 10L);
        savedAdmin.setName("Admin User");
        savedAdmin.setEmail("admin@uni.edu");
        savedAdmin.setUsername("admin_user");

        when(adminRepository.save(any(Admin.class))).thenReturn(savedAdmin);

        AdminResponse response = adminService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.username()).isEqualTo("admin_user");
        verify(accounts).requirePassword("password123");
        verify(accounts).ensureUnique("admin_user", "admin@uni.edu", null);
        verify(adminRepository).save(any(Admin.class));
    }

    @Test
    void findById_nonExistingId_throwsResourceNotFoundException() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Admin with id 99 not found");
    }

    @Test
    void delete_lastAdmin_throwsConflictException() {
        Admin admin = new Admin();
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> adminService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot delete the last admin account");

        verify(adminRepository, never()).delete(any());
    }
}
