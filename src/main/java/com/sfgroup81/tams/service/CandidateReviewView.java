package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.InternalReferral;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.User;

import java.util.Optional;

public record CandidateReviewView(
        TAApplication application,
        User user,
        ApplicantProfile profile,
        Optional<InternalReferral> referral,
        double reputationScore
) {
}
