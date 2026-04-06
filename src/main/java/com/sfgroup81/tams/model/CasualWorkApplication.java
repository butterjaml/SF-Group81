package com.sfgroup81.tams.model;

public record CasualWorkApplication(
        String applicationId,
        String postingId,
        String userId,
        String statement,
        String appliedAt
) {
}
