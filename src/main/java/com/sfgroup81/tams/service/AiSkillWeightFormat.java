package com.sfgroup81.tams.service;

import java.util.ArrayList;
import java.util.List;

public final class AiSkillWeightFormat {
    public static final String DEFAULT_VALUE = "Mandatory requirements=50; Preferred requirements=30; Bonus requirements=20";
    public static final String EXAMPLE = "Java=40; Teaching=35; Communication=25";

    private AiSkillWeightFormat() {
    }

    public static String normalizeOrDefault(String value) {
        String normalized = safe(value);
        return normalized.isBlank() ? DEFAULT_VALUE : normalized;
    }

    public static void validate(String value) {
        String normalized = normalizeOrDefault(value);
        List<String> invalidParts = new ArrayList<>();
        int total = 0;
        for (String part : normalized.split(";")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            int separator = token.lastIndexOf('=');
            if (separator <= 0 || separator == token.length() - 1) {
                invalidParts.add(token);
                continue;
            }
            String skill = token.substring(0, separator).trim();
            String weightText = token.substring(separator + 1).trim();
            if (skill.isBlank()) {
                invalidParts.add(token);
                continue;
            }
            try {
                int weight = Integer.parseInt(weightText);
                if (weight <= 0 || weight > 100) {
                    invalidParts.add(token);
                } else {
                    total += weight;
                }
            } catch (NumberFormatException ex) {
                invalidParts.add(token);
            }
        }
        if (!invalidParts.isEmpty() || total == 0) {
            throw new IllegalArgumentException("AI skill weights must use Skill=Weight pairs separated by semicolons, for example: " + EXAMPLE);
        }
        if (total != 100) {
            throw new IllegalArgumentException("AI skill weights must add up to 100. Example: " + EXAMPLE);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
