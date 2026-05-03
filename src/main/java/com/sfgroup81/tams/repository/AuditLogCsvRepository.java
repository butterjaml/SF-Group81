package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.AuditLogEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AuditLogCsvRepository {
    private static final String HEADER = "log_id,event_type,user_id,user_name,event_time,ip_address,details";

    private final Path auditLogCsv;

    public AuditLogCsvRepository() {
        this(Path.of("data"));
    }

    public AuditLogCsvRepository(Path dataDir) {
        this.auditLogCsv = dataDir.resolve("audit_logs.csv");
    }

    public List<AuditLogEntry> findAll() {
        try {
            if (Files.notExists(auditLogCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(auditLogCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<AuditLogEntry> entries = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < 7) {
                    continue;
                }
                entries.add(new AuditLogEntry(
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        cols[4],
                        cols[5],
                        cols[6]
                ));
            }
            return entries;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read audit_logs.csv", ex);
        }
    }

    public AuditLogEntry save(AuditLogEntry entry) {
        ensureFile();
        try {
            Files.writeString(
                    auditLogCsv,
                    String.join(",",
                            sanitize(entry.logId()),
                            sanitize(entry.eventType()),
                            sanitize(entry.userId()),
                            sanitize(entry.userName()),
                            sanitize(entry.eventTime()),
                            sanitize(entry.ipAddress()),
                            sanitize(entry.details()))
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            return entry;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write audit_logs.csv", ex);
        }
    }

    public String nextLogId() {
        return findAll().stream()
                .map(AuditLogEntry::logId)
                .filter(id -> id.startsWith("LOG"))
                .map(id -> id.substring(3))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("LOG%05d", max + 1))
                .orElse("LOG00001");
    }

    private void ensureFile() {
        try {
            if (Files.notExists(auditLogCsv)) {
                Files.writeString(
                        auditLogCsv,
                        HEADER + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW
                );
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize audit_logs.csv", ex);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
