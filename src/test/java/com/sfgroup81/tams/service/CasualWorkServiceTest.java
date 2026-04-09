package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.CasualWorkApplicationCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkPostingCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CasualWorkServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createPostingAndApplyShouldSupportAdminAndNonModularTaFlow() {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        userRepository.saveNewUser("Admin", "80001", "admin@example.com", SecurityUtil.sha256("password123"), UserRole.ADMIN);
        userRepository.saveNewUser("TA Amy", "20250010", "amy@example.com", SecurityUtil.sha256("password123"), UserRole.TA, TACategory.NON_MODULAR);
        userRepository.saveNewUser("TA Max", "20250011", "max@example.com", SecurityUtil.sha256("password123"), UserRole.TA, TACategory.MODULAR);

        CasualWorkService service = new CasualWorkService(
                new CasualWorkPostingCsvRepository(tempDir),
                new CasualWorkApplicationCsvRepository(tempDir),
                userRepository
        );

        service.createPosting(
                "Exam Invigilation",
                "Support final exam supervision",
                "2026-04-15",
                "Block A Room 301",
                "Reliable; punctual",
                3,
                "200 yuan/session",
                "U0001"
        );

        service.apply("CW0001", "U0002", "");
        assertThrows(IllegalArgumentException.class, () -> service.apply("CW0001", "U0003", "I am modular"));

        assertEquals(1, service.listOpenPostings().size());
        assertEquals(1, service.listApplicationsForPosting("CW0001").size());
        assertEquals("", service.listApplicationsForPosting("CW0001").getFirst().statement());
    }
}
