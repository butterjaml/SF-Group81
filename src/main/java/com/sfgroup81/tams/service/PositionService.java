package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.PositionCsvRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PositionService {
    private final PositionCsvRepository repository;

    public PositionService(PositionCsvRepository repository) {
        this.repository = repository;
    }

    public List<TAPosition> listAll() {
        return repository.findAll();
    }

    public TAPosition savePosition(String positionId,
                                   String courseId,
                                   String semesterId,
                                   String positionType,
                                   int headcount,
                                   String deadline,
                                   String title,
                                   String description,
                                   String createdBy,
                                   String status) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (semesterId == null || semesterId.isBlank()) {
            throw new IllegalArgumentException("Semester ID is required");
        }
        if (positionType == null || positionType.isBlank()) {
            throw new IllegalArgumentException("Position type is required");
        }
        if (headcount <= 0) {
            throw new IllegalArgumentException("Headcount must be positive");
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String resolvedId = (positionId == null || positionId.isBlank()) ? repository.nextPositionId() : positionId;
        String creator = (createdBy == null || createdBy.isBlank()) ? "SYSTEM" : createdBy;

        TAPosition existing = repository.findById(resolvedId).orElse(null);
        TAPosition saved = new TAPosition(
                resolvedId,
                courseId.trim(),
                semesterId.trim(),
                positionType.trim(),
                headcount,
                deadline == null ? "" : deadline.trim(),
                status,
                title == null ? "" : title.trim(),
                description == null ? "" : description.trim(),
                existing == null ? creator : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                now
        );
        return repository.saveOrUpdate(saved);
    }
}
