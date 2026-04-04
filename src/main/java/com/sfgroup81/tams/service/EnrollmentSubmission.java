package com.sfgroup81.tams.service;

import java.nio.file.Path;
import java.util.List;

public record EnrollmentSubmission(
        String userId,
        String phone,
        String major,
        String yearOfStudy,
        String gpa,
        String skills,
        String availability,
        String notes,
        Path resumeSourceFile,
        List<String> positionIds
) {
}
