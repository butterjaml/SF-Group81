package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;

import java.util.Comparator;
import java.util.List;

public class ApplicationStatusService {
    private final TAApplicationCsvRepository applicationRepository;
    private final ApplicationStatusHistoryCsvRepository historyRepository;
    private final PositionCsvRepository positionRepository;
    private final AuditLogService auditLogService;
    private final SemesterService semesterService;

    public ApplicationStatusService(TAApplicationCsvRepository applicationRepository,
                                    ApplicationStatusHistoryCsvRepository historyRepository,
                                    PositionCsvRepository positionRepository) {
        this(applicationRepository, historyRepository, positionRepository, null, AuditLogService.noop());
    }

    public ApplicationStatusService(TAApplicationCsvRepository applicationRepository,
                                    ApplicationStatusHistoryCsvRepository historyRepository,
                                    PositionCsvRepository positionRepository,
                                    AuditLogService auditLogService) {
        this(applicationRepository, historyRepository, positionRepository, null, auditLogService);
    }

    public ApplicationStatusService(TAApplicationCsvRepository applicationRepository,
                                    ApplicationStatusHistoryCsvRepository historyRepository,
                                    PositionCsvRepository positionRepository,
                                    SemesterService semesterService,
                                    AuditLogService auditLogService) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.positionRepository = positionRepository;
        this.semesterService = semesterService;
        this.auditLogService = auditLogService;
    }

    public List<ApplicantApplicationView> listForApplicant(String userId) {
        List<ApplicantApplicationView> views = applicationRepository.findByUserId(userId).stream()
                .filter(this::isVisibleInSemesterView)
                .sorted(Comparator.comparingInt(TAApplication::priorityNo))
                .map(application -> new ApplicantApplicationView(
                        application,
                        findPosition(application.positionId()),
                        historyRepository.findByApplicationId(application.applicationId())
                ))
                .toList();
        auditLogService.record("DATA_ACCESS", userId, "Viewed TA application status and progress records");
        return views;
    }

    private TAPosition findPosition(String positionId) {
        return positionRepository.findById(positionId).orElse(null);
    }

    private boolean isVisibleInSemesterView(TAApplication application) {
        if (semesterService == null) {
            return true;
        }
        TAPosition position = findPosition(application.positionId());
        String semesterId = application.semesterId() == null || application.semesterId().isBlank()
                ? position == null ? "" : position.semesterId()
                : application.semesterId();
        return semesterService.matchesViewedSemester(semesterId);
    }
}
