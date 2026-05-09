package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ApplicationReviewService {
    private final TAApplicationCsvRepository applicationRepository;
    private final ApplicationStatusHistoryCsvRepository historyRepository;
    private final PositionCsvRepository positionRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ApplicationReviewService(TAApplicationCsvRepository applicationRepository,
                                    ApplicationStatusHistoryCsvRepository historyRepository,
                                    PositionCsvRepository positionRepository) {
        this(applicationRepository, historyRepository, positionRepository, AuditLogService.noop(), NotificationService.noop());
    }

    public ApplicationReviewService(TAApplicationCsvRepository applicationRepository,
                                    ApplicationStatusHistoryCsvRepository historyRepository,
                                    PositionCsvRepository positionRepository,
                                    AuditLogService auditLogService) {
        this(applicationRepository, historyRepository, positionRepository, auditLogService, NotificationService.noop());
    }

    public ApplicationReviewService(TAApplicationCsvRepository applicationRepository,
                                    ApplicationStatusHistoryCsvRepository historyRepository,
                                    PositionCsvRepository positionRepository,
                                    AuditLogService auditLogService,
                                    NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.positionRepository = positionRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public TAApplication updateStatus(String applicationId, ApplicationStatus status, String note, String changedBy) {
        TAApplication existing = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        positionRepository.findById(existing.positionId())
                .orElseThrow(() -> new IllegalArgumentException("Position not found for application: " + existing.positionId()));

        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        TAApplication updated = new TAApplication(
                existing.applicationId(),
                existing.userId(),
                existing.positionId(),
                existing.semesterId(),
                existing.priorityNo(),
                status,
                note == null ? "" : note.trim(),
                existing.submittedAt(),
                now
        );
        applicationRepository.saveOrUpdate(updated);
        historyRepository.save(existing.applicationId(), status, note, changedBy, now);
        auditLogService.record("APPLICATION_STATUS_CHANGED", changedBy,
                "Updated " + applicationId + " to " + status);
        notificationService.notifyUser(
                existing.userId(),
                "Application status updated",
                buildNotificationMessage(existing.positionId(), status, note),
                status == ApplicationStatus.INTERVIEW ? "TA_INTERVIEWS" : "TA_STATUS",
                existing.applicationId()
        );
        return updated;
    }

    private String buildNotificationMessage(String positionId, ApplicationStatus status, String note) {
        String positionLabel = positionRepository.findById(positionId)
                .map(position -> position.courseId() + " / " + position.title())
                .orElse(positionId);
        StringBuilder builder = new StringBuilder("Your application for ");
        builder.append(positionLabel).append(" is now ").append(status).append(".");
        String normalizedNote = note == null ? "" : note.trim();
        if (!normalizedNote.isBlank()) {
            builder.append(" Note: ").append(normalizedNote);
        }
        return builder.toString();
    }
}
