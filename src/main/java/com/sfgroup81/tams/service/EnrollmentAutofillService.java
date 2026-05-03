package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.EnrollmentProfileSnapshot;
import com.sfgroup81.tams.model.ResumeFileRecord;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.EnrollmentProfileSnapshotCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EnrollmentAutofillService {
    private final ApplicantProfileCsvRepository profileRepository;
    private final TAApplicationCsvRepository applicationRepository;
    private final ResumeFileCsvRepository resumeRepository;
    private final EnrollmentProfileSnapshotCsvRepository snapshotRepository;

    public EnrollmentAutofillService(ApplicantProfileCsvRepository profileRepository,
                                     TAApplicationCsvRepository applicationRepository,
                                     ResumeFileCsvRepository resumeRepository) {
        this(profileRepository, applicationRepository, resumeRepository, new EnrollmentProfileSnapshotCsvRepository());
    }

    public EnrollmentAutofillService(ApplicantProfileCsvRepository profileRepository,
                                     TAApplicationCsvRepository applicationRepository,
                                     ResumeFileCsvRepository resumeRepository,
                                     EnrollmentProfileSnapshotCsvRepository snapshotRepository) {
        this.profileRepository = profileRepository;
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public EnrollmentAutofillSnapshot loadLatestForUser(String userId, String targetSemesterId) {
        Optional<EnrollmentProfileSnapshot> priorSnapshot = findLatestPriorSnapshot(userId, targetSemesterId);
        if (priorSnapshot.isPresent()) {
            EnrollmentProfileSnapshot snapshot = priorSnapshot.get();
            ApplicantProfile profile = new ApplicantProfile(
                    snapshot.userId(),
                    snapshot.phone(),
                    snapshot.major(),
                    snapshot.yearOfStudy(),
                    snapshot.gpa(),
                    snapshot.skills(),
                    snapshot.availability(),
                    snapshot.notes(),
                    snapshot.savedAt()
            );
            Optional<ResumeFileRecord> resume = snapshot.resumeAutoFilename().isBlank() && snapshot.resumeFilePath().isBlank()
                    ? Optional.empty()
                    : Optional.of(new ResumeFileRecord(
                    "SNAPSHOT-" + snapshot.snapshotId(),
                    "",
                    snapshot.resumeFilePath(),
                    "",
                    snapshot.resumeAutoFilename(),
                    snapshot.savedAt(),
                    snapshot.savedAt()
            ));
            return new EnrollmentAutofillSnapshot(
                    snapshot.semesterId(),
                    Optional.of(profile),
                    snapshot.positionIdList(),
                    resume
            );
        }

        List<TAApplication> applications = applicationRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(TAApplication::updatedAt).reversed())
                .toList();
        Optional<ResumeFileRecord> resume = applications.stream()
                .map(TAApplication::applicationId)
                .map(resumeRepository::findByApplicationId)
                .flatMap(Optional::stream)
                .findFirst();
        return new EnrollmentAutofillSnapshot(
                "",
                profileRepository.findByUserId(userId),
                applications.stream().map(TAApplication::positionId).distinct().toList(),
                resume
        );
    }

    public EnrollmentProfileSnapshot saveSnapshot(String userId,
                                                  String semesterId,
                                                  ApplicantProfile profile,
                                                  List<String> positionIds,
                                                  ResumeFileRecord resume) {
        String joinedPositionIds = String.join("; ", positionIds == null ? List.of() : new ArrayList<>(positionIds));
        EnrollmentProfileSnapshot snapshot = new EnrollmentProfileSnapshot(
                snapshotRepository.nextSnapshotId(),
                safe(userId),
                safe(semesterId),
                profile == null ? "" : safe(profile.phone()),
                profile == null ? "" : safe(profile.major()),
                profile == null ? "" : safe(profile.yearOfStudy()),
                profile == null ? "" : safe(profile.gpa()),
                profile == null ? "" : safe(profile.skills()),
                profile == null ? "" : safe(profile.availability()),
                profile == null ? "" : safe(profile.notes()),
                joinedPositionIds,
                resume == null ? "" : safe(resume.filePath()),
                resume == null ? "" : safe(resume.autoFilename()),
                LocalDateTime.now().toString()
        );
        return snapshotRepository.save(snapshot);
    }

    private Optional<EnrollmentProfileSnapshot> findLatestPriorSnapshot(String userId, String targetSemesterId) {
        return snapshotRepository.findAll().stream()
                .filter(snapshot -> snapshot.userId().equals(userId))
                .filter(snapshot -> targetSemesterId == null || targetSemesterId.isBlank() || compareSemester(snapshot.semesterId(), targetSemesterId) < 0)
                .max(Comparator.comparing(EnrollmentProfileSnapshot::savedAt));
    }

    private int compareSemester(String left, String right) {
        return semesterRank(left) - semesterRank(right);
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
