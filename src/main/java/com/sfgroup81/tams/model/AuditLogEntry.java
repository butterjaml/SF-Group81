package com.sfgroup81.tams.model;

public record AuditLogEntry(
        String logId,
        String eventType,
        String userId,
        String userName,
        String eventTime,
        String ipAddress,
        String details
) {
    public String userDisplay() {
        if (userName == null || userName.isBlank()) {
            return userId == null || userId.isBlank() ? "SYSTEM" : userId;
        }
        if (userId == null || userId.isBlank()) {
            return userName;
        }
        return userName + " (" + userId + ")";
    }
}
