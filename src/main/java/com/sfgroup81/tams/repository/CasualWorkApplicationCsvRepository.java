package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.CasualWorkApplication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CasualWorkApplicationCsvRepository {
    private static final String HEADER = "application_id,posting_id,user_id,statement,applied_at";
    private final Path applicationCsv;

    public CasualWorkApplicationCsvRepository() {
        this(Path.of("data"));
    }

    public CasualWorkApplicationCsvRepository(Path dataDir) {
        this.applicationCsv = dataDir.resolve("casual_work_applications.csv");
    }

    public List<CasualWorkApplication> findAll() {
        try {
            if (Files.notExists(applicationCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(applicationCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }
            List<CasualWorkApplication> applications = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 5) {
                    continue;
                }
                applications.add(new CasualWorkApplication(cols[0], cols[1], cols[2], cols[3], cols[4]));
            }
            return applications;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read casual_work_applications.csv", ex);
        }
    }

    public List<CasualWorkApplication> findByPostingId(String postingId) {
        return findAll().stream()
                .filter(item -> item.postingId().equals(postingId))
                .sorted(Comparator.comparing(CasualWorkApplication::appliedAt))
                .toList();
    }

    public boolean exists(String postingId, String userId) {
        return findAll().stream().anyMatch(item -> item.postingId().equals(postingId) && item.userId().equals(userId));
    }

    public CasualWorkApplication save(CasualWorkApplication application) {
        List<CasualWorkApplication> all = new ArrayList<>(findAll());
        all.add(application);
        rewriteAll(all);
        return application;
    }

    public String nextApplicationId() {
        return findAll().stream()
                .map(CasualWorkApplication::applicationId)
                .filter(id -> id.startsWith("CWA"))
                .map(id -> id.substring(3))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("CWA%04d", max + 1))
                .orElse("CWA0001");
    }

    private void rewriteAll(List<CasualWorkApplication> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (CasualWorkApplication item : rows) {
            lines.add(String.join(",",
                    sanitize(item.applicationId()),
                    sanitize(item.postingId()),
                    sanitize(item.userId()),
                    sanitize(item.statement()),
                    sanitize(item.appliedAt())
            ));
        }
        try {
            Files.write(applicationCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write casual_work_applications.csv", ex);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
