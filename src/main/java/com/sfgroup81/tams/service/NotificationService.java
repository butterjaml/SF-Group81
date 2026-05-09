package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.NotificationEntry;
import com.sfgroup81.tams.repository.NotificationCsvRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationService {
    private static final NotificationService NO_OP = new NotificationService(null, true);

    private final NotificationCsvRepository repository;
    private final boolean disabled;

    public NotificationService(NotificationCsvRepository repository) {
        this(repository, false);
    }

    private NotificationService(NotificationCsvRepository repository, boolean disabled) {
        this.repository = repository;
        this.disabled = disabled;
    }

    public static NotificationService noop() {
        return NO_OP;
    }

    public NotificationEntry notifyUser(String userId,
                                        String title,
                                        String message,
                                        String relatedPage,
                                        String relatedId) {
        if (disabled || repository == null) {
            return null;
        }
        NotificationEntry entry = new NotificationEntry(
                repository.nextNotificationId(),
                safe(userId),
                now(),
                safe(title),
                safe(message),
                safe(relatedPage),
                safe(relatedId),
                ""
        );
        return repository.saveOrUpdate(entry);
    }

    public List<NotificationEntry> listForUser(String userId) {
        if (disabled || repository == null) {
            return List.of();
        }
        return repository.findByUserId(safe(userId));
    }

    public int unreadCount(String userId) {
        return (int) listForUser(userId).stream()
                .filter(item -> !item.isRead())
                .count();
    }

    public void markAsRead(String notificationId, String userId) {
        if (disabled || repository == null) {
            return;
        }
        NotificationEntry entry = repository.findById(notificationId)
                .filter(item -> item.userId().equals(safe(userId)))
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (entry.isRead()) {
            return;
        }
        repository.saveOrUpdate(new NotificationEntry(
                entry.notificationId(),
                entry.userId(),
                entry.createdAt(),
                entry.title(),
                entry.message(),
                entry.relatedPage(),
                entry.relatedId(),
                now()
        ));
    }

    public void markAllAsRead(String userId) {
        for (NotificationEntry entry : listForUser(userId)) {
            markAsRead(entry.notificationId(), userId);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
