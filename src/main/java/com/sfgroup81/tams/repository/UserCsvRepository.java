package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.User;
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
    private static final Path USERS_CSV = Path.of("data", "users.csv");

    public List<User> findAll() {
        try {
            if (Files.notExists(USERS_CSV)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(USERS_CSV, StandardCharsets.UTF_8);
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
                users.add(new User(
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        cols[4],
                        UserRole.valueOf(cols[5]),
                        cols[6],
                        cols[7]
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

    public Optional<User> findByStaffOrStudentId(String staffOrStudentId) {
        return findAll().stream()
                .filter(user -> user.staffOrStudentId().equalsIgnoreCase(staffOrStudentId == null ? "" : staffOrStudentId.trim()))
                .findFirst();
    }

    public User saveNewUser(String name, String staffOrStudentId, String email, String passwordHash, UserRole role) {
        try {
            String userId = nextUserId();
            String row = String.join(",",
                    userId,
                    sanitize(name),
                    sanitize(staffOrStudentId),
                    sanitize(email),
                    sanitize(passwordHash),
                    role.name(),
                    "ACTIVE",
                    ""
            );
            Files.writeString(
                    USERS_CSV,
                    row + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
            return new User(userId, name, staffOrStudentId, email, passwordHash, role, "ACTIVE", "");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write users.csv", ex);
        }
    }

    public void updateLastLoginAt(String userId) {
        try {
            if (Files.notExists(USERS_CSV)) {
                return;
            }
            List<String> lines = Files.readAllLines(USERS_CSV, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return;
            }
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            List<String> updated = new ArrayList<>();
            updated.add(lines.get(0));

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < 8) {
                    continue;
                }
                if (cols[0].equals(userId)) {
                    cols[7] = now;
                }
                updated.add(String.join(",", cols));
            }

            Files.write(USERS_CSV, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to update last_login_at in users.csv", ex);
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
}
