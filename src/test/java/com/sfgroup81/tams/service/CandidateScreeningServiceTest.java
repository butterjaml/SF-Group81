package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.AiScreeningResultCsvRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateScreeningServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldFilterRankAndExportCandidates() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository(tempDir);
        InternalReferralCsvRepository referralRepository = new InternalReferralCsvRepository(tempDir);
        TAFeedbackCsvRepository feedbackRepository = new TAFeedbackCsvRepository(tempDir);

        userRepository.saveNewUser("Grace Liu", "90001", "mo@example.com", SecurityUtil.sha256("password123"), UserRole.MO);
        userRepository.saveNewUser("Alice Tan", "20250001", "alice@example.com", SecurityUtil.sha256("password123"), UserRole.TA);
        userRepository.saveNewUser("Chloe Ng", "20250002", "chloe@example.com", SecurityUtil.sha256("password123"), UserRole.TA);

        PositionService positionService = new PositionService(positionRepository);
        var position = positionService.savePosition(new PositionUpsertRequest(
                "",
                "COMP305",
                "Software Construction",
                "Grace Liu",
                "2026S1",
                "Modular TA",
                2,
                LocalDate.now().plusDays(10).toString(),
                "PUBLISHED",
                "Construction TA",
                "Support labs",
                "4 hours/week",
                "90 yuan/hour",
                "Java; tutoring",
                "Git; marking",
                "public speaking",
                "U0001"
        ), "U0001");

        profileRepository.saveOrUpdate(new ApplicantProfile("U0002", "18800001111", "Computer Science", "Year 3", "3.92",
                "Java; tutoring; Git", "Weekday afternoons", "Returning applicant with prior TA experience", "2026-05-01T10:00:00"));
        profileRepository.saveOrUpdate(new ApplicantProfile("U0003", "18800002222", "Information Systems", "Year 2", "3.40",
                "Excel; support", "Weekends", "No teaching background yet", "2026-05-01T10:05:00"));

        applicationRepository.saveOrUpdate(new TAApplication("APP-U0002-" + position.positionId(), "U0002", position.positionId(), 1,
                ApplicationStatus.PENDING_REVIEW, "", "2026-05-01T09:00:00", "2026-05-01T09:00:00"));
        applicationRepository.saveOrUpdate(new TAApplication("APP-U0003-" + position.positionId(), "U0003", position.positionId(), 2,
                ApplicationStatus.PENDING_REVIEW, "", "2026-05-01T09:30:00", "2026-05-01T09:30:00"));

        Path resumeFile = tempDir.resolve("alice_resume.txt");
        Files.writeString(resumeFile, "Experienced Java tutor with Git and lab teaching background.");
        resumeRepository.saveOrUpdate("APP-U0002-" + position.positionId(), resumeFile.toString(), "TXT", "alice_resume.txt");

        CandidateInsightService candidateInsightService = new CandidateInsightService(
                applicationRepository,
                userRepository,
                profileRepository,
                positionRepository,
                referralRepository,
                feedbackRepository,
                resumeRepository
        );
        candidateInsightService.tagInternalReferral("U0002", "Prof. Tan", "Strong recommendation", "U0001");

        AiResumeScreeningService aiResumeScreeningService = new AiResumeScreeningService(
                new AiScreeningResultCsvRepository(tempDir),
                resumeRepository,
                new ResumeTextExtractionService(),
                (systemPrompt, userPrompt) -> userPrompt.contains("Alice Tan")
                        ? "{\"match_score\":91,\"matched_skills\":[\"Java\",\"Git\",\"tutoring\"],\"missing_skills\":[\"public speaking\"],\"summary\":\"Excellent overall fit for the TA role.\",\"strengths\":\"Strong Java tutoring background and clear alignment with the lab support needs.\",\"risks\":\"Public speaking evidence is limited in the available resume.\"}"
                        : "{\"match_score\":54,\"matched_skills\":[\"support\"],\"missing_skills\":[\"Java\",\"Git\"],\"summary\":\"Partial fit but weaker alignment with the technical requirements.\",\"strengths\":\"Shows some student support experience.\",\"risks\":\"Core Java and Git evidence is limited.\"}",
                AuditLogService.noop()
        );

        CandidateScreeningService screeningService = new CandidateScreeningService(
                candidateInsightService,
                aiResumeScreeningService,
                AuditLogService.noop()
        );
        List<CandidateScreeningView> ranked = screeningService.screenCandidates(
                position,
                new CandidateFilterCriteria("", "", 3.5, null, "Java", "", "", false, true),
                CandidateRankingWeights.defaults(),
                CandidateSortOption.RECOMMENDATION_SCORE
        );

        assertEquals(1, ranked.size());
        assertEquals("U0002", ranked.get(0).candidate().application().userId());
        assertEquals(91.0, ranked.get(0).recommendationScore());
        assertTrue(ranked.get(0).matchedRequirementKeywords().contains("Java"));
        assertEquals("Excellent overall fit for the TA role.", ranked.get(0).aiSummary());

        Path exportPath = screeningService.exportRankedCandidates(position, ranked, tempDir.resolve("exports"), "U0001");
        assertTrue(Files.exists(exportPath));
        assertTrue(Files.readString(exportPath).contains("AI Match Score"));
    }
}
