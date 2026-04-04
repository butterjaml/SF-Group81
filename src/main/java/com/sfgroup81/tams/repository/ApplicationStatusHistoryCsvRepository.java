package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.ApplicationStatusHistory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ApplicationStatusHistoryCsvRepository {
    private static final String HEADER = "history_id,application_id,status,note,changed_by,changed_at";
    private final Path historyCsv;

    public ApplicationStatusHistoryCsvRepository() {
        this(Path.of("data"));
    }

    public ApplicationStatusHistoryCsvRepository(Path dataDir) {
        this.historyCsv = dataDir.resolve("application_status_history.csv");
    }

    public List<ApplicationStatusHistory> findAll() {
        try {
            if (Files.notExists(historyCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(historyCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<ApplicationStatusHistory> history = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 6) {
                    continue;
                }
                history.add(new ApplicationStatusHistory(
                        cols[0],
                        cols[1],
                        ApplicationStatus.valueOf(cols[2]),
                        cols[3],
                        cols[4],
                        cols[5]
                ));
            }
            history.sort(Comparator.comparing(ApplicationStatusHistory::changedAt));
            return history;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read application_status_history.csv", ex);
        }
    }

    public List<ApplicationStatusHistory> findByApplicationId(String applicationId) {
        return findAll().stream().filter(item -> item.applicationId().equals(applicationId)).toList();
    }

    public ApplicationStatusHistory save(String applicationId, ApplicationStatus status, String note, String changedBy, String changedAt) {
        List<ApplicationStatusHistory> all = new ArrayList<>(findAll());
        ApplicationStatusHistory saved = new ApplicationStatusHistory(
                nextHistoryId(all),
                applicationId,
                status,
                sanitize(note),
                sanitize(changedBy),
                sanitize(changedAt)
        );
        all.add(saved);
        rewriteAll(all);
        return saved;
    }

    public void deleteByApplicationPrefix(String prefix) {
        List<ApplicationStatusHistory> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.applicationId().startsWith(prefix));
        rewriteAll(all);
    }

    private String nextHistoryId(List<ApplicationStatusHistory> all) {
        return all.stream()
                .map(ApplicationStatusHistory::historyId)
                .filter(id -> id.startsWith("H"))
                .map(id -> id.substring(1))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("H%04d", max + 1))
                .orElse("H0001");
    }

    private void rewriteAll(List<ApplicationStatusHistory> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (ApplicationStatusHistory item : rows) {
            lines.add(String.join(",",
                    sanitize(item.historyId()),
                    sanitize(item.applicationId()),
                    item.status().name(),
                    sanitize(item.note()),
                    sanitize(item.changedBy()),
                    sanitize(item.changedAt())
            ));
        }
        try {
            Files.write(historyCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write application_status_history.csv", ex);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
