package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.SemesterRecord;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.SemesterCsvRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SemesterService {
    private final SemesterCsvRepository semesterRepository;
    private final PositionCsvRepository positionRepository;
    private final AuditLogService auditLogService;

    public SemesterService(SemesterCsvRepository semesterRepository, PositionCsvRepository positionRepository) {
        this(semesterRepository, positionRepository, AuditLogService.noop());
    }

    public SemesterService(SemesterCsvRepository semesterRepository,
                           PositionCsvRepository positionRepository,
                           AuditLogService auditLogService) {
        this.semesterRepository = semesterRepository;
        this.positionRepository = positionRepository;
        this.auditLogService = auditLogService;
        ensureInitialized();
    }

    public List<SemesterRecord> listSemesters() {
        ensureInitialized();
        return semesterRepository.findAll().stream()
                .sorted(Comparator.comparingInt((SemesterRecord item) -> semesterRank(item.semesterId())).reversed())
                .toList();
    }

    public String currentSemesterId() {
        ensureInitialized();
        return semesterRepository.findAll().stream()
                .filter(SemesterRecord::currentSemester)
                .map(SemesterRecord::semesterId)
                .findFirst()
                .orElseGet(this::defaultSemesterId);
    }

    public String viewedSemesterId() {
        ensureInitialized();
        return semesterRepository.findAll().stream()
                .filter(SemesterRecord::viewedSemester)
                .map(SemesterRecord::semesterId)
                .findFirst()
                .orElseGet(this::currentSemesterId);
    }

    public SemesterRecord createAndSwitchToNewSemester(String semesterId, String operatorUserId, String notes) {
        String normalizedSemesterId = normalizeSemesterId(semesterId);
        List<SemesterRecord> all = new ArrayList<>(listSemesters());
        String now = now();
        boolean alreadyExists = all.stream().anyMatch(item -> item.semesterId().equalsIgnoreCase(normalizedSemesterId));

        List<SemesterRecord> updated = new ArrayList<>();
        for (SemesterRecord item : all) {
            updated.add(new SemesterRecord(
                    item.semesterId(),
                    false,
                    false,
                    item.archived() || item.currentSemester(),
                    item.createdBy(),
                    item.createdAt(),
                    item.currentSemester() && item.archivedAt().isBlank() ? now : item.archivedAt(),
                    item.notes()
            ));
        }
        if (alreadyExists) {
            updated.replaceAll(item -> item.semesterId().equalsIgnoreCase(normalizedSemesterId)
                    ? new SemesterRecord(
                    item.semesterId(),
                    true,
                    true,
                    false,
                    item.createdBy(),
                    item.createdAt(),
                    "",
                    mergeNotes(item.notes(), notes)
            )
                    : item);
        } else {
            updated.add(new SemesterRecord(
                    normalizedSemesterId,
                    true,
                    true,
                    false,
                    safe(operatorUserId).isBlank() ? "SYSTEM" : safe(operatorUserId),
                    now,
                    "",
                    safe(notes)
            ));
        }
        semesterRepository.saveAll(updated);
        auditLogService.record("SEMESTER_CREATED", operatorUserId, "Created and switched to semester " + normalizedSemesterId);
        return semesterRepository.findBySemesterId(normalizedSemesterId).orElseThrow();
    }

    public SemesterRecord switchViewedSemester(String semesterId, String operatorUserId) {
        String normalizedSemesterId = normalizeSemesterId(semesterId);
        List<SemesterRecord> all = new ArrayList<>(listSemesters());
        Optional<SemesterRecord> target = all.stream()
                .filter(item -> item.semesterId().equalsIgnoreCase(normalizedSemesterId))
                .findFirst();
        if (target.isEmpty()) {
            throw new IllegalArgumentException("Semester not found: " + semesterId);
        }
        List<SemesterRecord> updated = all.stream()
                .map(item -> new SemesterRecord(
                        item.semesterId(),
                        item.currentSemester(),
                        item.semesterId().equalsIgnoreCase(normalizedSemesterId),
                        item.archived(),
                        item.createdBy(),
                        item.createdAt(),
                        item.archivedAt(),
                        item.notes()
                ))
                .toList();
        semesterRepository.saveAll(updated);
        auditLogService.record("SEMESTER_VIEW_SWITCHED", operatorUserId, "Viewing semester " + normalizedSemesterId);
        return semesterRepository.findBySemesterId(normalizedSemesterId).orElseThrow();
    }

    public boolean matchesViewedSemester(String semesterId) {
        String viewed = viewedSemesterId();
        String normalized = safe(semesterId);
        return viewed.isBlank() || viewed.equalsIgnoreCase(normalized);
    }

    private void ensureInitialized() {
        List<SemesterRecord> existing = semesterRepository.findAll();
        if (!existing.isEmpty()) {
            boolean hasCurrent = existing.stream().anyMatch(SemesterRecord::currentSemester);
            boolean hasViewed = existing.stream().anyMatch(SemesterRecord::viewedSemester);
            if (hasCurrent && hasViewed) {
                return;
            }
        }

        String inferredSemesterId = inferLatestSemesterId();
        String now = now();
        semesterRepository.saveAll(List.of(new SemesterRecord(
                inferredSemesterId,
                true,
                true,
                false,
                "SYSTEM",
                now,
                "",
                "Initialized from existing data"
        )));
    }

    private String inferLatestSemesterId() {
        return positionRepository.findAll().stream()
                .map(TAPosition::semesterId)
                .filter(item -> !safe(item).isBlank())
                .max(Comparator.comparingInt(this::semesterRank))
                .orElseGet(this::defaultSemesterId);
    }

    private String defaultSemesterId() {
        LocalDate today = LocalDate.now();
        return today.getYear() + (today.getMonthValue() <= 6 ? "S1" : "S2");
    }

    private String normalizeSemesterId(String semesterId) {
        String normalized = safe(semesterId).toUpperCase();
        if (!normalized.matches("\\d{4}S[12]")) {
            throw new IllegalArgumentException("Semester must use YYYYSk format, for example 2026S1 or 2026S2");
        }
        return normalized;
    }

    private int semesterRank(String semesterId) {
        String normalized = safe(semesterId).toUpperCase();
        if (!normalized.matches("\\d{4}S[12]")) {
            return Integer.MIN_VALUE;
        }
        int year = Integer.parseInt(normalized.substring(0, 4));
        int semester = Integer.parseInt(normalized.substring(5));
        return year * 10 + semester;
    }

    private String mergeNotes(String existing, String incoming) {
        String left = safe(existing);
        String right = safe(incoming);
        if (right.isBlank() || left.contains(right)) {
            return left;
        }
        if (left.isBlank()) {
            return right;
        }
        return left + " | " + right;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
