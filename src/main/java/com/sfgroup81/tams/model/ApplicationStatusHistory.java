package com.sfgroup81.tams.model;

public record ApplicationStatusHistory(
        String historyId,
        String applicationId,
        ApplicationStatus status,
        String note,
        String changedBy,
        String changedAt
) {
}
