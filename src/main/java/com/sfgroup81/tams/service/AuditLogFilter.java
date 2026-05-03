package com.sfgroup81.tams.service;

import java.time.LocalDate;

public record AuditLogFilter(
        LocalDate fromDate,
        LocalDate toDate,
        String userKeyword,
        String eventType
) {
}
