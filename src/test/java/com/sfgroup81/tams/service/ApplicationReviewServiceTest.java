package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.ApplicationStatus;
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

class ApplicationReviewServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void updateStatusShouldAppendHistoryAndExposeLatestFeedback() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository(tempDir);
        ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ApplicationStatusHistoryCsvRepository historyRepository = new ApplicationStatusHistoryCsvRepository(tempDir);

        userRepository.saveNewUser("Carol Sun", "20250003", "carol@example.com", SecurityUtil.sha256("password123"), UserRole.TA);
        userRepository.saveNewUser("MO Zhang", "90001", "mo@example.com", SecurityUtil.sha256("password123"), UserRole.MO);

        PositionService positionService = new PositionService(positionRepository);
        positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP501",
                "Distributed Systems",
                "MO Zhang",
                "2026S1",
                "Lead TA",
                1,
                LocalDate.now().plusDays(10).toString(),
                "PUBLISHED",
                "COMP501 Lead TA",
                "Help with project mentoring",
                "6 hours/week",
                "Base 100 yuan/hour",
                "Distributed systems background",
                "Teaching experience",
                "",
                "U0002"
        ), "U0002");

        EnrollmentService enrollmentService = new EnrollmentService(
                userRepository,
                positionRepository,
                profileRepository,
                new ResumeUploadService(tempDir, resumeRepository, userRepository),
                applicationRepository,
                historyRepository,
                new ApplicationPreferenceCsvRepository(tempDir)
        );
        Path resumeFile = tempDir.resolve("carol_cv.pdf");
        Files.writeString(resumeFile, "resume");
        enrollmentService.submit(new EnrollmentSubmission(
                "U0001",
                "18800003333",
                "Computer Science",
                "Year 4",
                "3.91",
                "Distributed systems; Java",
                "Flexible",
                "",
                resumeFile,
                List.of("P0001")
        ));

        ApplicationReviewService reviewService = new ApplicationReviewService(
                applicationRepository,
                historyRepository,
                positionRepository
        );
        ApplicationStatusService statusService = new ApplicationStatusService(
                applicationRepository,
                historyRepository,
                positionRepository
        );

        reviewService.updateStatus("APP-U0001-P0001", ApplicationStatus.INTERVIEW, "Interview on Tuesday", "U0002");
        reviewService.updateStatus("APP-U0001-P0001", ApplicationStatus.REJECTED, "Role filled by another candidate", "U0002");

        ApplicantApplicationView view = statusService.listForApplicant("U0001").getFirst();
        assertEquals(ApplicationStatus.REJECTED, view.application().status());
        assertEquals("Role filled by another candidate", view.application().feedback());
        assertEquals(3, view.history().size());
        assertEquals("Interview on Tuesday", view.history().get(1).note());
    }
}
