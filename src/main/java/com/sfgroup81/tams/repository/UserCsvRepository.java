package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.UserRole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class UserCsvRepository {
    private static final String HEADER = "user_id,name,staff_or_student_id,email,password_hash,role,ta_category,status,last_login_at";
    private final Path usersCsv;

    public UserCsvRepository() {
        this(Path.of("data"));
    }

    public UserCsvRepository(Path dataDir) {
        this.usersCsv = dataDir.resolve("users.csv");
    }

    public List<User> findAll() {
        try {
            if (Files.notExists(usersCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(usersCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<User> users = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < 8) {
                    continue;
                }
                UserRole role = UserRole.valueOf(cols[5]);
                TACategory taCategory = parseTaCategory(cols, role);
                users.add(new User(
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        cols[4],
                        role,
                        taCategory,
                        cols.length >= 9 ? cols[7] : cols[6],
                        cols.length >= 9 ? cols[8] : cols[7]
                ));
            }
            return users;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read users.csv", ex);
        }
    }

    public Optional<User> findByEmail(String email) {
        return findAll().stream()
                .filter(user -> user.email().equalsIgnoreCase(email == null ? "" : email.trim()))
                .findFirst();
    }

    public Optional<User> findById(String userId) {
        return findAll().stream()
                .filter(user -> user.userId().equals(userId == null ? "" : userId.trim()))
                .findFirst();
    }

    public Optional<User> findByStaffOrStudentId(String staffOrStudentId) {
        return findAll().stream()
                .filter(user -> user.staffOrStudentId().equalsIgnoreCase(staffOrStudentId == null ? "" : staffOrStudentId.trim()))
                .findFirst();
    }

    public User saveNewUser(String name, String staffOrStudentId, String email, String passwordHash, UserRole role) {
        return saveNewUser(name, staffOrStudentId, email, passwordHash, role, defaultCategory(role));
    }

    public User saveNewUser(String name,
                            String staffOrStudentId,
                            String email,
                            String passwordHash,
                            UserRole role,
                            TACategory taCategory) {
        User user = new User(
                nextUserId(),
                sanitize(name),
                sanitize(staffOrStudentId),
                sanitize(email),
                sanitize(passwordHash),
                role,
                normalizeCategory(role, taCategory),
                "ACTIVE",
                ""
        );
        return saveOrUpdate(user);
    }

    public User saveOrUpdate(User user) {
        List<User> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.userId().equals(user.userId()));
        all.add(user);
        rewriteAll(all);
        return user;
    }

    public void updateLastLoginAt(String userId) {
        try {
            if (Files.notExists(usersCsv)) {
                return;
            }
            List<String> lines = Files.readAllLines(usersCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return;
            }
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            List<User> updated = new ArrayList<>();
            for (User user : findAll()) {
                if (user.userId().equals(userId)) {
                    updated.add(new User(
                            user.userId(),
                            user.name(),
                            user.staffOrStudentId(),
                            user.email(),
                            user.passwordHash(),
                            user.role(),
                            user.taCategory(),
                            user.status(),
                            now
                    ));
                } else {
                    updated.add(user);
                }
            }
            rewriteAll(updated);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to update last_login_at in users.csv", ex);
        }
    }

    private void rewriteAll(List<User> users) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (User user : users) {
            lines.add(String.join(",",
                    sanitize(user.userId()),
                    sanitize(user.name()),
                    sanitize(user.staffOrStudentId()),
                    sanitize(user.email()),
                    sanitize(user.passwordHash()),
                    user.role().name(),
                    normalizeCategory(user.role(), user.taCategory()).name(),
                    sanitize(user.status()),
                    sanitize(user.lastLoginAt())
            ));
        }
        try {
            Files.write(usersCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write users.csv", ex);
        }
    }

    private String nextUserId() {
        return findAll().stream()
                .map(User::userId)
                .filter(id -> id.startsWith("U"))
                .map(id -> id.substring(1))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("U%04d", max + 1))
                .orElse("U0001");
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }

    private TACategory parseTaCategory(String[] cols, UserRole role) {
        if (cols.length >= 9 && !cols[6].isBlank()) {
            return normalizeCategory(role, TACategory.valueOf(cols[6]));
        }
        return defaultCategory(role);
    }

    private TACategory defaultCategory(UserRole role) {
        return role == UserRole.TA ? TACategory.MODULAR : TACategory.NONE;
    }

    private TACategory normalizeCategory(UserRole role, TACategory taCategory) {
        if (role != UserRole.TA) {
            return TACategory.NONE;
        }
        return taCategory == null || taCategory == TACategory.NONE ? TACategory.MODULAR : taCategory;
    }
}
