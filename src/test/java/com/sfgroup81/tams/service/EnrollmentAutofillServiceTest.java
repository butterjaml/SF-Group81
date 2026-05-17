package com.sfgroup81.tams.service;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.EnrollmentProfileSnapshotCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrollmentAutofillServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldPreferPreviousSemesterSnapshotOverCurrentApplicationRows() throws Exception {
        DataBootstrap.initialize(tempDir);
        ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository(tempDir);
        EnrollmentProfileSnapshotCsvRepository snapshotRepository = new EnrollmentProfileSnapshotCsvRepository(tempDir);

        profileRepository.saveOrUpdate(new ApplicantProfile(
                "U0004",
                "18800001111",
                "Current Major",
                "Year 4",
                "3.96",
                "Current Skills",
                "Weekday afternoons",
                "Current semester data",
                "2026-05-01T10:00:00"
        ));
        applicationRepository.saveOrUpdate(new TAApplication(
                "APP-U0004-P0001",
                "U0004",
                "P0001",
                1,
                ApplicationStatus.PENDING_REVIEW,
                "",
                "2026-05-01T09:00:00",
                "2026-05-01T09:00:00"
        ));
        Path resumeFile = tempDir.resolve("resume.pdf");
        Files.writeString(resumeFile, "resume");
        resumeRepository.saveOrUpdate("APP-U0004-P0001", resumeFile.toString(), "PDF", "resume.pdf");

        snapshotRepository.save(new com.sfgroup81.tams.model.EnrollmentProfileSnapshot(
                snapshotRepository.nextSnapshotId(),
                "U0004",
                "2025S2",
                "17777777777",
                "Computer Science",
                "Year 3",
                "3.86",
                "Java; Selenium",
                "Weekday afternoons",
                "Interested in quality engineering",
                "P0099; P0100",
                resumeFile.toString(),
                "resume.pdf",
                "2026-01-10T09:00:00"
        ));

        EnrollmentAutofillService service = new EnrollmentAutofillService(profileRepository, applicationRepository, resumeRepository, snapshotRepository);
        EnrollmentAutofillSnapshot snapshot = service.loadLatestForUser("U0004", "2026S1");

        assertTrue(snapshot.profile().isPresent());
        assertEquals("Computer Science", snapshot.profile().orElseThrow().major());
        assertEquals("2025S2", snapshot.semesterId());
        assertEquals(List.of("P0099", "P0100"), snapshot.positionIds());
        assertTrue(snapshot.resume().isPresent());
    }

    @Test
    void shouldAutofillCurrentSemesterProfileWhenApplyingAgain() throws Exception {
        DataBootstrap.initialize(tempDir);
        ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository(tempDir);
        EnrollmentProfileSnapshotCsvRepository snapshotRepository = new EnrollmentProfileSnapshotCsvRepository(tempDir);

        profileRepository.saveOrUpdate(new ApplicantProfile(
                "U0004",
                "2026S1",
                "18800001111",
                "Computer Science",
                "Year 3",
                "3.86",
                "Java; Selenium",
                "Weekday afternoons",
                "Current semester saved profile",
                "2026-05-01T10:00:00"
        ));
        applicationRepository.saveOrUpdate(new TAApplication(
                "APP-U0004-P0001",
                "U0004",
                "P0001",
                "2026S1",
                1,
                ApplicationStatus.PENDING_REVIEW,
                "",
                "2026-05-01T09:00:00",
                "2026-05-01T09:00:00"
        ));

        EnrollmentAutofillService service = new EnrollmentAutofillService(profileRepository, applicationRepository, resumeRepository, snapshotRepository);
        EnrollmentAutofillSnapshot snapshot = service.loadLatestForUser("U0004", "2026S1");

        assertTrue(snapshot.profile().isPresent());
        assertEquals("Current semester saved profile", snapshot.profile().orElseThrow().notes());
        assertEquals(List.of("P0001"), snapshot.positionIds());
    }
}
