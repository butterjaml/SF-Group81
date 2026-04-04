package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void closeExpiredPositionsShouldOnlyClosePublishedRows() {
        DataBootstrap.initialize(tempDir);
        PositionCsvRepository repository = new PositionCsvRepository(tempDir);
        PositionService service = new PositionService(repository);

        service.savePosition(new PositionUpsertRequest(
                "",
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
                "U0003"
        ), "U0003");

        service.savePosition(new PositionUpsertRequest(
                "",
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
                "U0003"
        ), "U0003");

        service.closeExpiredPositions(LocalDate.of(2026, 3, 29));

        assertEquals("CLOSED", repository.findById("P0001").orElseThrow().status());
        assertEquals("DRAFT", repository.findById("P0002").orElseThrow().status());
    }
}
