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

    public ApplicationStatusService(TAApplicationCsvRepository applicationRepository,
                                    ApplicationStatusHistoryCsvRepository historyRepository,
                                    PositionCsvRepository positionRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.positionRepository = positionRepository;
    }

    public List<ApplicantApplicationView> listForApplicant(String userId) {
        return applicationRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparingInt(TAApplication::priorityNo))
                .map(application -> new ApplicantApplicationView(
                        application,
                        findPosition(application.positionId()),
                        historyRepository.findByApplicationId(application.applicationId())
                ))
                .toList();
    }

    private TAPosition findPosition(String positionId) {
        return positionRepository.findById(positionId).orElse(null);
    }
}
