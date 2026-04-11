package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
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
                new ApplicationPreferenceCsvRepository());
    }

    public EnrollmentService(UserCsvRepository userRepository,
                             PositionCsvRepository positionRepository,
                             ApplicantProfileCsvRepository profileRepository,
                             ResumeUploadService resumeUploadService,
                             TAApplicationCsvRepository applicationRepository,
                             ApplicationStatusHistoryCsvRepository historyRepository,
                             ApplicationPreferenceCsvRepository preferenceRepository) {
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
        this.profileRepository = profileRepository;
        this.resumeUploadService = resumeUploadService;
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.preferenceRepository = preferenceRepository;
    }

    // 执行完整的申请提交流程，包括校验、保存个人资料、删除旧申请、创建新申请、上传简历等
    public void submit(EnrollmentSubmission submission) {
        validateSubmission(submission);
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // 保存或更新申请者个人资料
        profileRepository.saveOrUpdate(new ApplicantProfile(
                submission.userId().trim(),
                safe(submission.phone()),
                safe(submission.major()),
                safe(submission.yearOfStudy()),
                safe(submission.gpa()),
                safe(submission.skills()),
                safe(submission.availability()),
                safe(submission.notes()),
                now
        ));
        
        // 处理职位ID列表（去重、去空、保持顺序）
        Set<String> normalizedPositionIds = new LinkedHashSet<>();
        for (String positionId : submission.positionIds()) {
            if (positionId != null && !positionId.isBlank()) {
                normalizedPositionIds.add(positionId.trim());
            }
        }
        Map<String, TAPosition> positionsById = positionsById(normalizedPositionIds);

        // 删除该用户原有的所有申请和历史记录（覆盖式提交）
        applicationRepository.deleteByUserId(submission.userId().trim());
        historyRepository.deleteByApplicationPrefix("APP-" + submission.userId().trim());
        // 记录该用户选择了哪些课程的职位，保存职位偏好
        preferenceRepository.saveForApplication(
                "APP-" + submission.userId().trim(),
                normalizedPositionIds.stream()
                        .map(positionId -> positionsById.get(positionId))
                        .filter(java.util.Objects::nonNull)
                        .map(TAPosition::courseId)
                        .toList()
        );

        // 为每个职位创建独立的申请和历史记录
        List<String> applicationIds = new ArrayList<>();
        int priority = 1;
        for (String positionId : normalizedPositionIds) {
            String applicationId = toApplicationId(submission.userId(), positionId);
            applicationIds.add(applicationId);
            TAApplication application = new TAApplication(
                    applicationId,
                    submission.userId().trim(),
                    positionId,
                    priority++,
                    ApplicationStatus.PENDING_REVIEW,
                    "",
                    now,
                    now
            );
            applicationRepository.saveOrUpdate(application);
            historyRepository.save(applicationId, ApplicationStatus.PENDING_REVIEW, "Application submitted", submission.userId(), now);
        }

        // 上传简历
        resumeUploadService.uploadResumeForApplications(submission.userId(), applicationIds, submission.resumeSourceFile());
    }

    private void validateSubmission(EnrollmentSubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission is required");
        }
        if (submission.userId() == null || submission.userId().isBlank()) {
            throw new IllegalArgumentException("A logged-in TA user is required");
        }
        userRepository.findAll().stream()
                .filter(user -> user.userId().equals(submission.userId().trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + submission.userId()));
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
        // 职位状态和截止日期校验
        Map<String, TAPosition> positionsById = positionsById(uniqueIds);
        for (String positionId : uniqueIds) {
            TAPosition position = positionsById.get(positionId);
            if (!"PUBLISHED".equalsIgnoreCase(position.status())) {
                throw new IllegalArgumentException("Position is not open for application: " + positionId);
            }
            if (position.deadline() != null && !position.deadline().isBlank()) {
                LocalDate deadline = LocalDate.parse(position.deadline());
                if (deadline.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Position deadline has passed: " + positionId);
                }
            }
        }
    }

    private String toApplicationId(String userId, String positionId) {
        return "APP-" + userId.trim() + "-" + positionId.trim();
    }
    // 从 positionRepository 读取所有职位，转换成 Map<String, TAPosition>（键为 positionId）
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
