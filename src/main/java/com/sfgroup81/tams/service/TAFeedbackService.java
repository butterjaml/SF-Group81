package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.TAFeedback;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TAFeedbackService {
    private final TAFeedbackCsvRepository feedbackRepository;
    private final TAApplicationCsvRepository applicationRepository;
    private final PositionCsvRepository positionRepository;
    private final UserCsvRepository userRepository;

    public TAFeedbackService(TAFeedbackCsvRepository feedbackRepository,
                             TAApplicationCsvRepository applicationRepository,
                             PositionCsvRepository positionRepository,
                             UserCsvRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.applicationRepository = applicationRepository;
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
    }

    public List<TAFeedbackAssignment> listPendingAssignments(String moUserId) {
        return listPendingAssignments(moUserId, java.time.LocalDate.now());
    }

    public List<TAFeedbackAssignment> listPendingAssignments(String moUserId, java.time.LocalDate today) {
        return applicationRepository.findAll().stream()
                .filter(application -> "HIRED".equals(application.status().name()))
                .filter(application -> managedBy(application.positionId(), moUserId))
                .filter(application -> feedbackDue(application.positionId(), today))
                .filter(application -> !feedbackRepository.exists(moUserId, application.userId(), application.positionId()))
                .map(application -> new TAFeedbackAssignment(
                        application.userId(),
                        findUserName(application.userId()),
                        application.positionId(),
                        findPositionTitle(application.positionId())
                ))
                .toList();
    }

    public TAFeedback submitFeedback(String moUserId,
                                     String taUserId,
                                     String positionId,
                                     int communicationRating,
                                     int teachingRating,
                                     int reliabilityRating,
                                     String comment) {
        validateRating(communicationRating);
        validateRating(teachingRating);
        validateRating(reliabilityRating);
        if (!managedBy(positionId, moUserId)) {
            throw new IllegalArgumentException("MO does not manage this TA position");
        }
        TAFeedback feedback = new TAFeedback(
                feedbackRepository.nextFeedbackId(),
                taUserId,
                moUserId,
                positionId,
                communicationRating,
                teachingRating,
                reliabilityRating,
                comment == null ? "" : comment.trim(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        return feedbackRepository.save(feedback);
    }

    public double getReputationScore(String taUserId) {
        List<TAFeedback> feedbackList = feedbackRepository.findByTaUserId(taUserId);
        if (feedbackList.isEmpty()) {
            return 0.0;
        }
        double average = feedbackList.stream().mapToDouble(TAFeedback::averageScore).average().orElse(0.0);
        return Math.round(average * 100.0) / 100.0;
    }

    private boolean managedBy(String positionId, String moUserId) {
        return positionRepository.findById(positionId)
                .map(TAPosition::createdBy)
                .filter(createdBy -> createdBy.equals(moUserId))
                .isPresent();
    }

    private boolean feedbackDue(String positionId, java.time.LocalDate today) {
        return positionRepository.findById(positionId)
                .map(TAPosition::deadline)
                .filter(deadline -> deadline != null && !deadline.isBlank())
                .map(java.time.LocalDate::parse)
                .filter(deadline -> !deadline.isAfter(today))
                .isPresent();
    }

    private String findUserName(String userId) {
        return userRepository.findAll().stream()
                .filter(item -> item.userId().equals(userId))
                .map(User::name)
                .findFirst()
                .orElse(userId);
    }

    private String findPositionTitle(String positionId) {
        return positionRepository.findById(positionId)
                .map(position -> position.title().isBlank() ? position.courseName() : position.title())
                .orElse(positionId);
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Ratings must be between 1 and 5");
        }
    }
}
