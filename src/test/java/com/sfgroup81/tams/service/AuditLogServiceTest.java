package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.AuditLogCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldStoreAndFilterAuditEvents() {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        userRepository.saveNewUser("Ava Chen", "80001", "admin@example.com", SecurityUtil.sha256("password123"), UserRole.ADMIN);
        userRepository.saveNewUser("Grace Liu", "90001", "mo@example.com", SecurityUtil.sha256("password123"), UserRole.MO);

        AuditLogService auditLogService = new AuditLogService(new AuditLogCsvRepository(tempDir), userRepository);
        auditLogService.record("LOGIN_SUCCESS", "U0001", "Admin signed in");
        auditLogService.record("USER_CREATED", "U0001", "Created MO account");
        auditLogService.record("POSITION_CREATED", "U0002", "Created COMP101 TA position");

        assertEquals(3, auditLogService.listEntries(new AuditLogFilter(null, null, "", "")).size());
        assertEquals(2, auditLogService.listEntries(new AuditLogFilter(null, null, "Ava", "")).size());
        assertEquals(1, auditLogService.listEntries(new AuditLogFilter(null, null, "", "POSITION_CREATED")).size());
        assertTrue(auditLogService.listEntries(new AuditLogFilter(LocalDate.now(), LocalDate.now(), "", ""))
                .stream()
                .allMatch(entry -> !entry.eventTime().isBlank()));
    }
}
