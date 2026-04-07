package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.InterviewInvitation;
import com.sfgroup81.tams.model.InterviewResponseStatus;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.InterviewInvitationCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void scheduleAndRespondShouldPersistInterviewFlowAndReminder() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository(tempDir);
        ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ApplicationStatusHistoryCsvRepository historyRepository = new ApplicationStatusHistoryCsvRepository(tempDir);
        InterviewInvitationCsvRepository invitationRepository = new InterviewInvitationCsvRepository(tempDir);

        userRepository.saveNewUser("TA Alice", "20250001", "alice@example.com", SecurityUtil.sha256("password123"), UserRole.TA);
        userRepository.saveNewUser("MO Wang", "90001", "mo@example.com", SecurityUtil.sha256("password123"), UserRole.MO);

        PositionService positionService = new PositionService(positionRepository);
        positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP601",
                "Machine Learning",
                "MO Wang",
                "2026S1",
                "Lead TA",
                1,
                LocalDate.now().plusDays(10).toString(),
                "PUBLISHED",
                "COMP601 Lead TA",
                "Lead tutorials",
                "8 hours/week",
                "120 yuan/hour",
                "Strong ML grade",
                "Python experience",
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
        Path resumeFile = tempDir.resolve("alice_cv.pdf");
        Files.writeString(resumeFile, "resume");
        enrollmentService.submit(new EnrollmentSubmission(
                "U0001",
                "18800001111",
                "Computer Science",
                "Year 4",
                "3.92",
                "Python; tutoring",
                "Flexible",
                "",
                resumeFile,
                List.of("P0001")
        ));

        InterviewService service = new InterviewService(
                applicationRepository,
                historyRepository,
                invitationRepository
        );

        String scheduledAt = LocalDateTime.now().plusHours(20).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        InterviewInvitation invitation = service.scheduleInterview(
                "APP-U0001-P0001",
                scheduledAt,
                "Room 502",
                "Bring your transcript",
                "U0002"
        );

        assertEquals(ApplicationStatus.INTERVIEW, applicationRepository.findById("APP-U0001-P0001").orElseThrow().status());
        assertEquals(InterviewResponseStatus.PENDING_CONFIRMATION, invitation.responseStatus());
        assertEquals(2, historyRepository.findByApplicationId("APP-U0001-P0001").size());
        assertTrue(service.listReminderMessages("U0001", LocalDateTime.now()).getFirst().contains("Room 502"));

        InterviewInvitation updated = service.respondToInterview(
                invitation.invitationId(),
                InterviewResponseStatus.CONFIRMED,
                "I will attend on time.",
                "U0001"
        );

        assertEquals(InterviewResponseStatus.CONFIRMED, updated.responseStatus());
        assertEquals("I will attend on time.", updated.responseNote());
        assertEquals(3, historyRepository.findByApplicationId("APP-U0001-P0001").size());
        assertTrue(historyRepository.findByApplicationId("APP-U0001-P0001").getLast().note().contains("confirmed"));
        assertEquals(InterviewResponseStatus.CONFIRMED,
                service.findLatestInvitationForApplication("APP-U0001-P0001").orElseThrow().responseStatus());
        assertEquals("I will attend on time.",
                service.findLatestInvitationForApplication("APP-U0001-P0001").orElseThrow().responseNote());
    }
}
