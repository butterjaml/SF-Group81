package com.sfgroup81.tams.model;

public record User(
        String userId,
        String name,
        String staffOrStudentId,
        String email,
        String passwordHash,
        UserRole role,
        TACategory taCategory,
        String status,
        String lastLoginAt
) {
    public String userDisplay() {
        return name + " (" + userId + ")";
    }
}
