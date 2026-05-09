package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.NotificationEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NotificationCsvRepository {
    private static final String HEADER = "notification_id,user_id,created_at,title,message,related_page,related_id,read_at";

    private final Path notificationCsv;

    public NotificationCsvRepository() {
        this(Path.of("data"));
    }

    public NotificationCsvRepository(Path dataDir) {
        this.notificationCsv = dataDir.resolve("notifications.csv");
    }

    public List<NotificationEntry> findAll() {
        try {
            if (Files.notExists(notificationCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(notificationCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<NotificationEntry> notifications = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 8) {
                    continue;
                }
                notifications.add(new NotificationEntry(
                        cols[0], cols[1], cols[2], cols[3], cols[4], cols[5], cols[6], cols[7]
                ));
            }
            return notifications;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read notifications.csv", ex);
        }
    }

    public Optional<NotificationEntry> findById(String notificationId) {
        return findAll().stream()
                .filter(item -> item.notificationId().equals(notificationId))
                .findFirst();
    }

    public List<NotificationEntry> findByUserId(String userId) {
        return findAll().stream()
                .filter(item -> item.userId().equals(userId))
                .sorted(Comparator.comparing(NotificationEntry::createdAt).reversed())
                .toList();
    }

    public NotificationEntry saveOrUpdate(NotificationEntry entry) {
        List<NotificationEntry> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.notificationId().equals(entry.notificationId()));
        all.add(entry);
        rewriteAll(all);
        return entry;
    }

    public String nextNotificationId() {
        return findAll().stream()
                .map(NotificationEntry::notificationId)
                .filter(id -> id.startsWith("N"))
                .map(id -> id.substring(1))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("N%04d", max + 1))
                .orElse("N0001");
    }

    private void rewriteAll(List<NotificationEntry> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (NotificationEntry item : rows) {
            lines.add(String.join(",",
                    sanitize(item.notificationId()),
                    sanitize(item.userId()),
                    sanitize(item.createdAt()),
                    sanitize(item.title()),
                    sanitize(item.message()),
                    sanitize(item.relatedPage()),
                    sanitize(item.relatedId()),
                    sanitize(item.readAt())
            ));
        }
        try {
            Files.write(notificationCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write notifications.csv", ex);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
