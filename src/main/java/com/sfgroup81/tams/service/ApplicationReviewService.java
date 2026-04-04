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

    public ApplicationReviewService(TAApplicationCsvRepository applicationRepository,
                                    ApplicationStatusHistoryCsvRepository historyRepository,
                                    PositionCsvRepository positionRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.positionRepository = positionRepository;
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
                existing.priorityNo(),
                status,
                note == null ? "" : note.trim(),
                existing.submittedAt(),
                now
        );
        applicationRepository.saveOrUpdate(updated);
        historyRepository.save(existing.applicationId(), status, note, changedBy, now);
        return updated;
    }
}
