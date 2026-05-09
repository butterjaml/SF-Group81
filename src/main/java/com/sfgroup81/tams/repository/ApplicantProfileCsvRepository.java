package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.ApplicantProfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApplicantProfileCsvRepository {
    private static final String HEADER = "user_id,semester_id,phone,major,year_of_study,gpa,skills,availability,notes,updated_at";
    private final Path profileCsv;

    public ApplicantProfileCsvRepository() {
        this(Path.of("data"));
    }

    public ApplicantProfileCsvRepository(Path dataDir) {
        this.profileCsv = dataDir.resolve("applicant_profiles.csv");
    }

    public List<ApplicantProfile> findAll() {
        try {
            if (Files.notExists(profileCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(profileCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<ApplicantProfile> profiles = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 9) {
                    continue;
                }
                if (cols.length >= 10) {
                    profiles.add(new ApplicantProfile(
                            cols[0], cols[1], cols[2], cols[3], cols[4], cols[5], cols[6], cols[7], cols[8], cols[9]
                    ));
                } else {
                    profiles.add(new ApplicantProfile(
                            cols[0], "", cols[1], cols[2], cols[3], cols[4], cols[5], cols[6], cols[7], cols[8]
                    ));
                }
            }
            return profiles;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read applicant_profiles.csv", ex);
        }
    }

    public Optional<ApplicantProfile> findByUserId(String userId) {
        return findAll().stream()
                .filter(profile -> profile.userId().equals(userId))
                .max(java.util.Comparator.comparing(ApplicantProfile::updatedAt));
    }

    public Optional<ApplicantProfile> findByUserIdAndSemesterId(String userId, String semesterId) {
        String normalizedSemesterId = safe(semesterId);
        return findAll().stream()
                .filter(profile -> profile.userId().equals(userId))
                .filter(profile -> safe(profile.semesterId()).equalsIgnoreCase(normalizedSemesterId))
                .max(java.util.Comparator.comparing(ApplicantProfile::updatedAt));
    }

    public ApplicantProfile saveOrUpdate(ApplicantProfile profile) {
        List<ApplicantProfile> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.userId().equals(profile.userId())
                && safe(item.semesterId()).equalsIgnoreCase(safe(profile.semesterId())));
        all.add(profile);
        rewriteAll(all);
        return profile;
    }

    private void rewriteAll(List<ApplicantProfile> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (ApplicantProfile item : rows) {
            lines.add(String.join(",",
                    sanitize(item.userId()),
                    sanitize(item.semesterId()),
                    sanitize(item.phone()),
                    sanitize(item.major()),
                    sanitize(item.yearOfStudy()),
                    sanitize(item.gpa()),
                    sanitize(item.skills()),
                    sanitize(item.availability()),
                    sanitize(item.notes()),
                    sanitize(item.updatedAt())
            ));
        }
        try {
            Files.write(profileCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write applicant_profiles.csv", ex);
        }
    }

    private String sanitize(String value) {
        return safe(value).replace(",", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
