package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistrationService {
    private static final Logger LOGGER = Logger.getLogger(RegistrationService.class.getName());

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
        if (!request.email().contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (repository.findByEmail(request.email()).isPresent()) {
            LOGGER.log(Level.WARNING, "Registration blocked due to duplicate email: {0}", request.email());
            throw new IllegalArgumentException("Email already exists");
        }
        if (repository.findByStaffOrStudentId(request.staffOrStudentId()).isPresent()) {
            LOGGER.log(Level.WARNING, "Registration blocked due to duplicate ID: {0}", request.staffOrStudentId());
            throw new IllegalArgumentException("Staff/Student ID already exists");
        }

        String hashedPassword = SecurityUtil.sha256(request.password());
        TACategory taCategory = request.role() == UserRole.TA
                ? (request.taCategory() == null || request.taCategory() == TACategory.NONE ? TACategory.MODULAR : request.taCategory())
                : TACategory.NONE;
        User user = repository.saveNewUser(
                request.name(),
                request.staffOrStudentId(),
                request.email(),
                hashedPassword,
                request.role(),
                taCategory
        );
        LOGGER.log(Level.INFO, "Registration success for user: {0}", user.userId());
        return user;
    }
}
