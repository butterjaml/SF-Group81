package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.UserCsvRepository;

public class RegistrationService {
    private final UserCsvRepository repository;

    public RegistrationService(UserCsvRepository repository) {
        this.repository = repository;
    }

    public User register(RegistrationRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.staffOrStudentId() == null || request.staffOrStudentId().isBlank()) {
            throw new IllegalArgumentException("Staff/Student ID is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        return repository.saveNewUser(
                request.name(),
                request.staffOrStudentId(),
                request.email(),
                request.password(),
                request.role()
        );
    }
}
