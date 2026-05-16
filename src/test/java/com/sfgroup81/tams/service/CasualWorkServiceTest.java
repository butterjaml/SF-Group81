package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.CasualWorkApplicationCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkPostingCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CasualWorkServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createPostingAndApplyShouldSupportCurrentSemesterHiredTaFlow() {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        TAApplicationCsvRepository taApplicationRepository = new TAApplicationCsvRepository(tempDir);
        userRepository.saveNewUser("Admin", "80001", "admin@example.com", SecurityUtil.sha256("password123"), UserRole.ADMIN);
        userRepository.saveNewUser("TA Amy", "20250010", "amy@example.com", SecurityUtil.sha256("password123"), UserRole.TA, TACategory.NON_MODULAR);
        userRepository.saveNewUser("TA Max", "20250011", "max@example.com", SecurityUtil.sha256("password123"), UserRole.TA, TACategory.MODULAR);
        String currentSemester = LocalDate.now().getYear() + (LocalDate.now().getMonthValue() <= 6 ? "S1" : "S2");
        new PositionService(positionRepository).savePosition(new PositionUpsertRequest(
                "",
                "COMP701",
                "Capstone",
                "Admin",
                currentSemester,
                "Modular TA",
                1,
                LocalDate.now().plusDays(10).toString(),
                "PUBLISHED",
                "Capstone TA",
                "Support students",
                "4 hours/week",
                "100 yuan/hour",
                "Good communication",
                "",
                "",
                "U0001"
        ), "U0001");
        taApplicationRepository.saveOrUpdate(new TAApplication(
                "APP-U0003-P0001",
                "U0003",
                "P0001",
                1,
                ApplicationStatus.HIRED,
                "",
                "2026-04-01T09:00:00",
                "2026-04-01T09:00:00"
        ));

        CasualWorkService service = new CasualWorkService(
                new CasualWorkPostingCsvRepository(tempDir),
                new CasualWorkApplicationCsvRepository(tempDir),
                userRepository,
                taApplicationRepository,
                positionRepository
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

        service.apply("CW0001", "U0003", "I am modular and already hired this semester");
        assertThrows(IllegalArgumentException.class, () -> service.apply("CW0001", "U0002", "I am not hired this semester"));

        assertEquals(1, service.listOpenPostings().size());
        assertEquals(1, service.listApplicationsForPosting("CW0001").size());
        assertEquals("I am modular and already hired this semester", service.listApplicationsForPosting("CW0001").get(0).statement());
    }
}
