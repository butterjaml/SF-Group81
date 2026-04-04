package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TAApplication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TAApplicationCsvRepository {
    private static final String HEADER = "application_id,user_id,position_id,priority_no,status,feedback,submitted_at,updated_at";
    private final Path applicationCsv;

    public TAApplicationCsvRepository() {
        this(Path.of("data"));
    }

    public TAApplicationCsvRepository(Path dataDir) {
        this.applicationCsv = dataDir.resolve("ta_applications.csv");
    }

    public List<TAApplication> findAll() {
        try {
            if (Files.notExists(applicationCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(applicationCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<TAApplication> applications = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 8) {
                    continue;
                }
                applications.add(new TAApplication(
                        cols[0],
                        cols[1],
                        cols[2],
                        parseInt(cols[3]),
                        ApplicationStatus.valueOf(cols[4]),
                        cols[5],
                        cols[6],
                        cols[7]
                ));
            }
            return applications;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read ta_applications.csv", ex);
        }
    }

    public Optional<TAApplication> findById(String applicationId) {
        return findAll().stream().filter(item -> item.applicationId().equals(applicationId)).findFirst();
    }

    public List<TAApplication> findByUserId(String userId) {
        return findAll().stream()
                .filter(item -> item.userId().equals(userId))
                .sorted(Comparator.comparingInt(TAApplication::priorityNo))
                .toList();
    }

    public List<TAApplication> findByPositionId(String positionId) {
        return findAll().stream().filter(item -> item.positionId().equals(positionId)).toList();
    }

    public TAApplication saveOrUpdate(TAApplication application) {
        List<TAApplication> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.applicationId().equals(application.applicationId()));
        all.add(application);
        rewriteAll(all);
        return application;
    }

    public void deleteByUserId(String userId) {
        List<TAApplication> remaining = new ArrayList<>(findAll());
        remaining.removeIf(item -> item.userId().equals(userId));
        rewriteAll(remaining);
    }

    private void rewriteAll(List<TAApplication> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (TAApplication item : rows) {
            lines.add(String.join(",",
                    sanitize(item.applicationId()),
                    sanitize(item.userId()),
                    sanitize(item.positionId()),
                    Integer.toString(item.priorityNo()),
                    item.status().name(),
                    sanitize(item.feedback()),
                    sanitize(item.submittedAt()),
                    sanitize(item.updatedAt())
            ));
        }
        try {
            Files.write(applicationCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write ta_applications.csv", ex);
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
