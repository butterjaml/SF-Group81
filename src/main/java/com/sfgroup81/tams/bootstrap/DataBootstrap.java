package com.sfgroup81.tams.bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public final class DataBootstrap {
    private static final Path DATA_DIR = Path.of("data");
    private static final Path RESUME_DIR = DATA_DIR.resolve("resumes");

    private static final Map<String, String> BASE_HEADERS = Map.of(
            "users.csv", "user_id,name,staff_or_student_id,email,password_hash,role,status,last_login_at",
            "ta_positions.csv", "position_id,course_id,semester_id,position_type,headcount,deadline,status,title,description,created_by,created_at,updated_at",
            "application_preferences.csv", "preference_id,application_id,course_id,priority_no",
            "resume_files.csv", "resume_id,application_id,file_path,file_type,auto_filename,uploaded_at,updated_at"
    );

    private DataBootstrap() {
    }

    public static void initialize() {
        try {
            Files.createDirectories(DATA_DIR);
            Files.createDirectories(RESUME_DIR);
            for (Map.Entry<String, String> entry : BASE_HEADERS.entrySet()) {
                Path csvPath = DATA_DIR.resolve(entry.getKey());
                if (Files.notExists(csvPath)) {
                    Files.writeString(
                            csvPath,
                            entry.getValue() + System.lineSeparator(),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE_NEW
                    );
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize CSV data files", ex);
        }
    }
}
