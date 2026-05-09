package com.sfgroup81.tams.model;

public record TAApplication(
        String applicationId,
        String userId,
        String positionId,
        String semesterId,
        int priorityNo,
        ApplicationStatus status,
        String feedback,
        String submittedAt,
        String updatedAt
) {
    public TAApplication(String applicationId,
                         String userId,
                         String positionId,
                         int priorityNo,
                         ApplicationStatus status,
                         String feedback,
                         String submittedAt,
                         String updatedAt) {
        this(applicationId, userId, positionId, "", priorityNo, status, feedback, submittedAt, updatedAt);
    }
}
