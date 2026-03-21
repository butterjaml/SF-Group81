package com.sfgroup81.tams.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

public class ResumeUploadService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc");
    private static final Path RESUME_DIR = Path.of("data", "resumes");

    public String uploadResume(String userId, Path sourceFile) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("A logged-in TA user is required");
        }
        if (sourceFile == null || Files.notExists(sourceFile)) {
            throw new IllegalArgumentException("Please select a valid local file");
        }

        String fileName = sourceFile.getFileName().toString();
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only PDF and DOC files are supported");
        }

        try {
            Files.createDirectories(RESUME_DIR);
            Path target = RESUME_DIR.resolve(fileName);
            Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save resume file", ex);
        }
    }

    private String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
