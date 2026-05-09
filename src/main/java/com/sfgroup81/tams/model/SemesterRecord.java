package com.sfgroup81.tams.model;

public record SemesterRecord(
        String semesterId,
        boolean currentSemester,
        boolean viewedSemester,
        boolean archived,
        String createdBy,
        String createdAt,
        String archivedAt,
        String notes
) {
}
