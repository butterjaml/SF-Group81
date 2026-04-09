package com.sfgroup81.tams.service;

import java.nio.file.Path;

public record CandidateExportResult(
        Path filePath,
        int rowCount
) {
}
