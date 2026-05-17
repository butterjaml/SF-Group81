package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TAFeedbackServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void submitFeedbackShouldPromptOnlyAfterDeadlineAndUpdateReputationScore() {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);

        userRepository.saveNewUser("TA Leo", "20250031", "leo@example.com", SecurityUtil.sha256("password123"), UserRole.TA);
        userRepository.saveNewUser("MO Chen", "90021", "mo@example.com", SecurityUtil.sha256("password123"), UserRole.MO);

        PositionService positionService = new PositionService(positionRepository);
        positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP401",
                "Project Studio",
                "MO Chen",
                "2026S1",
                "Lead TA",
                1,
                LocalDate.now().plusDays(5).toString(),
                "PUBLISHED",
                "Studio TA",
                "Guide project teams",
                "6 hours/week",
                "100 yuan/hour",
                "Project experience",
                "",
                "",
                "U0002"
        ), "U0002");
        positionRepository.saveOrUpdate(new TAPosition(
                "P0002",
                "COMP402",
                "Legacy Systems",
                "MO Chen",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.now().minusDays(2).toString(),
                "PUBLISHED",
                "Legacy TA",
                "Support grading",
                "3 hours/week",
                "80 yuan/hour",
                "Course experience",
                "",
                "",
                "",
                "U0002",
                "2026-03-01T09:00:00",
                "2026-03-01T09:00:00"
        ));

        applicationRepository.saveOrUpdate(new TAApplication(
                "APP-U0001-P0001",
                "U0001",
                "P0001",
                1,
                ApplicationStatus.HIRED,
                "",
                "2026-03-20T10:00:00",
                "2026-03-29T10:00:00"
        ));
        applicationRepository.saveOrUpdate(new TAApplication(
                "APP-U0001-P0002",
                "U0001",
                "P0002",
                1,
                ApplicationStatus.HIRED,
                "",
                "2026-03-20T10:00:00",
                "2026-03-29T10:00:00"
        ));

        TAFeedbackService service = new TAFeedbackService(
                new TAFeedbackCsvRepository(tempDir),
                applicationRepository,
                positionRepository,
                userRepository
        );

        assertEquals(0, service.listPendingAssignments("U0002", LocalDate.now()).stream()
                .filter(item -> item.positionId().equals("P0001"))
                .count());
        assertEquals(1, service.listPendingAssignments("U0002", LocalDate.now()).stream()
                .filter(item -> item.positionId().equals("P0002"))
                .count());

        service.submitFeedback("U0002", "U0001", "P0002", 5, 4, 5, "Strong communicator and reliable.");

        assertEquals(0, service.listPendingAssignments("U0002", LocalDate.now()).stream()
                .filter(item -> item.positionId().equals("P0002"))
                .count());
        assertEquals(4.67, service.getReputationScore("U0001"));
    }
}
