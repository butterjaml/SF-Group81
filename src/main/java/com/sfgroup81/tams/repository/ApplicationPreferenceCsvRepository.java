package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.ApplicationPreference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationPreferenceCsvRepository {
    private static final String HEADER = "preference_id,application_id,course_id,priority_no";
    private final Path preferenceCsv;

    public ApplicationPreferenceCsvRepository() {
        this(Path.of("data"));
    }

    public ApplicationPreferenceCsvRepository(Path dataDir) {
        this.preferenceCsv = dataDir.resolve("application_preferences.csv");
    }

    public List<ApplicationPreference> findAll() {
        try {
            if (Files.notExists(preferenceCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(preferenceCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<ApplicationPreference> preferences = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < 4) {
                    continue;
                }
                preferences.add(new ApplicationPreference(
                        cols[0],
                        cols[1],
                        cols[2],
                        parseInt(cols[3])
                ));
            }
            return preferences;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read application_preferences.csv", ex);
        }
    }

    public List<ApplicationPreference> findByApplicationId(String applicationId) {
        return findAll().stream()
                .filter(item -> item.applicationId().equals(applicationId))
                .sorted(Comparator.comparingInt(ApplicationPreference::priorityNo))
                .collect(Collectors.toList());
    }

    public void saveForApplication(String applicationId, List<String> courseIds) {
        List<ApplicationPreference> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.applicationId().equals(applicationId));

        int nextNumber = nextPreferenceNumber(all);
        for (int i = 0; i < courseIds.size(); i++) {
            String preferenceId = String.format("AP%04d", nextNumber++);
            all.add(new ApplicationPreference(
                    preferenceId,
                    applicationId,
                    sanitize(courseIds.get(i)),
                    i + 1
            ));
        }

        rewriteAll(all);
    }

    private int nextPreferenceNumber(List<ApplicationPreference> all) {
        return all.stream()
                .map(ApplicationPreference::preferenceId)
                .filter(id -> id.startsWith("AP"))
                .map(id -> id.substring(2))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> max + 1)
                .orElse(1);
    }

    private void rewriteAll(List<ApplicationPreference> preferences) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (ApplicationPreference item : preferences) {
            lines.add(String.join(",",
                    sanitize(item.preferenceId()),
                    sanitize(item.applicationId()),
                    sanitize(item.courseId()),
                    Integer.toString(item.priorityNo())
            ));
        }
        try {
            Files.write(preferenceCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write application_preferences.csv", ex);
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
