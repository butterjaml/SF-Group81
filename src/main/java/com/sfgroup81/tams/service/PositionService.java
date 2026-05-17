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
    private final AuditLogService auditLogService;
    private final SemesterService semesterService;

    public PositionService(PositionCsvRepository repository) {
        this(repository, null, AuditLogService.noop());
    }

    public PositionService(PositionCsvRepository repository, AuditLogService auditLogService) {
        this(repository, null, auditLogService);
    }

    public PositionService(PositionCsvRepository repository, SemesterService semesterService, AuditLogService auditLogService) {
        this.repository = repository;
        this.semesterService = semesterService;
        this.auditLogService = auditLogService;
    }

    public List<TAPosition> listAll() {
        closeExpiredPositions();
        return visiblePositions(repository.findAll());
    }

    public List<TAPosition> listByCreator(String creatorUserId) {
        String creator = safe(creatorUserId);
        if (creator.isBlank()) {
            return List.of();
        }
        closeExpiredPositions();
        return visiblePositions(repository.findAll()).stream()
                .filter(position -> creator.equals(position.createdBy()))
                .toList();
    }

    public String viewedSemesterId() {
        return semesterService == null ? "" : semesterService.viewedSemesterId();
    }

    public List<TAPosition> listAllSemesters() {
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
        return visiblePositions(repository.findAll()).stream()
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
        ensureMatchesViewedSemester(request.semesterId());
        if (request.positionType() == null || request.positionType().isBlank()) {
            throw new IllegalArgumentException("Position type is required");
        }
        if (request.headcount() <= 0) {
            throw new IllegalArgumentException("Headcount must be positive");
        }
        String status = normalizeStatus(request.status());
        validateDeadline(request.deadline(), status);
        AiSkillWeightFormat.validate(request.aiScreeningCriteria());
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String resolvedId = (request.positionId() == null || request.positionId().isBlank())
                ? repository.nextPositionId()
                : request.positionId().trim();
        TAPosition existing = repository.findById(resolvedId).orElse(null);
        ensureEditable(existing);
        ensureOwner(existing, operatorUserId);
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
                AiSkillWeightFormat.normalizeOrDefault(request.aiScreeningCriteria()),
                creator,
                existing == null ? now : existing.createdAt(),
                now
        );
        TAPosition result = repository.saveOrUpdate(saved);
        auditLogService.record(existing == null ? "POSITION_CREATED" : "POSITION_UPDATED", operatorUserId,
                result.positionId() + " / " + result.courseName() + " / " + result.status());
        return result;
    }

    public TAPosition unpublish(String positionId) {
        return unpublish(positionId, null);
    }

    public TAPosition unpublish(String positionId, String operatorUserId) {
        TAPosition existing = repository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));
        ensureEditable(existing);
        ensureOwner(existing, operatorUserId);
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
                existing.aiScreeningCriteria(),
                existing.createdBy(),
                existing.createdAt(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        TAPosition result = repository.saveOrUpdate(closed);
        auditLogService.record("POSITION_UNPUBLISHED",
                safe(operatorUserId).isBlank() ? existing.createdBy() : operatorUserId,
                "Unpublished " + result.positionId() + " / " + result.courseName());
        return result;
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
                            p.aiScreeningCriteria(),
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
        String operator = safe(operatorUserId);
        if (!operator.isBlank() && !isSystemOperator(operator)) {
            return operator;
        }
        if (requestCreatedBy != null && !requestCreatedBy.isBlank()) {
            return requestCreatedBy.trim();
        }
        if (!operator.isBlank()) {
            return operator;
        }
        return "SYSTEM";
    }

    private void ensureOwner(TAPosition existing, String operatorUserId) {
        if (existing == null) {
            return;
        }
        String operator = safe(operatorUserId);
        if (operator.isBlank() || isSystemOperator(operator)) {
            return;
        }
        if (!operator.equals(existing.createdBy())) {
            throw new IllegalArgumentException("Only the MO who created this position can modify it");
        }
    }

    private boolean isSystemOperator(String operatorUserId) {
        return "SYSTEM".equalsIgnoreCase(safe(operatorUserId));
    }

    private void ensureMatchesViewedSemester(String semesterId) {
        String viewed = viewedSemesterId();
        if (!viewed.isBlank() && !viewed.equalsIgnoreCase(safe(semesterId))) {
            throw new IllegalArgumentException("Position semester must match the current viewed semester (" + viewed + ")");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateDeadline(String deadline, String status) {
        String value = safe(deadline);
        if (value.isBlank()) {
            return;
        }
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Deadline must use YYYY-MM-DD format, for example 2026-04-15");
        }
        if ("PUBLISHED".equalsIgnoreCase(safe(status)) && parsed.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Published positions must use today or a future deadline so TAs can select the job");
        }
    }

    private String normalizeStatus(String status) {
        String resolved = status == null || status.isBlank() ? "DRAFT" : status.trim().toUpperCase();
        return switch (resolved) {
            case "DRAFT", "PUBLISHED", "UNPUBLISHED", "CLOSED" -> resolved;
            default -> throw new IllegalArgumentException("Unsupported position status: " + status);
        };
    }

    private List<TAPosition> visiblePositions(List<TAPosition> positions) {
        if (semesterService == null) {
            return positions;
        }
        return positions.stream()
                .filter(position -> semesterService.matchesViewedSemester(position.semesterId()))
                .toList();
    }
}
