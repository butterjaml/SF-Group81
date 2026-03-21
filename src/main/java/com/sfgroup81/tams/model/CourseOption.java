package com.sfgroup81.tams.model;

public record CourseOption(
        String courseId,
        String displayTitle
) {
    @Override
    public String toString() {
        return courseId + " - " + displayTitle;
    }
}
