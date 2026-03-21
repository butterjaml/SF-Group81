package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.CourseOption;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CourseSelectionService {
    private final PositionCsvRepository positionRepository;
    private final ApplicationPreferenceCsvRepository preferenceRepository;

    public CourseSelectionService(PositionCsvRepository positionRepository,
                                  ApplicationPreferenceCsvRepository preferenceRepository) {
        this.positionRepository = positionRepository;
        this.preferenceRepository = preferenceRepository;
    }

    public List<CourseOption> listAvailableCourses() {
        Map<String, String> courseMap = new LinkedHashMap<>();
        for (TAPosition position : positionRepository.findAll()) {
            if (!"PUBLISHED".equalsIgnoreCase(position.status())) {
                continue;
            }
            String title = position.title() == null || position.title().isBlank()
                    ? "Untitled Position"
                    : position.title().trim();
            courseMap.putIfAbsent(position.courseId(), title);
        }

        List<CourseOption> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : courseMap.entrySet()) {
            options.add(new CourseOption(entry.getKey(), entry.getValue()));
        }
        return options;
    }

    public List<String> getSelectedCourseIds(String userId) {
        validateUser(userId);
        return preferenceRepository.findByApplicationId(toApplicationId(userId))
                .stream()
                .map(item -> item.courseId())
                .toList();
    }

    public void saveCoursePreferences(String userId, List<String> selectedCourseIds) {
        validateUser(userId);
        if (selectedCourseIds == null || selectedCourseIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one course");
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String courseId : selectedCourseIds) {
            if (courseId != null && !courseId.isBlank()) {
                normalized.add(courseId.trim());
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("No valid course selection found");
        }
        preferenceRepository.saveForApplication(toApplicationId(userId), new ArrayList<>(normalized));
    }

    private void validateUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("A logged-in TA user is required");
        }
    }

    private String toApplicationId(String userId) {
        return "APP-" + userId.trim();
    }
}
