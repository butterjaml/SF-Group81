package com.sfgroup81.tams.model;

public record TAPosition(
        String positionId,
        String courseId,
        String semesterId,
        String positionType,
        int headcount,
        String deadline,
        String status,
        String title,
        String description,
        String createdBy,
        String createdAt,
        String updatedAt
) {
}
