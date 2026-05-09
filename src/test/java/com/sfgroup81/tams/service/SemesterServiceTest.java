package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.EnrollmentProfileSnapshotCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.SemesterCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SemesterServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldPreserveDifferentSemesterApplicationsAndFilterByViewedSemester() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository(tempDir);
        ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ApplicationStatusHistoryCsvRepository historyRepository = new ApplicationStatusHistoryCsvRepository(tempDir);
        SemesterService semesterService = new SemesterService(new SemesterCsvRepository(tempDir), positionRepository, AuditLogService.noop());

        userRepository.saveNewUser("TA Lin", "20250009", "ta@example.com", SecurityUtil.sha256("password123"), UserRole.TA);
        userRepository.saveNewUser("MO Xu", "90008", "mo@example.com", SecurityUtil.sha256("password123"), UserRole.MO);

        PositionService positionService = new PositionService(positionRepository, semesterService, AuditLogService.noop());
        positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP201",
                "Algorithms",
                "MO Xu",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.now().plusDays(5).toString(),
                "PUBLISHED",
                "Algorithms TA",
                "Support labs",
                "4 hours/week",
                "90 yuan/hour",
                "Algorithms; Java",
                "Tutoring",
                "",
                "Algorithms=60; Java=40",
                "U0002"
        ), "U0002");

        EnrollmentService enrollmentService = new EnrollmentService(
                userRepository,
                positionRepository,
                profileRepository,
                new ResumeUploadService(tempDir, resumeRepository, userRepository),
                applicationRepository,
                historyRepository,
                new ApplicationPreferenceCsvRepository(tempDir),
                new EnrollmentProfileSnapshotCsvRepository(tempDir),
                AuditLogService.noop()
        );
        Path firstResume = tempDir.resolve("first_resume.pdf");
        Files.writeString(firstResume, "Algorithms and Java.");
        enrollmentService.submit(new EnrollmentSubmission(
                "U0001",
                "18800009999",
                "Computer Science",
                "Year 3",
                "3.80",
                "Algorithms; Java",
                "Flexible",
                "First semester",
                firstResume,
                List.of("P0001")
        ));

        semesterService.createAndSwitchToNewSemester("2026S2", "U0002", "New TA cycle");
        positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP202",
                "Operating Systems",
                "MO Xu",
                "2026S2",
                "Lead TA",
                1,
                LocalDate.now().plusDays(10).toString(),
                "PUBLISHED",
                "OS Lead TA",
                "Support studio",
                "6 hours/week",
                "100 yuan/hour",
                "C; concurrency",
                "Project mentoring",
                "",
                "C=55; concurrency=45",
                "U0002"
        ), "U0002");
        Path secondResume = tempDir.resolve("second_resume.doc");
        Files.writeString(secondResume, "C programming and concurrency.");
        enrollmentService.submit(new EnrollmentSubmission(
                "U0001",
                "18800009999",
                "Computer Science",
                "Year 4",
                "3.92",
                "C; concurrency",
                "Weekdays",
                "Second semester",
                secondResume,
                List.of("P0002")
        ));

        assertEquals(2, applicationRepository.findByUserId("U0001").size());
        assertEquals(2, profileRepository.findAll().size());

        ApplicationStatusService statusService = new ApplicationStatusService(
                applicationRepository,
                historyRepository,
                positionRepository,
                semesterService,
                AuditLogService.noop()
        );

        assertEquals("P0002", statusService.listForApplicant("U0001").get(0).application().positionId());
        semesterService.switchViewedSemester("2026S1", "U0002");
        assertEquals("P0001", statusService.listForApplicant("U0001").get(0).application().positionId());
        assertEquals("2026S1", positionService.listAll().get(0).semesterId());
    }
}
