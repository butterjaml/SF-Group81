package com.sfgroup81.tams.service;

public record CandidateFilterCriteria(
        String nameKeyword,
        String studentIdKeyword,
        Double minGpa,
        Double maxGpa,
        String skillKeyword,
        String yearOfStudy,
        String availabilityKeyword,
        boolean internallyRecommendedOnly,
        boolean experiencedOnly
) {
}
