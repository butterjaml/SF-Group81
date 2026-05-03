package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.EnrollmentProfileSnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EnrollmentProfileSnapshotCsvRepository {
    private static final String HEADER = "snapshot_id,user_id,semester_id,phone,major,year_of_study,gpa,skills,availability,notes,position_ids,resume_file_path,resume_auto_filename,saved_at";

    private final Path snapshotCsv;

    public EnrollmentProfileSnapshotCsvRepository() {
        this(Path.of("data"));
    }

    public EnrollmentProfileSnapshotCsvRepository(Path dataDir) {
        this.snapshotCsv = dataDir.resolve("enrollment_profile_snapshots.csv");
    }

    public List<EnrollmentProfileSnapshot> findAll() {
        try {
            if (Files.notExists(snapshotCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(snapshotCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }
            List<EnrollmentProfileSnapshot> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < 14) {
                    continue;
                }
                rows.add(new EnrollmentProfileSnapshot(
                        cols[0], cols[1], cols[2], cols[3], cols[4], cols[5], cols[6],
                        cols[7], cols[8], cols[9], cols[10], cols[11], cols[12], cols[13]
                ));
            }
            return rows;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read enrollment_profile_snapshots.csv", ex);
        }
    }

    public EnrollmentProfileSnapshot save(EnrollmentProfileSnapshot snapshot) {
        try {
            Files.writeString(
                    snapshotCsv,
                    String.join(",",
                            sanitize(snapshot.snapshotId()),
                            sanitize(snapshot.userId()),
                            sanitize(snapshot.semesterId()),
                            sanitize(snapshot.phone()),
                            sanitize(snapshot.major()),
                            sanitize(snapshot.yearOfStudy()),
                            sanitize(snapshot.gpa()),
                            sanitize(snapshot.skills()),
                            sanitize(snapshot.availability()),
                            sanitize(snapshot.notes()),
                            sanitize(snapshot.positionIds()),
                            sanitize(snapshot.resumeFilePath()),
                            sanitize(snapshot.resumeAutoFilename()),
                            sanitize(snapshot.savedAt()))
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            return snapshot;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write enrollment_profile_snapshots.csv", ex);
        }
    }

    public String nextSnapshotId() {
        return findAll().stream()
                .map(EnrollmentProfileSnapshot::snapshotId)
                .filter(id -> id.startsWith("SN"))
                .map(id -> id.substring(2))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("SN%05d", max + 1))
                .orElse("SN00001");
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
