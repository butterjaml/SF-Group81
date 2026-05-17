package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.SemesterCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void savePublishedPositionShouldPersistSprintOneDetails() {
        DataBootstrap.initialize(tempDir);
        PositionCsvRepository repository = new PositionCsvRepository(tempDir);
        PositionService service = new PositionService(repository);

        TAPosition saved = service.savePosition(new PositionUpsertRequest(
                "",
                "COMP101",
                "Software Engineering",
                "Dr. Li",
                "2026S1",
                "Lead TA",
                2,
                LocalDate.now().plusDays(7).toString(),
                "PUBLISHED",
                "COMP101 Lead TA",
                "Run weekly labs and office hours",
                "6 hours/week including 2 office hours",
                "Base 80 yuan/hour; marking 15 yuan/report",
                "Completed COMP101 with A",
                "Prior tutoring experience",
                "Bilingual support",
                "U0002"
        ), "U0002");

        assertEquals("P0001", saved.positionId());
        assertEquals("Software Engineering", saved.courseName());
        assertEquals("Dr. Li", saved.instructorName());
        assertEquals("Base 80 yuan/hour; marking 15 yuan/report", saved.salaryInfo());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void savePositionShouldPreserveMultilineRequirements() {
        DataBootstrap.initialize(tempDir);
        PositionCsvRepository repository = new PositionCsvRepository(tempDir);
        PositionService service = new PositionService(repository);

        String mandatoryRequirements = "First requirement\nSecond requirement with comma, and \"quote\"";
        TAPosition saved = service.savePosition(new PositionUpsertRequest(
                "",
                "COMP301",
                "Human Computer Interaction",
                "Dr. Ng",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.now().plusDays(7).toString(),
                "PUBLISHED",
                "COMP301 Studio TA",
                "Run studio sessions",
                "5 hours/week",
                "Base 90 yuan/hour",
                mandatoryRequirements,
                "Figma experience",
                "Portfolio review",
                "U0002"
        ), "U0002");

        TAPosition reloaded = repository.findById(saved.positionId()).orElseThrow();
        assertEquals(mandatoryRequirements, reloaded.mandatoryRequirements());
        assertTrue(service.listOpenPublishedPositions().stream()
                .anyMatch(position -> saved.positionId().equals(position.positionId())));
    }

    @Test
    void moShouldOnlyListAndModifyOwnPositions() {
        DataBootstrap.initialize(tempDir);
        PositionCsvRepository repository = new PositionCsvRepository(tempDir);
        PositionService service = new PositionService(repository);

        service.savePosition(new PositionUpsertRequest(
                "",
                "COMP401",
                "Project Studio",
                "Grace Liu",
                "2026S1",
                "Lead TA",
                1,
                LocalDate.now().plusDays(7).toString(),
                "PUBLISHED",
                "COMP401 Lead TA",
                "Coach project teams",
                "6 hours/week",
                "Base 110 yuan/hour",
                "Project mentoring",
                "",
                "",
                "U0002"
        ), "U0002");
        service.savePosition(new PositionUpsertRequest(
                "",
                "COMP402",
                "Legacy Systems",
                "Daniel Wu",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.now().plusDays(7).toString(),
                "PUBLISHED",
                "COMP402 Support TA",
                "Support grading",
                "4 hours/week",
                "Base 85 yuan/hour",
                "Completed COMP402",
                "",
                "",
                "U0003"
        ), "U0003");

        assertEquals(List.of("P0001"), service.listByCreator("U0002").stream()
                .map(TAPosition::positionId)
                .toList());
        IllegalArgumentException editError = assertThrows(IllegalArgumentException.class, () -> service.savePosition(new PositionUpsertRequest(
                "P0002",
                "COMP402",
                "Edited By Wrong MO",
                "Daniel Wu",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.now().plusDays(7).toString(),
                "PUBLISHED",
                "Edited",
                "Support grading",
                "4 hours/week",
                "Base 85 yuan/hour",
                "Completed COMP402",
                "",
                "",
                "U0002"
        ), "U0002"));
        assertEquals("Only the MO who created this position can modify it", editError.getMessage());

        IllegalArgumentException unpublishError = assertThrows(IllegalArgumentException.class,
                () -> service.unpublish("P0002", "U0002"));
        assertEquals("Only the MO who created this position can modify it", unpublishError.getMessage());
    }

    @Test
    void savePositionShouldRejectSemesterOutsideCurrentView() {
        DataBootstrap.initialize(tempDir);
        PositionCsvRepository repository = new PositionCsvRepository(tempDir);
        SemesterService semesterService = new SemesterService(new SemesterCsvRepository(tempDir), repository, AuditLogService.noop());
        semesterService.createAndSwitchToNewSemester("2026S2", "U0001", "Current recruitment cycle");
        PositionService service = new PositionService(repository, semesterService, AuditLogService.noop());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.savePosition(new PositionUpsertRequest(
                "",
                "COMP490",
                "Research Methods",
                "Grace Liu",
                "2026S1",
                "Lead TA",
                1,
                LocalDate.now().plusDays(7).toString(),
                "PUBLISHED",
                "COMP490 Lead TA",
                "Lead seminars",
                "5 hours/week",
                "Base 100 yuan/hour",
                "Research methods background",
                "",
                "",
                "U0002"
        ), "U0002"));

        assertEquals("Position semester must match the current viewed semester (2026S2)", ex.getMessage());
        assertEquals(0, repository.findAll().size());
    }

    @Test
    void closeExpiredPositionsShouldOnlyClosePublishedRows() {
        DataBootstrap.initialize(tempDir);
        PositionCsvRepository repository = new PositionCsvRepository(tempDir);
        PositionService service = new PositionService(repository);

        repository.saveOrUpdate(new TAPosition(
                "P0001",
                "COMP201",
                "Algorithms",
                "Prof. Chen",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.of(2026, 3, 1).toString(),
                "PUBLISHED",
                "COMP201 TA",
                "Support tutorials",
                "4 hours/week",
                "Base 75 yuan/hour",
                "Strong algorithms grade",
                "",
                "",
                "",
                "U0003",
                "2026-02-01T09:00:00",
                "2026-02-01T09:00:00"
        ));

        repository.saveOrUpdate(new TAPosition(
                "P0002",
                "COMP202",
                "Databases",
                "Prof. Wang",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.of(2026, 3, 1).toString(),
                "DRAFT",
                "COMP202 TA",
                "Support tutorials",
                "4 hours/week",
                "Base 75 yuan/hour",
                "Strong database grade",
                "",
                "",
                "",
                "U0003",
                "2026-02-01T09:00:00",
                "2026-02-01T09:00:00"
        ));

        service.closeExpiredPositions(LocalDate.of(2026, 3, 29));

        assertEquals("CLOSED", repository.findById("P0001").orElseThrow().status());
        assertEquals("DRAFT", repository.findById("P0002").orElseThrow().status());
    }

    @Test
    void savePositionShouldRejectInvalidDeadlineFormat() {
        DataBootstrap.initialize(tempDir);
        PositionService service = new PositionService(new PositionCsvRepository(tempDir));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.savePosition(new PositionUpsertRequest(
                "",
                "COMP901",
                "Compiler Design",
                "Dr. Li",
                "2026S1",
                "Modular TA",
                1,
                "04/15/2026",
                "PUBLISHED",
                "Compiler TA",
                "Support labs",
                "4 hours/week",
                "Base 80 yuan/hour",
                "Requirement",
                "",
                "",
                "U0002"
        ), "U0002"));

        assertEquals("Deadline must use YYYY-MM-DD format, for example 2026-04-15", ex.getMessage());
    }

    @Test
    void savePublishedPositionShouldRejectPastDeadlineBecauseTaCannotSelectIt() {
        DataBootstrap.initialize(tempDir);
        PositionService service = new PositionService(new PositionCsvRepository(tempDir));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.savePosition(new PositionUpsertRequest(
                "",
                "COMP902",
                "Software Quality",
                "Dr. Li",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.now().minusDays(1).toString(),
                "PUBLISHED",
                "Quality TA",
                "Support labs",
                "4 hours/week",
                "Base 80 yuan/hour",
                "Requirement",
                "",
                "",
                "Requirement=100",
                "U0002"
        ), "U0002"));

        assertEquals("Published positions must use today or a future deadline so TAs can select the job", ex.getMessage());
    }

    @Test
    void savePositionShouldValidateAiSkillWeightFormat() {
        DataBootstrap.initialize(tempDir);
        PositionService service = new PositionService(new PositionCsvRepository(tempDir));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.savePosition(new PositionUpsertRequest(
                "",
                "COMP903",
                "AI Tools",
                "Dr. Li",
                "2026S1",
                "Modular TA",
                1,
                LocalDate.now().plusDays(7).toString(),
                "PUBLISHED",
                "AI Tools TA",
                "Support labs",
                "4 hours/week",
                "Base 80 yuan/hour",
                "Requirement",
                "",
                "",
                "Java=70; Teaching=20",
                "U0002"
        ), "U0002"));

        assertTrue(ex.getMessage().contains("add up to 100"));
    }
}
