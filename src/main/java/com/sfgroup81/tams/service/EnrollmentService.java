package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.EnrollmentProfileSnapshot;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.EnrollmentProfileSnapshotCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnrollmentService {
    private static final int MAX_SELECTION = 3;

    private final UserCsvRepository userRepository;
    private final PositionCsvRepository positionRepository;
    private final ApplicantProfileCsvRepository profileRepository;
    private final ResumeUploadService resumeUploadService;
    private final TAApplicationCsvRepository applicationRepository;
    private final ApplicationStatusHistoryCsvRepository historyRepository;
    private final ApplicationPreferenceCsvRepository preferenceRepository;
    private final EnrollmentProfileSnapshotCsvRepository snapshotRepository;
    private final AuditLogService auditLogService;

    public EnrollmentService(UserCsvRepository userRepository,
                             PositionCsvRepository positionRepository,
                             ApplicantProfileCsvRepository profileRepository,
                             ResumeUploadService resumeUploadService,
                             TAApplicationCsvRepository applicationRepository,
                             ApplicationStatusHistoryCsvRepository historyRepository) {
        this(userRepository,
                positionRepository,
                profileRepository,
                resumeUploadService,
                applicationRepository,
                historyRepository,
                new ApplicationPreferenceCsvRepository(),
                new EnrollmentProfileSnapshotCsvRepository(),
                AuditLogService.noop());
    }

    public EnrollmentService(UserCsvRepository userRepository,
                             PositionCsvRepository positionRepository,
                             ApplicantProfileCsvRepository profileRepository,
                             ResumeUploadService resumeUploadService,
                             TAApplicationCsvRepository applicationRepository,
                             ApplicationStatusHistoryCsvRepository historyRepository,
                             ApplicationPreferenceCsvRepository preferenceRepository) {
        this(userRepository,
                positionRepository,
                profileRepository,
                resumeUploadService,
                applicationRepository,
                historyRepository,
                preferenceRepository,
                new EnrollmentProfileSnapshotCsvRepository(),
                AuditLogService.noop());
    }

    public EnrollmentService(UserCsvRepository userRepository,
                             PositionCsvRepository positionRepository,
                             ApplicantProfileCsvRepository profileRepository,
                             ResumeUploadService resumeUploadService,
                             TAApplicationCsvRepository applicationRepository,
                             ApplicationStatusHistoryCsvRepository historyRepository,
                             ApplicationPreferenceCsvRepository preferenceRepository,
                             EnrollmentProfileSnapshotCsvRepository snapshotRepository,
                             AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
        this.profileRepository = profileRepository;
        this.resumeUploadService = resumeUploadService;
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.preferenceRepository = preferenceRepository;
        this.snapshotRepository = snapshotRepository;
        this.auditLogService = auditLogService;
    }

    public void submit(EnrollmentSubmission submission) {
        validateSubmission(submission);
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Set<String> normalizedPositionIds = new LinkedHashSet<>();
        for (String positionId : submission.positionIds()) {
            if (positionId != null && !positionId.isBlank()) {
                normalizedPositionIds.add(positionId.trim());
            }
        }
        Map<String, TAPosition> positionsById = positionsById(normalizedPositionIds);
        String semesterId = resolveSemesterId(positionsById, normalizedPositionIds);

        ApplicantProfile profile = profileRepository.saveOrUpdate(new ApplicantProfile(
                submission.userId().trim(),
                semesterId,
                safe(submission.phone()),
                safe(submission.major()),
                safe(submission.yearOfStudy()),
                safe(submission.gpa()),
                safe(submission.skills()),
                safe(submission.availability()),
                safe(submission.notes()),
                now
        ));

        List<String> existingApplicationIds = applicationRepository.findByUserIdAndSemesterId(submission.userId().trim(), semesterId).stream()
                .map(TAApplication::applicationId)
                .toList();
        applicationRepository.deleteByUserIdAndSemesterId(submission.userId().trim(), semesterId);
        for (String applicationId : existingApplicationIds) {
            historyRepository.deleteByApplicationId(applicationId);
        }
        preferenceRepository.saveForApplication(
                preferenceBundleId(submission.userId().trim(), semesterId),
                normalizedPositionIds.stream()
                        .map(positionId -> positionsById.get(positionId))
                        .filter(java.util.Objects::nonNull)
                        .map(TAPosition::courseId)
                        .toList()
        );

        List<String> applicationIds = new ArrayList<>();
        int priority = 1;
        for (String positionId : normalizedPositionIds) {
            String applicationId = toApplicationId(submission.userId(), positionId);
            applicationIds.add(applicationId);
            TAApplication application = new TAApplication(
                    applicationId,
                    submission.userId().trim(),
                    positionId,
                    semesterId,
                    priority++,
                    ApplicationStatus.PENDING_REVIEW,
                    "",
                    now,
                    now
            );
            applicationRepository.saveOrUpdate(application);
            historyRepository.save(applicationId, ApplicationStatus.PENDING_REVIEW, "Application submitted", submission.userId(), now);
        }

        resumeUploadService.uploadResumeForApplications(submission.userId(), applicationIds, submission.resumeSourceFile());
        snapshotRepository.save(new EnrollmentProfileSnapshot(
                snapshotRepository.nextSnapshotId(),
                submission.userId().trim(),
                semesterId,
                profile.phone(),
                profile.major(),
                profile.yearOfStudy(),
                profile.gpa(),
                profile.skills(),
                profile.availability(),
                profile.notes(),
                String.join("; ", normalizedPositionIds),
                applicationIds.isEmpty() ? "" : resumeUploadService.findResumeForApplication(applicationIds.get(0)).map(item -> item.filePath()).orElse(""),
                applicationIds.isEmpty() ? "" : resumeUploadService.findResumeForApplication(applicationIds.get(0)).map(item -> item.autoFilename()).orElse(""),
                now
        ));
        auditLogService.record("ENROLLMENT_SUBMITTED", submission.userId(),
                "Submitted TA application package for positions " + String.join("; ", normalizedPositionIds));
    }

    private String preferenceBundleId(String userId, String semesterId) {
        return "APP-" + userId + "-" + safe(semesterId).toUpperCase();
    }

    private void validateSubmission(EnrollmentSubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission is required");
        }
        if (submission.userId() == null || submission.userId().isBlank()) {
            throw new IllegalArgumentException("A logged-in TA user is required");
        }
        User applicant = userRepository.findAll().stream()
                .filter(item -> item.userId().equals(submission.userId().trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + submission.userId()));
        if (applicant.taCategory() == TACategory.NON_MODULAR) {
            throw new IllegalArgumentException("Non-modular TAs cannot apply for formal TA positions");
        }
        if (submission.major() == null || submission.major().isBlank()) {
            throw new IllegalArgumentException("Major is required");
        }
        if (submission.positionIds() == null || submission.positionIds().isEmpty()) {
            throw new IllegalArgumentException("Select at least one position");
        }

        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String positionId : submission.positionIds()) {
            if (positionId != null && !positionId.isBlank()) {
                uniqueIds.add(positionId.trim());
            }
        }
        if (uniqueIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one valid position");
        }
        if (uniqueIds.size() > MAX_SELECTION) {
            throw new IllegalArgumentException("You can select up to 3 positions only");
        }

        Map<String, TAPosition> positionsById = positionsById(uniqueIds);
        for (String positionId : uniqueIds) {
            TAPosition position = positionsById.get(positionId);
            if (!"PUBLISHED".equalsIgnoreCase(position.status())) {
                throw new IllegalArgumentException("Position is not open for application: " + positionId);
            }
            if (position.deadline() != null && !position.deadline().isBlank()) {
                LocalDate deadline;
                try {
                    deadline = LocalDate.parse(position.deadline());
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Position " + positionId
                            + " has an invalid deadline. Ask the MO to use YYYY-MM-DD format, for example 2026-04-15");
                }
                if (deadline.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Position deadline has passed: " + positionId);
                }
            }
        }
    }

    private String toApplicationId(String userId, String positionId) {
        return "APP-" + userId.trim() + "-" + positionId.trim();
    }

    private String resolveSemesterId(Map<String, TAPosition> positionsById, Set<String> positionIds) {
        for (String positionId : positionIds) {
            TAPosition position = positionsById.get(positionId);
            if (position != null && position.semesterId() != null && !position.semesterId().isBlank()) {
                return position.semesterId().trim();
            }
        }
        return "";
    }

    private Map<String, TAPosition> positionsById(Set<String> positionIds) {
        Map<String, TAPosition> positionsById = positionRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(TAPosition::positionId, position -> position, (left, right) -> left));
        for (String positionId : positionIds) {
            if (!positionsById.containsKey(positionId)) {
                throw new IllegalArgumentException("Position not found: " + positionId);
            }
        }
        return positionsById;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
