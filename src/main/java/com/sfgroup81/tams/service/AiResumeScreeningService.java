package com.sfgroup81.tams.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfgroup81.tams.model.AiScreeningResult;
import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ResumeFileRecord;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.AiScreeningResultCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AiResumeScreeningService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiScreeningResultCsvRepository resultRepository;
    private final ResumeFileCsvRepository resumeRepository;
    private final ResumeTextExtractionService textExtractionService;
    private final AiChatClient chatClient;
    private final AuditLogService auditLogService;

    public AiResumeScreeningService(AiScreeningResultCsvRepository resultRepository,
                                    ResumeFileCsvRepository resumeRepository,
                                    AuditLogService auditLogService) {
        this(resultRepository, resumeRepository, new ResumeTextExtractionService(), new OpenAiCompatibleChatClient(), auditLogService);
    }

    public AiResumeScreeningService(AiScreeningResultCsvRepository resultRepository,
                                    ResumeFileCsvRepository resumeRepository,
                                    ResumeTextExtractionService textExtractionService,
                                    AiChatClient chatClient,
                                    AuditLogService auditLogService) {
        this.resultRepository = resultRepository;
        this.resumeRepository = resumeRepository;
        this.textExtractionService = textExtractionService;
        this.chatClient = chatClient;
        this.auditLogService = auditLogService;
    }

    public AiScreeningResult analyze(TAPosition position, CandidateReviewView candidate) {
        String promptHash = SecurityUtil.sha256(buildPromptHashSource(position, candidate));
        Optional<AiScreeningResult> cached = resultRepository.findByPositionAndApplication(position.positionId(), candidate.application().applicationId())
                .filter(item -> item.promptHash().equals(promptHash));
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<ResumeFileRecord> resumeRecord = resumeRepository.findByApplicationId(candidate.application().applicationId());
        String resumeText = resumeRecord
                .map(ResumeFileRecord::filePath)
                .map(Path::of)
                .map(textExtractionService::extract)
                .map(this::truncate)
                .orElse("");
        String responseJson = chatClient.completeJson(systemPrompt(), userPrompt(position, candidate, resumeRecord, resumeText));
        AiScreeningResult result = parseResponse(position, candidate, promptHash, responseJson);
        resultRepository.saveOrUpdate(result);
        auditLogService.record("AI_SCREENING_RUN", candidate.application().userId(),
                "Generated AI candidate screening for " + candidate.application().applicationId());
        return result;
    }

    private AiScreeningResult parseResponse(TAPosition position,
                                            CandidateReviewView candidate,
                                            String promptHash,
                                            String responseJson) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseJson);
            double matchScore = clamp(root.path("match_score").asDouble(0.0));
            String matchedSkills = joinArray(root.path("matched_skills"));
            String missingSkills = joinArray(root.path("missing_skills"));
            return new AiScreeningResult(
                    resultRepository.nextResultId(),
                    position.positionId(),
                    candidate.application().applicationId(),
                    safe(candidate.application().semesterId()),
                    AiModelConfig.MODEL,
                    matchScore,
                    matchedSkills,
                    missingSkills,
                    safe(root.path("summary").asText("")),
                    safe(root.path("strengths").asText("")),
                    safe(root.path("risks").asText("")),
                    promptHash,
                    now()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("AI screening response could not be parsed", ex);
        }
    }

    private String systemPrompt() {
        return """
                You are an assistant for a university TA management system.
                Evaluate one candidate against one TA job posting.
                Return strict JSON only with keys:
                match_score (0-100 number),
                matched_skills (array of short strings),
                missing_skills (array of short strings),
                summary (one concise sentence),
                strengths (one concise sentence),
                risks (one concise sentence).
                Use the job's weighted AI skills as the main reference when available.
                """;
    }

    private String userPrompt(TAPosition position,
                              CandidateReviewView candidate,
                              Optional<ResumeFileRecord> resumeRecord,
                              String resumeText) {
        ApplicantProfile profile = candidate.profile();
        String candidateName = candidate.user() == null ? candidate.application().userId() : candidate.user().name();
        return """
                Job information:
                Course: %s (%s)
                Title: %s
                Semester: %s
                Responsibilities: %s
                Mandatory requirements: %s
                Preferred requirements: %s
                Bonus requirements: %s
                Weighted AI skills: %s

                Candidate information:
                Name: %s
                GPA: %s
                Major: %s
                Year of study: %s
                Skills profile: %s
                Availability: %s
                Notes: %s
                Reputation score: %.2f
                Resume filename: %s

                Resume text:
                %s
                """.formatted(
                safe(position.courseName()),
                safe(position.courseId()),
                safe(position.title()),
                safe(position.semesterId()),
                safe(position.responsibilities()),
                safe(position.mandatoryRequirements()),
                safe(position.preferredRequirements()),
                safe(position.bonusRequirements()),
                weightedSkillReference(position),
                safe(candidateName),
                profile == null ? "" : safe(profile.gpa()),
                profile == null ? "" : safe(profile.major()),
                profile == null ? "" : safe(profile.yearOfStudy()),
                profile == null ? "" : safe(profile.skills()),
                profile == null ? "" : safe(profile.availability()),
                profile == null ? "" : safe(profile.notes()),
                candidate.reputationScore(),
                resumeRecord.map(ResumeFileRecord::autoFilename).orElse(""),
                resumeText.isBlank() ? "(resume text unavailable; use the structured profile and requirements)" : resumeText
        );
    }

    private String buildPromptHashSource(TAPosition position, CandidateReviewView candidate) {
        ApplicantProfile profile = candidate.profile();
        String resumeUpdatedAt = resumeRepository.findByApplicationId(candidate.application().applicationId())
                .map(ResumeFileRecord::updatedAt)
                .orElse("");
        return String.join("|",
                AiModelConfig.MODEL,
                safe(position.positionId()),
                safe(position.updatedAt()),
                safe(position.aiScreeningCriteria()),
                safe(position.mandatoryRequirements()),
                safe(position.preferredRequirements()),
                safe(position.bonusRequirements()),
                safe(candidate.application().applicationId()),
                safe(candidate.application().updatedAt()),
                profile == null ? "" : safe(profile.updatedAt()),
                profile == null ? "" : safe(profile.skills()),
                resumeUpdatedAt
        );
    }

    private String weightedSkillReference(TAPosition position) {
        String explicit = safe(position.aiScreeningCriteria());
        if (!explicit.isBlank()) {
            return explicit;
        }
        List<String> derived = new java.util.ArrayList<>();
        addDerivedSkills(derived, position.mandatoryRequirements(), 50);
        addDerivedSkills(derived, position.preferredRequirements(), 30);
        addDerivedSkills(derived, position.bonusRequirements(), 20);
        return derived.isEmpty() ? "No weighted skill list provided" : String.join("; ", derived);
    }

    private void addDerivedSkills(List<String> target, String source, int defaultWeight) {
        for (String part : safe(source).split("[;|,/\\n]")) {
            String token = part.trim();
            if (token.length() >= 3) {
                target.add(token + "=" + defaultWeight);
            }
        }
    }

    private String joinArray(JsonNode node) {
        if (!node.isArray()) {
            return "";
        }
        List<String> values = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            String value = safe(item.asText(""));
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return String.join("; ", values);
    }

    private double clamp(double score) {
        return Math.max(0.0, Math.min(score, 100.0));
    }

    private String truncate(String value) {
        String normalized = safe(value).replace('\0', ' ');
        if (normalized.length() <= 12000) {
            return normalized;
        }
        return normalized.substring(0, 12000);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
