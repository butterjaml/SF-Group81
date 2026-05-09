package com.sfgroup81.tams.model;

import java.util.List;

public record AiScreeningResult(
        String resultId,
        String positionId,
        String applicationId,
        String semesterId,
        String modelName,
        double matchScore,
        String matchedSkills,
        String missingSkills,
        String summary,
        String strengths,
        String risks,
        String promptHash,
        String generatedAt
) {
    public List<String> matchedSkillList() {
        return splitValues(matchedSkills);
    }

    public List<String> missingSkillList() {
        return splitValues(missingSkills);
    }

    private List<String> splitValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("\\s*;\\s*"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
