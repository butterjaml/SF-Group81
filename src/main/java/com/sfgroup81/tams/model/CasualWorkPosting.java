package com.sfgroup81.tams.model;

public record CasualWorkPosting(
        String postingId,
        String title,
        String description,
        String workDate,
        String location,
        String requiredSkills,
        int headcount,
        String compensation,
        String status,
        String createdBy,
        String createdAt,
        String updatedAt
) {
}
