package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.InternalReferral;
import com.sfgroup81.tams.model.ResumeFileRecord;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAFeedback;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.InternalReferralCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CandidateInsightService {
    private final TAApplicationCsvRepository applicationRepository;
    private final UserCsvRepository userRepository;
    private final ApplicantProfileCsvRepository profileRepository;
    private final PositionCsvRepository positionRepository;
    private final InternalReferralCsvRepository referralRepository;
    private final TAFeedbackCsvRepository feedbackRepository;
    private final ResumeFileCsvRepository resumeRepository;
    private final AuditLogService auditLogService;

    public CandidateInsightService(TAApplicationCsvRepository applicationRepository,
                                   UserCsvRepository userRepository,
                                   ApplicantProfileCsvRepository profileRepository,
                                   PositionCsvRepository positionRepository,
                                   InternalReferralCsvRepository referralRepository,
                                   TAFeedbackCsvRepository feedbackRepository) {
        this(applicationRepository,
                userRepository,
                profileRepository,
                positionRepository,
                referralRepository,
                feedbackRepository,
                new ResumeFileCsvRepository(),
                AuditLogService.noop());
    }

    public CandidateInsightService(TAApplicationCsvRepository applicationRepository,
                                   UserCsvRepository userRepository,
                                   ApplicantProfileCsvRepository profileRepository,
                                   PositionCsvRepository positionRepository,
                                   InternalReferralCsvRepository referralRepository,
                                   TAFeedbackCsvRepository feedbackRepository,
                                   ResumeFileCsvRepository resumeRepository) {
        this(applicationRepository, userRepository, profileRepository, positionRepository, referralRepository, feedbackRepository, resumeRepository, AuditLogService.noop());
    }

    public CandidateInsightService(TAApplicationCsvRepository applicationRepository,
                                   UserCsvRepository userRepository,
                                   ApplicantProfileCsvRepository profileRepository,
                                   PositionCsvRepository positionRepository,
                                   InternalReferralCsvRepository referralRepository,
                                   TAFeedbackCsvRepository feedbackRepository,
                                   ResumeFileCsvRepository resumeRepository,
                                   AuditLogService auditLogService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.positionRepository = positionRepository;
        this.referralRepository = referralRepository;
        this.feedbackRepository = feedbackRepository;
        this.resumeRepository = resumeRepository;
        this.auditLogService = auditLogService;
    }

    public InternalReferral tagInternalReferral(String userId, String recommenderName, String note, String taggedBy) {
        InternalReferral existing = referralRepository.findByUserId(userId).orElse(null);
        String normalizedName = safe(recommenderName);
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Recommender name is required");
        }
        String mergedRecommenders = mergeRecommenders(existing, normalizedName);
        String mergedNote = mergeNotes(existing == null ? "" : existing.note(), note);
        InternalReferral referral = new InternalReferral(
                existing == null ? referralRepository.nextReferralId() : existing.referralId(),
                userId,
                mergedRecommenders,
                mergedNote,
                safe(taggedBy),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        InternalReferral saved = referralRepository.saveOrUpdate(referral);
        auditLogService.record("REFERRAL_TAGGED", taggedBy,
                "Tagged internal referral for " + userId + " by " + normalizedName);
        return saved;
    }

    public List<CandidateReviewView> listCandidatesForPosition(String positionId, boolean internallyRecommendedOnly) {
        List<CandidateReviewView> candidates = new ArrayList<>();
        for (TAApplication application : applicationRepository.findByPositionId(positionId)) {
            User user = userRepository.findAll().stream()
                    .filter(item -> item.userId().equals(application.userId()))
                    .findFirst()
                    .orElse(null);
            String semesterId = safe(application.semesterId()).isBlank()
                    ? positionRepository.findById(application.positionId()).map(TAPosition::semesterId).orElse("")
                    : application.semesterId();
            ApplicantProfile profile = profileRepository.findByUserIdAndSemesterId(application.userId(), semesterId)
                    .orElseGet(() -> profileRepository.findByUserId(application.userId()).orElse(null));
            Optional<InternalReferral> referral = referralRepository.findByUserId(application.userId());
            if (internallyRecommendedOnly && referral.filter(InternalReferral::hasRecommenders).isEmpty()) {
                continue;
            }
            candidates.add(new CandidateReviewView(
                    application,
                    user,
                    profile,
                    referral,
                    reputationScore(application.userId())
            ));
        }
        return candidates;
    }

    public CandidateExportResult exportCandidates(String positionId, Path outputDir) {
        return exportCandidates(positionId, outputDir, "");
    }

    public CandidateExportResult exportCandidates(String positionId, Path outputDir, String operatorUserId) {
        TAPosition position = positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));
        List<CandidateReviewView> candidates = listCandidatesForPosition(positionId, false);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No candidates available");
        }
        try {
            Files.createDirectories(outputDir);
            String filename = sanitizeFileFragment(position.title().isBlank() ? position.courseName() : position.title())
                    + "_Candidates_"
                    + LocalDate.now()
                    + ".csv";
            Path exportFile = outputDir.resolve(filename);
            List<String> lines = new ArrayList<>();
            lines.add("Name,Student ID,Major,Skills,Application Time");
            for (CandidateReviewView candidate : candidates) {
                lines.add(String.join(",",
                        sanitize(candidate.user() == null ? candidate.application().userId() : candidate.user().name()),
                        sanitize(candidate.user() == null ? "" : candidate.user().staffOrStudentId()),
                        sanitize(candidate.profile() == null ? "" : candidate.profile().major()),
                        sanitize(candidate.profile() == null ? "" : candidate.profile().skills()),
                        sanitize(candidate.application().submittedAt())
                ));
            }
            Files.write(exportFile, lines, StandardCharsets.UTF_8);
            auditLogService.record("CANDIDATE_EXPORT", operatorUserId, "Exported candidate CSV for " + positionId + " to " + exportFile);
            return new CandidateExportResult(exportFile, candidates.size());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export candidates", ex);
        }
    }

    public Optional<ResumeFileRecord> findResumeForApplication(String applicationId) {
        return resumeRepository.findByApplicationId(applicationId);
    }

    public CandidateExportResult exportResumes(String positionId, Path outputDir) {
        return exportResumes(positionId, outputDir, "");
    }

    public CandidateExportResult exportResumes(String positionId, Path outputDir, String operatorUserId) {
        TAPosition position = positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));
        List<CandidateReviewView> candidates = listCandidatesForPosition(positionId, false);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No candidates available");
        }
        try {
            String folderName = sanitizeFileFragment(position.title().isBlank() ? position.courseName() : position.title())
                    + "_Resumes_"
                    + LocalDate.now();
            Path exportDir = outputDir.resolve(folderName);
            Files.createDirectories(exportDir);
            int copied = 0;
            for (CandidateReviewView candidate : candidates) {
                Optional<ResumeFileRecord> resume = resumeRepository.findByApplicationId(candidate.application().applicationId());
                if (resume.isEmpty()) {
                    continue;
                }
                Path source = Path.of(resume.get().filePath());
                if (Files.notExists(source)) {
                    continue;
                }
                String applicantPrefix = candidate.user() == null
                        ? candidate.application().userId()
                        : candidate.user().staffOrStudentId() + "_" + candidate.user().name();
                Path target = exportDir.resolve(sanitizeFileFragment(applicantPrefix) + "_" + resume.get().autoFilename());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            }
            if (copied == 0) {
                throw new IllegalArgumentException("No available resume files for this position");
            }
            auditLogService.record("RESUME_EXPORT", operatorUserId, "Exported resumes for " + positionId + " to " + exportDir);
            return new CandidateExportResult(exportDir, copied);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export resume files", ex);
        }
    }

    public double reputationScore(String userId) {
        List<TAFeedback> feedback = feedbackRepository.findByTaUserId(userId);
        if (feedback.isEmpty()) {
            return 0.0;
        }
        double average = feedback.stream().mapToDouble(TAFeedback::averageScore).average().orElse(0.0);
        return Math.round(average * 100.0) / 100.0;
    }

    private String mergeRecommenders(InternalReferral existing, String newName) {
        Set<String> names = new LinkedHashSet<>();
        if (existing != null) {
            names.addAll(existing.recommenderNames());
        }
        names.add(newName);
        return String.join("; ", names);
    }

    private String mergeNotes(String existingNote, String newNote) {
        String existing = safe(existingNote);
        String incoming = safe(newNote);
        if (incoming.isBlank() || existing.contains(incoming)) {
            return existing;
        }
        if (existing.isBlank()) {
            return incoming;
        }
        return existing + " | " + incoming;
    }

    private String sanitizeFileFragment(String value) {
        String normalized = safe(value).replaceAll("[^A-Za-z0-9]+", "_");
        return normalized.isBlank() ? "Position" : normalized.replaceAll("^_+|_+$", "");
    }

    private String sanitize(String value) {
        return safe(value).replace(",", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
