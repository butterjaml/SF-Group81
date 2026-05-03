package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ResumeFileRecord;

import java.util.List;
import java.util.Optional;

public record EnrollmentAutofillSnapshot(
        String semesterId,
        Optional<ApplicantProfile> profile,
        List<String> positionIds,
        Optional<ResumeFileRecord> resume
) {
    public boolean hasAnyData() {
        return profile.isPresent() || !positionIds.isEmpty() || resume.isPresent();
    }
}
