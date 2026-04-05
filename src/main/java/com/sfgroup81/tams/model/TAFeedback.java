package com.sfgroup81.tams.model;

public record TAFeedback(
        String feedbackId,
        String taUserId,
        String moUserId,
        String positionId,
        int communicationRating,
        int teachingRating,
        int reliabilityRating,
        String comment,
        String submittedAt
) {
    public double averageScore() {
        return (communicationRating + teachingRating + reliabilityRating) / 3.0;
    }
}
