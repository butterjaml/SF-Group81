package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.InterviewInvitation;
import com.sfgroup81.tams.model.InterviewResponseStatus;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.InterviewInvitationCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class InterviewService {
    private final TAApplicationCsvRepository applicationRepository;
    private final ApplicationStatusHistoryCsvRepository historyRepository;
    private final InterviewInvitationCsvRepository invitationRepository;
    private final AuditLogService auditLogService;
    private final SemesterService semesterService;
    private final NotificationService notificationService;

    public InterviewService(TAApplicationCsvRepository applicationRepository,
                            ApplicationStatusHistoryCsvRepository historyRepository,
                            InterviewInvitationCsvRepository invitationRepository) {
        this(applicationRepository, historyRepository, invitationRepository, null, AuditLogService.noop(), NotificationService.noop());
    }

    public InterviewService(TAApplicationCsvRepository applicationRepository,
                            ApplicationStatusHistoryCsvRepository historyRepository,
                            InterviewInvitationCsvRepository invitationRepository,
                            AuditLogService auditLogService) {
        this(applicationRepository, historyRepository, invitationRepository, null, auditLogService, NotificationService.noop());
    }

    public InterviewService(TAApplicationCsvRepository applicationRepository,
                            ApplicationStatusHistoryCsvRepository historyRepository,
                            InterviewInvitationCsvRepository invitationRepository,
                            SemesterService semesterService,
                            AuditLogService auditLogService) {
        this(applicationRepository, historyRepository, invitationRepository, semesterService, auditLogService, NotificationService.noop());
    }

    public InterviewService(TAApplicationCsvRepository applicationRepository,
                            ApplicationStatusHistoryCsvRepository historyRepository,
                            InterviewInvitationCsvRepository invitationRepository,
                            SemesterService semesterService,
                            AuditLogService auditLogService,
                            NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.invitationRepository = invitationRepository;
        this.semesterService = semesterService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public InterviewInvitation scheduleInterview(String applicationId,
                                                 String scheduledAt,
                                                 String location,
                                                 String notes,
                                                 String changedBy) {
        return scheduleInterview(applicationId, scheduledAt, location, notes, "", changedBy);
    }

    public InterviewInvitation scheduleInterview(String applicationId,
                                                 String scheduledAt,
                                                 String location,
                                                 String notes,
                                                 String onlineLink,
                                                 String changedBy) {
        TAApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        validateScheduledAt(scheduledAt);
        String now = now();
        applicationRepository.saveOrUpdate(new TAApplication(
                application.applicationId(),
                application.userId(),
                application.positionId(),
                application.semesterId(),
                application.priorityNo(),
                ApplicationStatus.INTERVIEW,
                application.feedback(),
                application.submittedAt(),
                now
        ));
        String linkNote = safe(onlineLink).isBlank() ? "" : " Online link: " + safe(onlineLink);
        historyRepository.save(applicationId, ApplicationStatus.INTERVIEW,
                "Interview arranged at " + safe(scheduledAt) + " in " + safe(location) + ". " + safe(notes) + linkNote,
                changedBy,
                now);
        InterviewInvitation invitation = new InterviewInvitation(
                invitationRepository.nextInvitationId(),
                applicationId,
                safe(scheduledAt),
                safe(location),
                safe(notes),
                safe(onlineLink),
                InterviewResponseStatus.PENDING_CONFIRMATION,
                "",
                safe(changedBy),
                now
        );
        InterviewInvitation saved = invitationRepository.saveOrUpdate(invitation);
        auditLogService.record("INTERVIEW_SCHEDULED", changedBy,
                "Scheduled interview for " + applicationId + " at " + scheduledAt + " / " + location);
        notificationService.notifyUser(
                application.userId(),
                "Interview invitation scheduled",
                "Interview arranged for " + safe(scheduledAt) + " at " + safe(location)
                        + (safe(onlineLink).isBlank() ? "" : " | Link: " + safe(onlineLink)),
                "TA_INTERVIEWS",
                application.applicationId()
        );
        return saved;
    }

    public InterviewInvitation respondToInterview(String invitationId,
                                                  InterviewResponseStatus responseStatus,
                                                  String responseNote,
                                                  String changedBy) {
        InterviewInvitation existing = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Interview invitation not found: " + invitationId));
        String now = now();
        InterviewInvitation updated = new InterviewInvitation(
                existing.invitationId(),
                existing.applicationId(),
                existing.scheduledAt(),
                existing.location(),
                existing.notes(),
                existing.onlineLink(),
                responseStatus,
                safe(responseNote),
                existing.createdBy(),
                now
        );
        invitationRepository.saveOrUpdate(updated);
        historyRepository.save(existing.applicationId(), ApplicationStatus.INTERVIEW, historyNote(responseStatus, responseNote), changedBy, now);
        auditLogService.record("INTERVIEW_RESPONSE_UPDATED", changedBy,
                "Updated interview response for " + existing.applicationId() + " to " + responseStatus);
        return updated;
    }

    public List<InterviewInvitation> listForApplicant(String userId) {
        List<InterviewInvitation> invitations = applicationRepository.findByUserId(userId).stream()
                .filter(this::isVisibleInSemesterView)
                .flatMap(application -> invitationRepository.findByApplicationId(application.applicationId()).stream())
                .sorted(Comparator.comparing(InterviewInvitation::scheduledAt))
                .toList();
        auditLogService.record("DATA_ACCESS", userId, "Viewed TA interview invitations");
        return invitations;
    }

    public List<InterviewInvitation> listForApplicantWithoutAudit(String userId) {
        return applicationRepository.findByUserId(userId).stream()
                .filter(this::isVisibleInSemesterView)
                .flatMap(application -> invitationRepository.findByApplicationId(application.applicationId()).stream())
                .sorted(Comparator.comparing(InterviewInvitation::scheduledAt))
                .toList();
    }

    public Optional<InterviewInvitation> findLatestInvitationForApplication(String applicationId) {
        return invitationRepository.findByApplicationId(applicationId).stream()
                .max(Comparator.comparing(InterviewInvitation::updatedAt));
    }

    public List<String> listReminderMessages(String userId, LocalDateTime now) {
        LocalDateTime reminderDeadline = now.plusDays(2);
        return listForApplicantWithoutAudit(userId).stream()
                .filter(invitation -> withinReminderWindow(invitation.scheduledAt(), now, reminderDeadline))
                .map(invitation -> "Reminder: interview at " + invitation.location() + " on " + invitation.scheduledAt())
                .toList();
    }

    private boolean withinReminderWindow(String scheduledAt, LocalDateTime now, LocalDateTime reminderDeadline) {
        try {
            LocalDateTime scheduled = LocalDateTime.parse(scheduledAt);
            return !scheduled.isBefore(now) && !scheduled.isAfter(reminderDeadline);
        } catch (Exception ex) {
            return false;
        }
    }

    private void validateScheduledAt(String scheduledAt) {
        String value = safe(scheduledAt);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Interview time is required in YYYY-MM-DDTHH:MM:SS format, for example 2026-04-01T10:00:00");
        }
        try {
            LocalDateTime.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Interview time must use YYYY-MM-DDTHH:MM:SS format, for example 2026-04-01T10:00:00");
        }
    }

    private String historyNote(InterviewResponseStatus responseStatus, String responseNote) {
        String action = switch (responseStatus) {
            case CONFIRMED -> "Interview confirmed";
            case RESCHEDULE_REQUESTED -> "Interview reschedule requested";
            case PENDING_CONFIRMATION -> "Interview response pending";
        };
        String note = safe(responseNote);
        return note.isBlank() ? action : action + ": " + note;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isVisibleInSemesterView(TAApplication application) {
        if (semesterService == null) {
            return true;
        }
        return semesterService.matchesViewedSemester(application.semesterId());
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
