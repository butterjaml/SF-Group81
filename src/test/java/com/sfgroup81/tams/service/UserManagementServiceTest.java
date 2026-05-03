package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.AuditLogCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UserManagementServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldCreateUpdateDisableAndResetUsers() {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        User admin = userRepository.saveNewUser("Ava Chen", "80001", "admin@example.com", SecurityUtil.sha256("password123"), UserRole.ADMIN);

        AuditLogService auditLogService = new AuditLogService(new AuditLogCsvRepository(tempDir), userRepository);
        UserManagementService service = new UserManagementService(userRepository, auditLogService);

        User created = service.createUser(new UserUpsertRequest(
                "Daniel Wu",
                "20250031",
                "daniel@example.com",
                "password123",
                UserRole.TA,
                TACategory.NON_MODULAR
        ), admin.userId());

        assertEquals(UserRole.TA, created.role());
        assertEquals(TACategory.NON_MODULAR, created.taCategory());

        User updatedRole = service.updateUserRoleAndCategory(created.userId(), UserRole.MO, TACategory.NONE, admin.userId());
        assertEquals(UserRole.MO, updatedRole.role());
        assertEquals(TACategory.NONE, updatedRole.taCategory());

        User disabled = service.updateAccountStatus(created.userId(), false, admin.userId());
        assertEquals("DISABLED", disabled.status());

        String oldHash = disabled.passwordHash();
        User reset = service.resetPassword(created.userId(), "newpass123", admin.userId());
        assertNotEquals(oldHash, reset.passwordHash());
        assertEquals(4, auditLogService.listEntries(new AuditLogFilter(null, null, "", "")).size());
    }
}
