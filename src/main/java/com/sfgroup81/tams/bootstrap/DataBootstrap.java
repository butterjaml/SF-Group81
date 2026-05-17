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
    private static final String TA_POSITIONS_HEADER = "position_id,course_id,course_name,instructor_name,semester_id,position_type,headcount,deadline,status,title,responsibilities,working_hours,salary_info,mandatory_requirements,preferred_requirements,bonus_requirements,ai_screening_criteria,created_by,created_at,updated_at";
    private static final String LEGACY_TA_POSITIONS_HEADER = "position_id,course_id,course_name,instructor_name,semester_id,position_type,headcount,deadline,status,title,responsibilities,working_hours,salary_info,mandatory_requirements,preferred_requirements,bonus_requirements,created_by,created_at,updated_at";

    private static final Map<String, String> BASE_HEADERS = Map.ofEntries(
            Map.entry("users.csv", "user_id,name,staff_or_student_id,email,password_hash,role,ta_category,status,last_login_at"),
            Map.entry("audit_logs.csv", "log_id,event_type,user_id,user_name,event_time,ip_address,details"),
            Map.entry("ta_positions.csv", TA_POSITIONS_HEADER),
            Map.entry("application_preferences.csv", "preference_id,application_id,course_id,priority_no"),
            Map.entry("resume_files.csv", "resume_id,application_id,file_path,file_type,auto_filename,uploaded_at,updated_at"),
            Map.entry("applicant_profiles.csv", "user_id,semester_id,phone,major,year_of_study,gpa,skills,availability,notes,updated_at"),
            Map.entry("enrollment_profile_snapshots.csv", "snapshot_id,user_id,semester_id,phone,major,year_of_study,gpa,skills,availability,notes,position_ids,resume_file_path,resume_auto_filename,saved_at"),
            Map.entry("ta_applications.csv", "application_id,user_id,position_id,semester_id,priority_no,status,feedback,submitted_at,updated_at"),
            Map.entry("application_status_history.csv", "history_id,application_id,status,note,changed_by,changed_at"),
            Map.entry("interview_invitations.csv", "invitation_id,application_id,scheduled_at,location,notes,online_link,response_status,response_note,created_by,updated_at"),
            Map.entry("casual_work_postings.csv", "posting_id,title,description,work_date,location,required_skills,headcount,compensation,status,created_by,created_at,updated_at"),
            Map.entry("casual_work_applications.csv", "application_id,posting_id,user_id,statement,applied_at"),
            Map.entry("internal_referrals.csv", "referral_id,user_id,recommender_name,note,tagged_by,updated_at"),
            Map.entry("ta_feedback.csv", "feedback_id,ta_user_id,mo_user_id,position_id,communication_rating,teaching_rating,reliability_rating,comment,submitted_at"),
            Map.entry("semesters.csv", "semester_id,is_current,is_viewed,is_archived,created_by,created_at,archived_at,notes"),
            Map.entry("notifications.csv", "notification_id,user_id,created_at,title,message,related_page,related_id,read_at"),
            Map.entry("ai_screening_results.csv", "result_id,position_id,application_id,semester_id,model_name,match_score,matched_skills,missing_skills,summary,strengths,risks,prompt_hash,generated_at")
    );

    private DataBootstrap() {
    }

    public static void initialize() {
        initialize(DATA_DIR);
    }

    public static void initialize(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(dataDir.resolve("resumes"));
            for (Map.Entry<String, String> entry : BASE_HEADERS.entrySet()) {
                Path csvPath = dataDir.resolve(entry.getKey());
                if ("ta_positions.csv".equals(entry.getKey())) {
                    ensureTaPositionHeader(csvPath);
                } else {
                    ensureHeader(csvPath, entry.getValue());
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize CSV data files", ex);
        }
    }

    private static void ensureTaPositionHeader(Path csvPath) throws IOException {
        if (Files.notExists(csvPath)) {
            Files.writeString(
                    csvPath,
                    TA_POSITIONS_HEADER + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
            );
            return;
        }

        java.util.List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            Files.writeString(csvPath, TA_POSITIONS_HEADER + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            return;
        }
        if (TA_POSITIONS_HEADER.equals(lines.get(0))) {
            return;
        }
        if (LEGACY_TA_POSITIONS_HEADER.equals(lines.get(0))) {
            java.util.List<String> migrated = new java.util.ArrayList<>();
            migrated.add(TA_POSITIONS_HEADER);
            for (int i = 1; i < lines.size(); i++) {
                java.util.List<String> cols = parseCsvLine(lines.get(i));
                if (cols.size() == 19) {
                    cols.add(16, "");
                    migrated.add(encodeCsvLine(cols));
                } else {
                    migrated.add(lines.get(i));
                }
            }
            Files.write(csvPath, migrated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            return;
        }
        lines.set(0, TA_POSITIONS_HEADER);
        Files.write(csvPath, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void ensureHeader(Path csvPath, String header) throws IOException {
        if (Files.notExists(csvPath)) {
            Files.writeString(
                    csvPath,
                    header + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
            );
            return;
        }

        java.util.List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            Files.writeString(csvPath, header + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            return;
        }
        if (!header.equals(lines.get(0))) {
            lines.set(0, header);
            Files.write(csvPath, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private static java.util.List<String> parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    private static String encodeCsvLine(java.util.List<String> fields) {
        return fields.stream()
                .map(value -> "\"" + (value == null ? "" : value.trim()).replace("\"", "\"\"") + "\"")
                .collect(java.util.stream.Collectors.joining(","));
    }
}
