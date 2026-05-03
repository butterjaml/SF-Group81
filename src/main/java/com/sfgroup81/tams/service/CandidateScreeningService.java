package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.TAPosition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class CandidateScreeningService {
    private final CandidateInsightService candidateInsightService;
    private final AuditLogService auditLogService;

    public CandidateScreeningService(CandidateInsightService candidateInsightService) {
        this(candidateInsightService, AuditLogService.noop());
    }

    public CandidateScreeningService(CandidateInsightService candidateInsightService, AuditLogService auditLogService) {
        this.candidateInsightService = candidateInsightService;
        this.auditLogService = auditLogService;
    }

    public List<CandidateScreeningView> screenCandidates(TAPosition position,
                                                         CandidateFilterCriteria filter,
                                                         CandidateRankingWeights weights,
                                                         CandidateSortOption sortOption) {
        CandidateFilterCriteria criteria = filter == null
                ? new CandidateFilterCriteria("", "", null, null, "", "", "", false, false)
                : filter;
        CandidateRankingWeights rankingWeights = weights == null ? CandidateRankingWeights.defaults() : weights;
        CandidateSortOption resolvedSort = sortOption == null ? CandidateSortOption.RECOMMENDATION_SCORE : sortOption;

        List<CandidateScreeningView> rows = new ArrayList<>();
        for (CandidateReviewView candidate : candidateInsightService.listCandidatesForPosition(position.positionId(), criteria.internallyRecommendedOnly())) {
            CandidateScreeningView view = toScreeningView(position, candidate, rankingWeights);
            if (matches(view, criteria)) {
                rows.add(view);
            }
        }
        rows.sort(comparator(resolvedSort));
        return rows;
    }

    public Path exportRankedCandidates(TAPosition position,
                                       List<CandidateScreeningView> candidates,
                                       Path outputDir,
                                       String operatorUserId) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("No candidates available");
        }
        try {
            Files.createDirectories(outputDir);
            String filename = sanitizeFileFragment(position.title().isBlank() ? position.courseName() : position.title())
                    + "_Ranked_Candidates_"
                    + LocalDate.now()
                    + ".csv";
            Path exportFile = outputDir.resolve(filename);
            List<String> lines = new ArrayList<>();
            lines.add("Name,Student ID,Recommendation Score,GPA,Past TA Experience,Skills,Availability,Applied At");
            for (CandidateScreeningView row : candidates) {
                ApplicantProfile profile = row.candidate().profile();
                lines.add(String.join(",",
                        sanitize(row.candidate().user() == null ? row.candidate().application().userId() : row.candidate().user().name()),
                        sanitize(row.candidate().user() == null ? "" : row.candidate().user().staffOrStudentId()),
                        sanitize(String.format(Locale.ROOT, "%.2f", row.recommendationScore())),
                        sanitize(profile == null ? "" : profile.gpa()),
                        row.hasPastTaExperience() ? "YES" : "NO",
                        sanitize(profile == null ? "" : profile.skills()),
                        sanitize(profile == null ? "" : profile.availability()),
                        sanitize(row.candidate().application().submittedAt())
                ));
            }
            Files.write(exportFile, lines, StandardCharsets.UTF_8);
            auditLogService.record("RANKED_CANDIDATE_EXPORT", operatorUserId,
                    "Exported ranked candidates for " + position.positionId() + " to " + exportFile);
            return exportFile;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export ranked candidates", ex);
        }
    }

    private CandidateScreeningView toScreeningView(TAPosition position, CandidateReviewView candidate, CandidateRankingWeights weights) {
        ApplicantProfile profile = candidate.profile();
        double gpaValue = parseGpa(profile == null ? "" : profile.gpa());
        boolean hasExperience = hasPastTaExperience(profile, candidate.reputationScore());
        List<String> matchedKeywords = matchedRequirementKeywords(position, profile == null ? "" : profile.skills());
        double skillScore = requirementKeywords(position).isEmpty()
                ? 0.5
                : (double) matchedKeywords.size() / requirementKeywords(position).size();
        double referralScore = candidate.referral().filter(referral -> referral.hasRecommenders()).isPresent() ? 1.0 : 0.0;
        double reputationScore = Math.min(candidate.reputationScore() / 5.0, 1.0);

        int totalWeight = Math.max(weights.totalWeight(), 1);
        double weightedScore = (
                normalizeGpa(gpaValue) * weights.gpaWeight()
                        + (hasExperience ? 1.0 : 0.0) * weights.experienceWeight()
                        + skillScore * weights.skillWeight()
                        + referralScore * weights.referralWeight()
                        + reputationScore * weights.reputationWeight()
        ) / totalWeight;

        return new CandidateScreeningView(
                candidate,
                Math.round(weightedScore * 10000.0) / 100.0,
                gpaValue,
                hasExperience,
                Math.round(skillScore * 100.0) / 100.0,
                matchedKeywords
        );
    }

    private boolean matches(CandidateScreeningView view, CandidateFilterCriteria criteria) {
        String name = view.candidate().user() == null ? "" : view.candidate().user().name();
        String studentId = view.candidate().user() == null ? "" : view.candidate().user().staffOrStudentId();
        ApplicantProfile profile = view.candidate().profile();
        String skills = profile == null ? "" : profile.skills();
        String year = profile == null ? "" : profile.yearOfStudy();
        String availability = profile == null ? "" : profile.availability();

        if (!containsIgnoreCase(name, criteria.nameKeyword())) {
            return false;
        }
        if (!containsIgnoreCase(studentId, criteria.studentIdKeyword())) {
            return false;
        }
        if (!containsIgnoreCase(skills, criteria.skillKeyword())) {
            return false;
        }
        if (!containsIgnoreCase(year, criteria.yearOfStudy())) {
            return false;
        }
        if (!containsIgnoreCase(availability, criteria.availabilityKeyword())) {
            return false;
        }
        if (criteria.minGpa() != null && view.gpaValue() < criteria.minGpa()) {
            return false;
        }
        if (criteria.maxGpa() != null && view.gpaValue() > criteria.maxGpa()) {
            return false;
        }
        return !criteria.experiencedOnly() || view.hasPastTaExperience();
    }

    private Comparator<CandidateScreeningView> comparator(CandidateSortOption option) {
        return switch (option) {
            case GPA -> Comparator.comparingDouble(CandidateScreeningView::gpaValue).reversed()
                    .thenComparing(item -> item.candidate().application().submittedAt());
            case APPLICATION_DATE -> Comparator.comparing((CandidateScreeningView item) -> item.candidate().application().submittedAt()).reversed();
            case NAME -> Comparator.comparing(item -> item.candidate().user() == null ? item.candidate().application().userId() : item.candidate().user().name());
            case RECOMMENDATION_SCORE -> Comparator.comparingDouble(CandidateScreeningView::recommendationScore).reversed()
                    .thenComparing(Comparator.comparingDouble(CandidateScreeningView::gpaValue).reversed());
        };
    }

    private boolean hasPastTaExperience(ApplicantProfile profile, double reputationScore) {
        if (reputationScore > 0.0) {
            return true;
        }
        String text = ((profile == null ? "" : profile.skills()) + " " + (profile == null ? "" : profile.notes()))
                .toLowerCase(Locale.ROOT);
        return text.contains("ta")
                || text.contains("teaching assistant")
                || text.contains("tutor")
                || text.contains("mentoring")
                || text.contains("marking")
                || text.contains("returning applicant");
    }

    private Set<String> requirementKeywords(TAPosition position) {
        Set<String> keywords = new TreeSet<>();
        addKeywords(keywords, position.mandatoryRequirements());
        addKeywords(keywords, position.preferredRequirements());
        addKeywords(keywords, position.bonusRequirements());
        return keywords;
    }

    private List<String> matchedRequirementKeywords(TAPosition position, String skills) {
        String haystack = safe(skills).toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (String keyword : requirementKeywords(position)) {
            if (!keyword.isBlank() && haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                matched.add(keyword);
            }
        }
        return matched;
    }

    private void addKeywords(Set<String> keywords, String text) {
        for (String part : safe(text).split("[;|,/]")) {
            String token = part.trim();
            if (token.length() >= 3) {
                keywords.add(token);
            }
        }
    }

    private double parseGpa(String gpa) {
        try {
            return Double.parseDouble(safe(gpa));
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private double normalizeGpa(double gpaValue) {
        return Math.max(0.0, Math.min(gpaValue / 4.0, 1.0));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        String normalized = safe(keyword).toLowerCase(Locale.ROOT);
        return normalized.isBlank() || safe(source).toLowerCase(Locale.ROOT).contains(normalized);
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
