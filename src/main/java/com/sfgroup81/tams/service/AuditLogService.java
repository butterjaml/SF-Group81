package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.AuditLogEntry;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.AuditLogCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AuditLogService {
    private static final String DEFAULT_IP = "127.0.0.1";
    private static final AuditLogService NO_OP = new AuditLogService(null, null, true);

    private final AuditLogCsvRepository repository;
    private final UserCsvRepository userRepository;
    private final boolean disabled;

    public AuditLogService(AuditLogCsvRepository repository) {
        this(repository, null);
    }

    public AuditLogService(AuditLogCsvRepository repository, UserCsvRepository userRepository) {
        this(repository, userRepository, false);
    }

    private AuditLogService(AuditLogCsvRepository repository, UserCsvRepository userRepository, boolean disabled) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.disabled = disabled;
    }

    public static AuditLogService noop() {
        return NO_OP;
    }

    public AuditLogEntry record(String eventType, String userId, String details) {
        return record(eventType, userId, DEFAULT_IP, details);
    }

    public AuditLogEntry record(String eventType, String userId, String ipAddress, String details) {
        if (disabled || repository == null) {
            return null;
        }
        String normalizedUserId = safe(userId);
        Optional<User> user = userRepository == null
                ? Optional.empty()
                : userRepository.findAll().stream()
                .filter(item -> item.userId().equals(normalizedUserId))
                .findFirst();
        AuditLogEntry entry = new AuditLogEntry(
                repository.nextLogId(),
                safe(eventType),
                normalizedUserId,
                user.map(User::name).orElse(""),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                safe(ipAddress).isBlank() ? DEFAULT_IP : safe(ipAddress),
                safe(details)
        );
        return repository.save(entry);
    }

    public List<AuditLogEntry> listEntries(AuditLogFilter filter) {
        return repository.findAll().stream()
                .filter(entry -> matchesDate(entry, filter == null ? null : filter.fromDate(), filter == null ? null : filter.toDate()))
                .filter(entry -> matchesUser(entry, filter == null ? "" : filter.userKeyword()))
                .filter(entry -> matchesEvent(entry, filter == null ? "" : filter.eventType()))
                .sorted(Comparator.comparing(AuditLogEntry::eventTime).reversed())
                .toList();
    }

    private boolean matchesDate(AuditLogEntry entry, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        try {
            LocalDate date = LocalDateTime.parse(entry.eventTime()).toLocalDate();
            if (fromDate != null && date.isBefore(fromDate)) {
                return false;
            }
            return toDate == null || !date.isAfter(toDate);
        } catch (Exception ex) {
            return fromDate == null && toDate == null;
        }
    }

    private boolean matchesUser(AuditLogEntry entry, String userKeyword) {
        String keyword = safe(userKeyword).toLowerCase(Locale.ROOT);
        if (keyword.isBlank()) {
            return true;
        }
        return entry.userId().toLowerCase(Locale.ROOT).contains(keyword)
                || entry.userName().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesEvent(AuditLogEntry entry, String eventType) {
        String keyword = safe(eventType).toLowerCase(Locale.ROOT);
        return keyword.isBlank() || entry.eventType().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
