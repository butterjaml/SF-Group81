package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnrollmentServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void submitShouldSaveProfileResumeApplicationsAndInitialHistory() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository(tempDir);
        ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ApplicationStatusHistoryCsvRepository historyRepository = new ApplicationStatusHistoryCsvRepository(tempDir);

        userRepository.saveNewUser("Alice Tan", "20250001", "alice@example.com", SecurityUtil.sha256("password123"), UserRole.TA);
        PositionService positionService = new PositionService(positionRepository);
        positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP301",
                "Operating Systems",
                "Prof. Xu",
                "2026S1",
                "Lead TA",
                1,
                LocalDate.now().plusDays(10).toString(),
                "PUBLISHED",
                "COMP301 Lead TA",
                "Run office hours",
                "6 hours/week",
                "Base 90 yuan/hour",
                "A in COMP301",
                "Linux experience",
                "",
                "U0002"
        ), "U0002");
        positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP302",
                "Networks",
                "Prof. Guo",
                "2026S1",
                "Modular TA",
                2,
                LocalDate.now().plusDays(10).toString(),
                "PUBLISHED",
                "COMP302 TA",
                "Mark assignments",
                "5 hours/week",
                "Base 80 yuan/hour",
                "A in COMP302",
                "",
                "",
                "U0002"
        ), "U0002");

        ResumeUploadService resumeUploadService = new ResumeUploadService(tempDir, resumeRepository, userRepository);
        EnrollmentService service = new EnrollmentService(
                userRepository,
                positionRepository,
                profileRepository,
                resumeUploadService,
                applicationRepository,
                historyRepository,
                new ApplicationPreferenceCsvRepository(tempDir)
        );

        Path resumeFile = tempDir.resolve("alice_cv.pdf");
        Files.writeString(resumeFile, "resume");

        service.submit(new EnrollmentSubmission(
                "U0001",
                "18800001111",
                "Computer Science",
                "Year 3",
                "3.82",
                "Java; Linux; tutoring",
                "Weekday afternoons",
                "Interested in systems courses",
                resumeFile,
                List.of("P0001", "P0002")
        ));

        assertEquals("Computer Science", profileRepository.findByUserId("U0001").orElseThrow().major());
        assertEquals("resume_20250001_Alice_Tan.pdf", resumeRepository.findByApplicationId("APP-U0001-P0001").orElseThrow().autoFilename());
        assertEquals(2, applicationRepository.findByUserId("U0001").size());
        assertEquals(ApplicationStatus.PENDING_REVIEW, applicationRepository.findById("APP-U0001-P0001").orElseThrow().status());
        assertEquals(1, historyRepository.findByApplicationId("APP-U0001-P0001").size());
        assertEquals(List.of("COMP301", "COMP302"),
                new ApplicationPreferenceCsvRepository(tempDir).findByApplicationId("APP-U0001").stream()
                        .map(item -> item.courseId())
                        .toList());
        assertTrue(Files.readString(tempDir.resolve("application_preferences.csv")).contains("APP-U0001,COMP301,1"));
    }

    @Test
    void submitShouldRejectMoreThanThreeSelectedPositions() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);

        userRepository.saveNewUser("Bob Lee", "20250002", "bob@example.com", SecurityUtil.sha256("password123"), UserRole.TA);
        PositionService positionService = new PositionService(positionRepository);
        for (int i = 1; i <= 4; i++) {
            positionService.savePosition(new PositionUpsertRequest(
                    "",
                    "COMP40" + i,
                    "Course " + i,
                    "Prof. " + i,
                    "2026S1",
                    "Modular TA",
                    1,
                    LocalDate.now().plusDays(10).toString(),
                    "PUBLISHED",
                    "Position " + i,
                    "Support teaching",
                    "4 hours/week",
                    "Base 80 yuan/hour",
                    "Requirement " + i,
                    "",
                    "",
                    "U0002"
            ), "U0002");
        }

        EnrollmentService service = new EnrollmentService(
                userRepository,
                positionRepository,
                new ApplicantProfileCsvRepository(tempDir),
                new ResumeUploadService(tempDir, new ResumeFileCsvRepository(tempDir), userRepository),
                new TAApplicationCsvRepository(tempDir),
                new ApplicationStatusHistoryCsvRepository(tempDir),
                new ApplicationPreferenceCsvRepository(tempDir)
        );

        Path resumeFile = tempDir.resolve("bob_cv.pdf");
        Files.writeString(resumeFile, "resume");

        assertThrows(IllegalArgumentException.class, () -> service.submit(new EnrollmentSubmission(
                "U0001",
                "18800002222",
                "Software Engineering",
                "Year 2",
                "3.45",
                "Java",
                "Flexible",
                "",
                resumeFile,
                List.of("P0001", "P0002", "P0003", "P0004")
        )));
    }

    @Test
    void submitShouldRejectNonModularTaForFormalPositions() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);

        userRepository.saveNewUser("Dana Wong", "20250005", "dana@example.com", SecurityUtil.sha256("password123"), UserRole.TA, TACategory.NON_MODULAR);
        new PositionService(positionRepository).savePosition(new PositionUpsertRequest(
                "",
                "COMP450",
                "Formal Methods",
                "Prof. Ho",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.now().plusDays(10).toString(),
                "PUBLISHED",
                "COMP450 TA",
                "Support tutorials",
                "4 hours/week",
                "Base 80 yuan/hour",
                "Requirement",
                "",
                "",
                "U0002"
        ), "U0002");

        EnrollmentService service = new EnrollmentService(
                userRepository,
                positionRepository,
                new ApplicantProfileCsvRepository(tempDir),
                new ResumeUploadService(tempDir, new ResumeFileCsvRepository(tempDir), userRepository),
                new TAApplicationCsvRepository(tempDir),
                new ApplicationStatusHistoryCsvRepository(tempDir),
                new ApplicationPreferenceCsvRepository(tempDir)
        );

        Path resumeFile = tempDir.resolve("dana_cv.pdf");
        Files.writeString(resumeFile, "resume");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.submit(new EnrollmentSubmission(
                "U0001",
                "18800004444",
                "Software Engineering",
                "Year 2",
                "3.50",
                "Java",
                "Flexible",
                "",
                resumeFile,
                List.of("P0001")
        )));
        assertTrue(ex.getMessage().contains("Non-modular"));
    }
}
