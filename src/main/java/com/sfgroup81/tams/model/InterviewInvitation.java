package com.sfgroup81.tams.model;

public record InterviewInvitation(
        String invitationId,
        String applicationId,
        String scheduledAt,
        String location,
        String notes,
        InterviewResponseStatus responseStatus,
        String responseNote,
        String createdBy,
        String updatedAt
) {
}
