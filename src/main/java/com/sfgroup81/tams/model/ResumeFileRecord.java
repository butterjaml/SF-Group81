package com.sfgroup81.tams.model;

public record ResumeFileRecord(
        String resumeId,
        String applicationId,
        String filePath,
        String fileType,
        String autoFilename,
        String uploadedAt,
        String updatedAt
) {
}
