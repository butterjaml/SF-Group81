package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.ResumeFileRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ResumeFileCsvRepository {
    private final Path resumeCsv;
    private static final String HEADER = "resume_id,application_id,file_path,file_type,auto_filename,uploaded_at,updated_at";

    public ResumeFileCsvRepository() {
        this(Path.of("data"));
    }

    public ResumeFileCsvRepository(Path dataDir) {
        this.resumeCsv = dataDir.resolve("resume_files.csv");
    }

    public List<ResumeFileRecord> findAll() {
        try {
            if (Files.notExists(resumeCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(resumeCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<ResumeFileRecord> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < 7) {
                    continue;
                }
                rows.add(new ResumeFileRecord(
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        cols[4],
                        cols[5],
                        cols[6]
                ));
            }
            return rows;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read resume_files.csv", ex);
        }
    }

    public Optional<ResumeFileRecord> findByApplicationId(String applicationId) {
        return findAll().stream()
                .filter(item -> item.applicationId().equals(applicationId))
                .findFirst();
    }

    public ResumeFileRecord saveOrUpdate(String applicationId,
                                         String filePath,
                                         String fileType,
                                         String autoFilename) {
        List<ResumeFileRecord> all = new ArrayList<>(findAll());
        Optional<ResumeFileRecord> existing = all.stream()
                .filter(item -> item.applicationId().equals(applicationId))
                .findFirst();

        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        ResumeFileRecord updated;
        if (existing.isPresent()) {
            ResumeFileRecord old = existing.get();
            updated = new ResumeFileRecord(
                    old.resumeId(),
                    old.applicationId(),
                    sanitize(filePath),
                    sanitize(fileType),
                    sanitize(autoFilename),
                    old.uploadedAt(),
                    now
            );
            all.removeIf(item -> item.applicationId().equals(applicationId));
            all.add(updated);
        } else {
            updated = new ResumeFileRecord(
                    nextResumeId(all),
                    applicationId,
                    sanitize(filePath),
                    sanitize(fileType),
                    sanitize(autoFilename),
                    now,
                    now
            );
            all.add(updated);
        }
        rewriteAll(all);
        return updated;
    }

    private String nextResumeId(List<ResumeFileRecord> all) {
        return all.stream()
                .map(ResumeFileRecord::resumeId)
                .filter(id -> id.startsWith("R"))
                .map(id -> id.substring(1))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("R%04d", max + 1))
                .orElse("R0001");
    }

    private void rewriteAll(List<ResumeFileRecord> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (ResumeFileRecord row : rows) {
            lines.add(String.join(",",
                    sanitize(row.resumeId()),
                    sanitize(row.applicationId()),
                    sanitize(row.filePath()),
                    sanitize(row.fileType()),
                    sanitize(row.autoFilename()),
                    sanitize(row.uploadedAt()),
                    sanitize(row.updatedAt())
            ));
        }

        try {
            Files.write(resumeCsv, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write resume_files.csv", ex);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
