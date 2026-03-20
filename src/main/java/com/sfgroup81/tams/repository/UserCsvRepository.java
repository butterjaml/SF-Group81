package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
