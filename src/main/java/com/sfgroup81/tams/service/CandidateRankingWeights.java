package com.sfgroup81.tams.service;

public record CandidateRankingWeights(
        int gpaWeight,
        int experienceWeight,
        int skillWeight,
        int referralWeight,
        int reputationWeight
) {
    public static CandidateRankingWeights defaults() {
        return new CandidateRankingWeights(30, 25, 20, 10, 15);
    }

    public int totalWeight() {
        return gpaWeight + experienceWeight + skillWeight + referralWeight + reputationWeight;
    }
}
