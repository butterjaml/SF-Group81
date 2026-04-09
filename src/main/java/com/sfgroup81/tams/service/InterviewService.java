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

    public InterviewService(TAApplicationCsvRepository applicationRepository,
                            ApplicationStatusHistoryCsvRepository historyRepository,
                            InterviewInvitationCsvRepository invitationRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.invitationRepository = invitationRepository;
    }

    public InterviewInvitation scheduleInterview(String applicationId,
                                                 String scheduledAt,
                                                 String location,
                                                 String notes,
                                                 String changedBy) {
        TAApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        String now = now();
        applicationRepository.saveOrUpdate(new TAApplication(
                application.applicationId(),
                application.userId(),
                application.positionId(),
                application.priorityNo(),
                ApplicationStatus.INTERVIEW,
                application.feedback(),
                application.submittedAt(),
                now
        ));
        historyRepository.save(applicationId, ApplicationStatus.INTERVIEW,
                "Interview arranged at " + safe(scheduledAt) + " in " + safe(location) + ". " + safe(notes),
                changedBy,
                now);
        InterviewInvitation invitation = new InterviewInvitation(
                invitationRepository.nextInvitationId(),
                applicationId,
                safe(scheduledAt),
                safe(location),
                safe(notes),
                InterviewResponseStatus.PENDING_CONFIRMATION,
                "",
                safe(changedBy),
                now
        );
        return invitationRepository.saveOrUpdate(invitation);
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
                responseStatus,
                safe(responseNote),
                existing.createdBy(),
                now
        );
        invitationRepository.saveOrUpdate(updated);
        historyRepository.save(existing.applicationId(), ApplicationStatus.INTERVIEW, historyNote(responseStatus, responseNote), changedBy, now);
        return updated;
    }

    public List<InterviewInvitation> listForApplicant(String userId) {
        return applicationRepository.findByUserId(userId).stream()
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
        return listForApplicant(userId).stream()
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

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
