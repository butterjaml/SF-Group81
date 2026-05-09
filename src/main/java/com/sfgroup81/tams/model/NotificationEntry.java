package com.sfgroup81.tams.model;

public record NotificationEntry(
        String notificationId,
        String userId,
        String createdAt,
        String title,
        String message,
        String relatedPage,
        String relatedId,
        String readAt
) {
    public boolean isRead() {
        return readAt != null && !readAt.isBlank();
    }
}
