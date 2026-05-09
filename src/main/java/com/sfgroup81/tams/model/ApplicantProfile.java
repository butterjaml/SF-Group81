package com.sfgroup81.tams.model;

public record ApplicantProfile(
        String userId,
        String semesterId,
        String phone,
        String major,
        String yearOfStudy,
        String gpa,
        String skills,
        String availability,
        String notes,
        String updatedAt
) {
    public ApplicantProfile(String userId,
                            String phone,
                            String major,
                            String yearOfStudy,
                            String gpa,
                            String skills,
                            String availability,
                            String notes,
                            String updatedAt) {
        this(userId, "", phone, major, yearOfStudy, gpa, skills, availability, notes, updatedAt);
    }
}
