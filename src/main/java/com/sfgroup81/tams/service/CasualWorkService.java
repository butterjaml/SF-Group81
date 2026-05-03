package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.CasualWorkApplication;
import com.sfgroup81.tams.model.CasualWorkPosting;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.CasualWorkApplicationCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkPostingCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CasualWorkService {
    private final CasualWorkPostingCsvRepository postingRepository;
    private final CasualWorkApplicationCsvRepository applicationRepository;
    private final UserCsvRepository userRepository;
    private final TAApplicationCsvRepository taApplicationRepository;
    private final PositionCsvRepository positionRepository;
    private final AuditLogService auditLogService;

    public CasualWorkService(CasualWorkPostingCsvRepository postingRepository,
                             CasualWorkApplicationCsvRepository applicationRepository,
                             UserCsvRepository userRepository) {
        this(postingRepository,
                applicationRepository,
                userRepository,
                new TAApplicationCsvRepository(),
                new PositionCsvRepository(),
                AuditLogService.noop());
    }

    public CasualWorkService(CasualWorkPostingCsvRepository postingRepository,
                             CasualWorkApplicationCsvRepository applicationRepository,
                             UserCsvRepository userRepository,
                             TAApplicationCsvRepository taApplicationRepository,
                             PositionCsvRepository positionRepository) {
        this(postingRepository, applicationRepository, userRepository, taApplicationRepository, positionRepository, AuditLogService.noop());
    }

    public CasualWorkService(CasualWorkPostingCsvRepository postingRepository,
                             CasualWorkApplicationCsvRepository applicationRepository,
                             UserCsvRepository userRepository,
                             TAApplicationCsvRepository taApplicationRepository,
                             PositionCsvRepository positionRepository,
                             AuditLogService auditLogService) {
        this.postingRepository = postingRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.taApplicationRepository = taApplicationRepository;
        this.positionRepository = positionRepository;
        this.auditLogService = auditLogService;
    }

    public CasualWorkPosting createPosting(String title,
                                           String description,
                                           String workDate,
                                           String location,
                                           String requiredSkills,
                                           int headcount,
                                           String compensation,
                                           String createdBy) {
        User admin = requireUser(createdBy, UserRole.ADMIN);
        if (headcount <= 0) {
            throw new IllegalArgumentException("Headcount must be positive");
        }
        validateWorkDate(workDate);
        String now = now();
        CasualWorkPosting posting = postingRepository.saveOrUpdate(new CasualWorkPosting(
                postingRepository.nextPostingId(),
                safe(title),
                safe(description),
                safe(workDate),
                safe(location),
                safe(requiredSkills),
                headcount,
                safe(compensation),
                "OPEN",
                admin.userId(),
                now,
                now
        ));
        auditLogService.record("CASUAL_WORK_POSTED", createdBy, "Published casual work " + posting.postingId() + " / " + posting.title());
        return posting;
    }

    public CasualWorkApplication apply(String postingId, String userId, String statement) {
        requireCurrentSemesterTa(userId);
        CasualWorkPosting posting = postingRepository.findById(postingId)
                .orElseThrow(() -> new IllegalArgumentException("Casual work posting not found: " + postingId));
        if (!"OPEN".equalsIgnoreCase(posting.status())) {
            throw new IllegalArgumentException("Posting is not open: " + postingId);
        }
        if (applicationRepository.exists(postingId, userId)) {
            throw new IllegalArgumentException("Application already exists for this posting");
        }
        CasualWorkApplication application = applicationRepository.save(new CasualWorkApplication(
                applicationRepository.nextApplicationId(),
                postingId,
                userId,
                safe(statement),
                now()
        ));
        auditLogService.record("CASUAL_WORK_APPLIED", userId, "Applied for casual work " + postingId);
        return application;
    }

    public List<CasualWorkPosting> listOpenPostings() {
        return postingRepository.findAll().stream()
                .filter(item -> "OPEN".equalsIgnoreCase(item.status()))
                .toList();
    }

    public List<CasualWorkApplication> listApplicationsForPosting(String postingId) {
        return applicationRepository.findByPostingId(postingId);
    }

    public boolean canApplyCasualWork(String userId) {
        try {
            User user = requireUser(userId, UserRole.TA);
            return hasCurrentSemesterAssignment(user.userId(), LocalDate.now());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private User requireUser(String userId, UserRole role) {
        return userRepository.findAll().stream()
                .filter(user -> user.userId().equals(userId))
                .findFirst()
                .filter(user -> user.role() == role)
                .orElseThrow(() -> new IllegalArgumentException("User role mismatch for " + userId));
    }

    private User requireCurrentSemesterTa(String userId) {
        User user = requireUser(userId, UserRole.TA);
        if (!hasCurrentSemesterAssignment(user.userId(), LocalDate.now())) {
            throw new IllegalArgumentException("Casual work is available only to TAs hired for the current semester");
        }
        return user;
    }

    private boolean hasCurrentSemesterAssignment(String userId, LocalDate today) {
        String currentSemester = currentSemesterId(today);
        for (TAApplication application : taApplicationRepository.findByUserId(userId)) {
            if (application.status() != ApplicationStatus.HIRED) {
                continue;
            }
            TAPosition position = positionRepository.findById(application.positionId()).orElse(null);
            if (position != null && position.semesterId().equalsIgnoreCase(currentSemester)) {
                return true;
            }
        }
        return false;
    }

    private String currentSemesterId(LocalDate today) {
        return today.getYear() + (today.getMonthValue() <= 6 ? "S1" : "S2");
    }

    private void validateWorkDate(String workDate) {
        String value = safe(workDate);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Work date is required in YYYY-MM-DD format, for example 2026-04-15");
        }
        try {
            LocalDate.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Work date must use YYYY-MM-DD format, for example 2026-04-15");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
