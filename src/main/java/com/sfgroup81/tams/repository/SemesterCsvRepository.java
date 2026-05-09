package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.SemesterRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SemesterCsvRepository {
    private static final String HEADER = "semester_id,is_current,is_viewed,is_archived,created_by,created_at,archived_at,notes";

    private final Path semesterCsv;

    public SemesterCsvRepository() {
        this(Path.of("data"));
    }

    public SemesterCsvRepository(Path dataDir) {
        this.semesterCsv = dataDir.resolve("semesters.csv");
    }

    public List<SemesterRecord> findAll() {
        try {
            if (Files.notExists(semesterCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(semesterCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<SemesterRecord> semesters = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 8) {
                    continue;
                }
                semesters.add(new SemesterRecord(
                        cols[0],
                        parseBoolean(cols[1]),
                        parseBoolean(cols[2]),
                        parseBoolean(cols[3]),
                        cols[4],
                        cols[5],
                        cols[6],
                        cols[7]
                ));
            }
            return semesters;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read semesters.csv", ex);
        }
    }

    public Optional<SemesterRecord> findBySemesterId(String semesterId) {
        return findAll().stream()
                .filter(item -> item.semesterId().equalsIgnoreCase(safe(semesterId)))
                .findFirst();
    }

    public void saveAll(List<SemesterRecord> records) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (SemesterRecord record : records) {
            lines.add(String.join(",",
                    sanitize(record.semesterId()),
                    Boolean.toString(record.currentSemester()),
                    Boolean.toString(record.viewedSemester()),
                    Boolean.toString(record.archived()),
                    sanitize(record.createdBy()),
                    sanitize(record.createdAt()),
                    sanitize(record.archivedAt()),
                    sanitize(record.notes())
            ));
        }
        try {
            Files.write(semesterCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write semesters.csv", ex);
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(safe(value));
    }

    private String sanitize(String value) {
        return safe(value).replace(",", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
