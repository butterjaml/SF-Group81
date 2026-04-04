package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ResumeFileRecord;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ResumeUploadService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc");
    private final Path resumeDir;
    private final ResumeFileCsvRepository resumeFileRepository;
    private final UserCsvRepository userRepository;

    public ResumeUploadService() {
        this(Path.of("data"), new ResumeFileCsvRepository(), new UserCsvRepository());
    }

    public ResumeUploadService(ResumeFileCsvRepository resumeFileRepository) {
        this(Path.of("data"), resumeFileRepository, new UserCsvRepository());
    }

    public ResumeUploadService(Path dataDir,
                               ResumeFileCsvRepository resumeFileRepository,
                               UserCsvRepository userRepository) {
        this.resumeDir = dataDir.resolve("resumes");
        this.resumeFileRepository = resumeFileRepository;
        this.userRepository = userRepository;
    }

    public String uploadResume(String userId, Path sourceFile) {
        return uploadResumeForApplications(userId, List.of(toApplicationId(userId)), sourceFile);
    }

    public String uploadResumeForApplications(String userId, List<String> applicationIds, Path sourceFile) {
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
            Files.createDirectories(resumeDir);
            String autoFilename = autoFilename(userId, extension);
            Path target = resumeDir.resolve(autoFilename);
            Path normalizedSource = sourceFile.toAbsolutePath().normalize();
            Path normalizedTarget = target.toAbsolutePath().normalize();
            if (!normalizedSource.equals(normalizedTarget)) {
                Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
            for (String applicationId : applicationIds) {
                ResumeFileRecord saved = resumeFileRepository.saveOrUpdate(
                        applicationId,
                        target.toString(),
                        extension.toUpperCase(Locale.ROOT),
                        autoFilename
                );
                autoFilename = saved.autoFilename();
            }
            return autoFilename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save resume file", ex);
        }
    }

    private String autoFilename(String userId, String extension) {
        User user = userRepository.findAll().stream()
                .filter(item -> item.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return "resume_" + sanitizeToken(user.staffOrStudentId()) + "_" + sanitizeToken(user.name()) + "." + extension;
    }

    private String toApplicationId(String userId) {
        return "APP-" + userId.trim();
    }

    private String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeToken(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", "_");
        return normalized.replaceAll("[^A-Za-z0-9_]", "");
    }
}
