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

    private static final Map<String, String> BASE_HEADERS = Map.ofEntries(
            Map.entry("users.csv", "user_id,name,staff_or_student_id,email,password_hash,role,ta_category,status,last_login_at"),
            Map.entry("audit_logs.csv", "log_id,event_type,user_id,user_name,event_time,ip_address,details"),
            Map.entry("ta_positions.csv", "position_id,course_id,course_name,instructor_name,semester_id,position_type,headcount,deadline,status,title,responsibilities,working_hours,salary_info,mandatory_requirements,preferred_requirements,bonus_requirements,created_by,created_at,updated_at"),
            Map.entry("application_preferences.csv", "preference_id,application_id,course_id,priority_no"),
            Map.entry("resume_files.csv", "resume_id,application_id,file_path,file_type,auto_filename,uploaded_at,updated_at"),
            Map.entry("applicant_profiles.csv", "user_id,phone,major,year_of_study,gpa,skills,availability,notes,updated_at"),
            Map.entry("enrollment_profile_snapshots.csv", "snapshot_id,user_id,semester_id,phone,major,year_of_study,gpa,skills,availability,notes,position_ids,resume_file_path,resume_auto_filename,saved_at"),
            Map.entry("ta_applications.csv", "application_id,user_id,position_id,priority_no,status,feedback,submitted_at,updated_at"),
            Map.entry("application_status_history.csv", "history_id,application_id,status,note,changed_by,changed_at"),
            Map.entry("interview_invitations.csv", "invitation_id,application_id,scheduled_at,location,notes,online_link,response_status,response_note,created_by,updated_at"),
            Map.entry("casual_work_postings.csv", "posting_id,title,description,work_date,location,required_skills,headcount,compensation,status,created_by,created_at,updated_at"),
            Map.entry("casual_work_applications.csv", "application_id,posting_id,user_id,statement,applied_at"),
            Map.entry("internal_referrals.csv", "referral_id,user_id,recommender_name,note,tagged_by,updated_at"),
            Map.entry("ta_feedback.csv", "feedback_id,ta_user_id,mo_user_id,position_id,communication_rating,teaching_rating,reliability_rating,comment,submitted_at")
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
                ensureHeader(csvPath, entry.getValue());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize CSV data files", ex);
        }
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
}
