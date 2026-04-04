package com.sfgroup81.tams.service;

public record PositionUpsertRequest(
        String positionId,
        String courseId,
        String courseName,
        String instructorName,
        String semesterId,
        String positionType,
        int headcount,
        String deadline,
        String status,
        String title,
        String responsibilities,
        String workingHours,
        String salaryInfo,
        String mandatoryRequirements,
        String preferredRequirements,
        String bonusRequirements,
        String createdBy
) {
}
