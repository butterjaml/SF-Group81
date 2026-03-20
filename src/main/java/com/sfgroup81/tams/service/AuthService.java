package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.UserCsvRepository;

public class AuthService {
    private final UserCsvRepository userCsvRepository;

    public AuthService(UserCsvRepository userCsvRepository) {
        this.userCsvRepository = userCsvRepository;
    }

    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }

        User user = userCsvRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String hashedPassword = SecurityUtil.sha256(password);
        if (!hashedPassword.equals(user.passwordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new IllegalArgumentException("User is not active");
        }

        userCsvRepository.updateLastLoginAt(user.userId());
        SessionContext.setCurrentUser(user);
        return user;
    }
}
