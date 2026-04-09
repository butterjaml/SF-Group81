package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.InternalReferral;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAFeedback;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.InternalReferralCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CandidateInsightService {
    private final TAApplicationCsvRepository applicationRepository;
    private final UserCsvRepository userRepository;
    private final ApplicantProfileCsvRepository profileRepository;
    private final PositionCsvRepository positionRepository;
    private final InternalReferralCsvRepository referralRepository;
    private final TAFeedbackCsvRepository feedbackRepository;

    public CandidateInsightService(TAApplicationCsvRepository applicationRepository,
                                   UserCsvRepository userRepository,
                                   ApplicantProfileCsvRepository profileRepository,
                                   PositionCsvRepository positionRepository,
                                   InternalReferralCsvRepository referralRepository,
                                   TAFeedbackCsvRepository feedbackRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.positionRepository = positionRepository;
        this.referralRepository = referralRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public InternalReferral tagInternalReferral(String userId, String recommenderName, String note, String taggedBy) {
        InternalReferral existing = referralRepository.findByUserId(userId).orElse(null);
        InternalReferral referral = new InternalReferral(
                existing == null ? referralRepository.nextReferralId() : existing.referralId(),
                userId,
                safe(recommenderName),
                safe(note),
                safe(taggedBy),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        return referralRepository.saveOrUpdate(referral);
    }

    public List<CandidateReviewView> listCandidatesForPosition(String positionId, boolean internallyRecommendedOnly) {
        List<CandidateReviewView> candidates = new ArrayList<>();
        for (TAApplication application : applicationRepository.findByPositionId(positionId)) {
            User user = userRepository.findAll().stream()
                    .filter(item -> item.userId().equals(application.userId()))
                    .findFirst()
                    .orElse(null);
            ApplicantProfile profile = profileRepository.findByUserId(application.userId()).orElse(null);
            Optional<InternalReferral> referral = referralRepository.findByUserId(application.userId());
            if (internallyRecommendedOnly && referral.isEmpty()) {
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
            return new CandidateExportResult(exportFile, candidates.size());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export candidates", ex);
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
