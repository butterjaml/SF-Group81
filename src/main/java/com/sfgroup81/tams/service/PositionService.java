package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.PositionCsvRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PositionService {
    private final PositionCsvRepository repository;

    public PositionService(PositionCsvRepository repository) {
        this.repository = repository;
    }

    public List<TAPosition> listAll() {
        closeExpiredPositions();
        return repository.findAll();
    }

    public List<TAPosition> listPublishedPositions() {
        return listAll().stream()
                .filter(position -> "PUBLISHED".equalsIgnoreCase(position.status()))
                .toList();
    }

    public List<TAPosition> listOpenPublishedPositions() {
        closeExpiredPositions();
        return repository.findAll().stream()
                .filter(position -> "PUBLISHED".equalsIgnoreCase(position.status()))
                .toList();
    }

    public Map<String, TAPosition> listById() {
        Map<String, TAPosition> positions = new LinkedHashMap<>();
        for (TAPosition position : listAll()) {
            positions.put(position.positionId(), position);
        }
        return positions;
    }

    public TAPosition getById(String positionId) {
        closeExpiredPositions();
        return repository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));
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
        return savePosition(new PositionUpsertRequest(
                positionId,
                courseId,
                title,
                "",
                semesterId,
                positionType,
                headcount,
                deadline,
                status,
                title,
                description,
                "",
                "",
                "",
                "",
                "",
                createdBy
        ), createdBy);
    }

    public TAPosition savePosition(PositionUpsertRequest request, String operatorUserId) {
        if (request.courseId() == null || request.courseId().isBlank()) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (request.courseName() == null || request.courseName().isBlank()) {
            throw new IllegalArgumentException("Course name is required");
        }
        if (request.semesterId() == null || request.semesterId().isBlank()) {
            throw new IllegalArgumentException("Semester ID is required");
        }
        if (request.positionType() == null || request.positionType().isBlank()) {
            throw new IllegalArgumentException("Position type is required");
        }
        if (request.headcount() <= 0) {
            throw new IllegalArgumentException("Headcount must be positive");
        }
        String status = normalizeStatus(request.status());
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String resolvedId = (request.positionId() == null || request.positionId().isBlank())
                ? repository.nextPositionId()
                : request.positionId().trim();
        TAPosition existing = repository.findById(resolvedId).orElse(null);
        ensureEditable(existing);
        String creator = resolveCreator(request.createdBy(), operatorUserId, existing);

        TAPosition saved = new TAPosition(
                resolvedId,
                request.courseId().trim(),
                request.courseName().trim(),
                safe(request.instructorName()),
                request.semesterId().trim(),
                request.positionType().trim(),
                request.headcount(),
                safe(request.deadline()),
                status,
                safe(request.title()),
                safe(request.responsibilities()),
                safe(request.workingHours()),
                safe(request.salaryInfo()),
                safe(request.mandatoryRequirements()),
                safe(request.preferredRequirements()),
                safe(request.bonusRequirements()),
                creator,
                existing == null ? now : existing.createdAt(),
                now
        );
        return repository.saveOrUpdate(saved);
    }

    public TAPosition unpublish(String positionId) {
        TAPosition existing = repository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));
        ensureEditable(existing);
        TAPosition closed = new TAPosition(
                existing.positionId(),
                existing.courseId(),
                existing.courseName(),
                existing.instructorName(),
                existing.semesterId(),
                existing.positionType(),
                existing.headcount(),
                existing.deadline(),
                "UNPUBLISHED",
                existing.title(),
                existing.responsibilities(),
                existing.workingHours(),
                existing.salaryInfo(),
                existing.mandatoryRequirements(),
                existing.preferredRequirements(),
                existing.bonusRequirements(),
                existing.createdBy(),
                existing.createdAt(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        return repository.saveOrUpdate(closed);
    }

    public void closeExpiredPositions() {
        closeExpiredPositions(LocalDate.now());
    }

    public void closeExpiredPositions(LocalDate today) {
        List<TAPosition> positions = repository.findAll();
        for (TAPosition p : positions) {
            if (!"PUBLISHED".equalsIgnoreCase(p.status())) {
                continue;
            }
            if (p.deadline() == null || p.deadline().isBlank()) {
                continue;
            }
            try {
                LocalDate deadline = LocalDate.parse(p.deadline());
                if (deadline.isBefore(today)) {
                    TAPosition closed = new TAPosition(
                            p.positionId(),
                            p.courseId(),
                            p.courseName(),
                            p.instructorName(),
                            p.semesterId(),
                            p.positionType(),
                            p.headcount(),
                            p.deadline(),
                            "CLOSED",
                            p.title(),
                            p.responsibilities(),
                            p.workingHours(),
                            p.salaryInfo(),
                            p.mandatoryRequirements(),
                            p.preferredRequirements(),
                            p.bonusRequirements(),
                            p.createdBy(),
                            p.createdAt(),
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    );
                    repository.saveOrUpdate(closed);
                }
            } catch (Exception ignored) {
                // Keep malformed deadline rows unchanged.
            }
        }
    }

    private void ensureEditable(TAPosition existing) {
        if (existing == null) {
            return;
        }
        if (existing.deadline() == null || existing.deadline().isBlank()) {
            return;
        }
        try {
            LocalDate deadline = LocalDate.parse(existing.deadline());
            if (!deadline.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Published positions cannot be edited after the deadline");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ignored) {
            // Ignore malformed deadlines and allow editing.
        }
    }

    private String resolveCreator(String requestCreatedBy, String operatorUserId, TAPosition existing) {
        if (existing != null) {
            return existing.createdBy();
        }
        if (requestCreatedBy != null && !requestCreatedBy.isBlank()) {
            return requestCreatedBy.trim();
        }
        if (operatorUserId != null && !operatorUserId.isBlank()) {
            return operatorUserId.trim();
        }
        return "SYSTEM";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeStatus(String status) {
        String resolved = status == null || status.isBlank() ? "DRAFT" : status.trim().toUpperCase();
        return switch (resolved) {
            case "DRAFT", "PUBLISHED", "UNPUBLISHED", "CLOSED" -> resolved;
            default -> throw new IllegalArgumentException("Unsupported position status: " + status);
        };
    }
}
