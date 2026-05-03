package com.sfgroup81.tams.model;

import java.util.Arrays;
import java.util.List;

public record EnrollmentProfileSnapshot(
        String snapshotId,
        String userId,
        String semesterId,
        String phone,
        String major,
        String yearOfStudy,
        String gpa,
        String skills,
        String availability,
        String notes,
        String positionIds,
        String resumeFilePath,
        String resumeAutoFilename,
        String savedAt
) {
    public List<String> positionIdList() {
        return Arrays.stream((positionIds == null ? "" : positionIds).split(";"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
