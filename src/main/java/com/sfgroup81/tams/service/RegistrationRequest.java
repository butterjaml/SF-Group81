package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.UserRole;

public record RegistrationRequest(
        String name,
        String staffOrStudentId,
        String email,
        String password,
        UserRole role
) {
}
