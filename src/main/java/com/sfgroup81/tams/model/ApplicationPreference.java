package com.sfgroup81.tams.model;

public record ApplicationPreference(
        String preferenceId,
        String applicationId,
        String courseId,
        int priorityNo
) {
}
