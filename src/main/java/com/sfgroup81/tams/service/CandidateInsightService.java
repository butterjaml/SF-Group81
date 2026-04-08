package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.InternalReferral;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAFeedback;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.InternalReferralCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CandidateInsightService {
    private final TAApplicationCsvRepository applicationRepository;
    private final UserCsvRepository userRepository;
    private final ApplicantProfileCsvRepository profileRepository;
    @SuppressWarnings("unused")
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

    public double reputationScore(String userId) {
        List<TAFeedback> feedback = feedbackRepository.findByTaUserId(userId);
        if (feedback.isEmpty()) {
            return 0.0;
        }
        double average = feedback.stream().mapToDouble(TAFeedback::averageScore).average().orElse(0.0);
        return Math.round(average * 100.0) / 100.0;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
