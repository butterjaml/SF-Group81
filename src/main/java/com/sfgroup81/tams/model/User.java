package com.sfgroup81.tams.model;

public record User(
        String userId,
        String name,
        String staffOrStudentId,
        String email,
        String passwordHash,
        UserRole role,
        String status,
        String lastLoginAt
) {
}
