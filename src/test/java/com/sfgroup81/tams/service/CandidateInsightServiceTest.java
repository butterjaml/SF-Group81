package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.InternalReferralCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateInsightServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void exportAndReferralFilterShouldReflectSprintTwoCandidateInsights() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository(tempDir);

        userRepository.saveNewUser("TA Nora", "20250021", "nora@example.com", SecurityUtil.sha256("password123"), UserRole.TA);
        userRepository.saveNewUser("MO Lin", "90011", "mo@example.com", SecurityUtil.sha256("password123"), UserRole.MO);

        profileRepository.saveOrUpdate(new com.sfgroup81.tams.model.ApplicantProfile(
                "U0001",
                "18800009999",
                "Software Engineering",
                "Year 3",
                "3.88",
                "Java; SQL",
                "Weekends",
                "",
                "2026-03-29T10:00:00"
        ));

        PositionService positionService = new PositionService(positionRepository);
        positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP305",
                "Software Construction",
                "MO Lin",
                "2026S1",
                "Modular TA",
                2,
                LocalDate.now().plusDays(7).toString(),
                "PUBLISHED",
                "Construction TA",
                "Support marking",
                "4 hours/week",
                "90 yuan/hour",
                "Course completed",
                "",
                "",
                "U0002"
        ), "U0002");

        applicationRepository.saveOrUpdate(new com.sfgroup81.tams.model.TAApplication(
                "APP-U0001-P0001",
                "U0001",
                "P0001",
                1,
                com.sfgroup81.tams.model.ApplicationStatus.PENDING_REVIEW,
                "",
                "2026-03-29T09:00:00",
                "2026-03-29T09:00:00"
        ));
        Path resumeFile = tempDir.resolve("nora_resume.pdf");
        Files.writeString(resumeFile, "resume");
        resumeRepository.saveOrUpdate("APP-U0001-P0001", resumeFile.toString(), "PDF", "nora_resume.pdf");

        CandidateInsightService service = new CandidateInsightService(
                applicationRepository,
                userRepository,
                profileRepository,
                positionRepository,
                new InternalReferralCsvRepository(tempDir),
                new TAFeedbackCsvRepository(tempDir),
                resumeRepository
        );

        service.tagInternalReferral("U0001", "Prof. Zhao", "Worked together in lab", "U0002");
        service.tagInternalReferral("U0001", "Prof. Tan", "Excellent demo helper", "U0002");
        CandidateExportResult result = service.exportCandidates("P0001", tempDir.resolve("exports"));
        CandidateExportResult resumeResult = service.exportResumes("P0001", tempDir.resolve("exports"));

        assertTrue(Files.exists(result.filePath()));
        assertTrue(result.filePath().getFileName().toString().startsWith("Construction_TA_Candidates_"));
        assertTrue(Files.readString(result.filePath()).contains("Name,Student ID,Major,Skills,Application Time"));
        assertTrue(Files.isDirectory(resumeResult.filePath()));
        assertEquals(1, resumeResult.rowCount());
        assertEquals(1, service.listCandidatesForPosition("P0001", true).size());
        assertEquals("Prof. Zhao; Prof. Tan", service.listCandidatesForPosition("P0001", true).get(0).referral().orElseThrow().displayRecommenders());
    }
}
