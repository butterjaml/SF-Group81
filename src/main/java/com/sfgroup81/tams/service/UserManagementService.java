package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.util.List;
import java.util.Locale;





public class UserManagementService {
    private final UserCsvRepository userRepository;
    private final AuditLogService auditLogService;
    private final TAApplicationCsvRepository taApplicationRepository;
    private final PositionCsvRepository positionRepository;

    public UserManagementService(UserCsvRepository userRepository) {
        this(userRepository, AuditLogService.noop());
    }

    public UserManagementService(UserCsvRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.taApplicationRepository = new TAApplicationCsvRepository();
        this.positionRepository = new PositionCsvRepository();
    }

    // ... 保留原有的其他方法 (listUsers, createUser 等) ...

    public List<User> listUsers(String keyword) {
        String normalized = safe(keyword).toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(user -> normalized.isBlank()
                        || user.userId().toLowerCase(Locale.ROOT).contains(normalized)
                        || user.name().toLowerCase(Locale.ROOT).contains(normalized)
                        || user.email().toLowerCase(Locale.ROOT).contains(normalized)
                        || user.staffOrStudentId().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(java.util.Comparator.comparing(User::userId))
                .toList();
    }

    public User createUser(UserUpsertRequest request, String operatorUserId) {
        validateNewUser(request, null);
        TACategory category = normalizeCategory(request.role(), request.taCategory());
        User user = userRepository.saveNewUser(
                safe(request.name()),
                safe(request.staffOrStudentId()),
                safe(request.email()),
                SecurityUtil.sha256(request.password()),
                request.role(),
                category
        );
        auditLogService.record("USER_CREATED", operatorUserId,
                "Created user " + user.userDisplay() + " with role " + user.role());
        return user;
    }

    public User updateUserRoleAndCategory(String userId, UserRole role, TACategory taCategory, String operatorUserId) {
        User existing = requireUser(userId);
        TACategory normalizedCategory = normalizeCategory(role, taCategory);
        User updated = new User(
                existing.userId(),
                existing.name(),
                existing.staffOrStudentId(),
                existing.email(),
                existing.passwordHash(),
                role,
                normalizedCategory,
                existing.status(),
                existing.lastLoginAt()
        );
        userRepository.saveOrUpdate(updated);
        auditLogService.record("ROLE_CHANGED", operatorUserId,
                "Updated role for " + updated.userDisplay() + " to " + updated.role() + " / " + updated.taCategory());
        return updated;
    }

    public User updateAccountStatus(String userId, boolean active, String operatorUserId) {
        User existing = requireUser(userId);
        User updated = new User(
                existing.userId(),
                existing.name(),
                existing.staffOrStudentId(),
                existing.email(),
                existing.passwordHash(),
                existing.role(),
                existing.taCategory(),
                active ? "ACTIVE" : "DISABLED",
                existing.lastLoginAt()
        );
        userRepository.saveOrUpdate(updated);
        auditLogService.record("ACCOUNT_STATUS_CHANGED", operatorUserId,
                "Set account " + updated.userDisplay() + " to " + updated.status());
        return updated;
    }

    public User resetPassword(String userId, String newPassword, String operatorUserId) {
        User existing = requireUser(userId);
        if (safe(newPassword).length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        User updated = new User(
                existing.userId(),
                existing.name(),
                existing.staffOrStudentId(),
                existing.email(),
                SecurityUtil.sha256(newPassword),
                existing.role(),
                existing.taCategory(),
                existing.status(),
                existing.lastLoginAt()
        );
        userRepository.saveOrUpdate(updated);
        auditLogService.record("PASSWORD_RESET", operatorUserId,
                "Reset password for " + updated.userDisplay());
        return updated;
    }

    private void validateNewUser(UserUpsertRequest request, String userId) {
        if (request == null) {
            throw new IllegalArgumentException("User request is required");
        }
        if (safe(request.name()).isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (safe(request.staffOrStudentId()).isBlank()) {
            throw new IllegalArgumentException("Staff/Student ID is required");
        }
        if (safe(request.email()).isBlank() || !safe(request.email()).contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (request.role() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (safe(request.password()).length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        userRepository.findByEmail(request.email())
                .filter(user -> userId == null || !user.userId().equals(userId))
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Email already exists");
                });
        userRepository.findByStaffOrStudentId(request.staffOrStudentId())
                .filter(user -> userId == null || !user.userId().equals(userId))
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Staff/Student ID already exists");
                });
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private TACategory normalizeCategory(UserRole role, TACategory taCategory) {
        if (role != UserRole.TA) {
            return TACategory.NONE;
        }
        return taCategory == null || taCategory == TACategory.NONE ? TACategory.MODULAR : taCategory;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public int calculateTAWorkload(String userId) {
        int totalHours = 0;
        List<TAApplication> applications = taApplicationRepository.findByUserId(userId);
        for (TAApplication app : applications) {
            // 只统计已经被雇佣 (HIRED) 的职位
            if (app.status() == ApplicationStatus.HIRED) {
                TAPosition pos = positionRepository.findById(app.positionId()).orElse(null);
                if (pos != null) {
                    totalHours += extractHours(pos.workingHours());
                }
            }
        }
        return totalHours;
    }

    // 辅助方法：从类似 "10 hours/week" 的字符串中提取数字
    private int extractHours(String workingHoursStr) {
        if (workingHoursStr == null || workingHoursStr.isBlank()) {
            return 0;
        }
        Matcher m = Pattern.compile("\\d+").matcher(workingHoursStr);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}

