package com.sfgroup81.tams.service;

import java.util.List;

public record CandidateScreeningView(
        CandidateReviewView candidate,
        double recommendationScore,
        double gpaValue,
        boolean hasPastTaExperience,
        double skillMatchScore,
        List<String> matchedRequirementKeywords
) {
}
