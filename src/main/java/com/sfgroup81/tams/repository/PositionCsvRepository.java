package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.TAPosition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PositionCsvRepository {
    private static final String HEADER = "position_id,course_id,course_name,instructor_name,semester_id,position_type,headcount,deadline,status,title,responsibilities,working_hours,salary_info,mandatory_requirements,preferred_requirements,bonus_requirements,ai_screening_criteria,created_by,created_at,updated_at";
    private final Path positionCsv;

    public PositionCsvRepository() {
        this(Path.of("data"));
    }

    public PositionCsvRepository(Path dataDir) {
        this.positionCsv = dataDir.resolve("ta_positions.csv");
    }

    public List<TAPosition> findAll() {
        try {
            if (Files.notExists(positionCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(positionCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }

            List<TAPosition> positions = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < 19) {
                    continue;
                }
                if (cols.length >= 20) {
                    positions.add(new TAPosition(
                            cols[0],
                            cols[1],
                            cols[2],
                            cols[3],
                            cols[4],
                            cols[5],
                            parseInt(cols[6]),
                            cols[7],
                            cols[8],
                            cols[9],
                            cols[10],
                            cols[11],
                            cols[12],
                            cols[13],
                            cols[14],
                            cols[15],
                            cols[16],
                            cols[17],
                            cols[18],
                            cols[19]
                    ));
                } else {
                    positions.add(new TAPosition(
                            cols[0],
                            cols[1],
                            cols[2],
                            cols[3],
                            cols[4],
                            cols[5],
                            parseInt(cols[6]),
                            cols[7],
                            cols[8],
                            cols[9],
                            cols[10],
                            cols[11],
                            cols[12],
                            cols[13],
                            cols[14],
                            cols[15],
                            "",
                            cols[16],
                            cols[17],
                            cols[18]
                    ));
                }
            }
            return positions;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read ta_positions.csv", ex);
        }
    }

    public Optional<TAPosition> findById(String positionId) {
        return findAll().stream().filter(p -> p.positionId().equals(positionId)).findFirst();
    }

    public TAPosition saveOrUpdate(TAPosition position) {
        List<TAPosition> all = new ArrayList<>(findAll());
        int existingIndex = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).positionId().equals(position.positionId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            all.set(existingIndex, position);
        } else {
            all.add(position);
        }

        rewriteAll(all);
        return position;
    }

    public String nextPositionId() {
        return findAll().stream()
                .map(TAPosition::positionId)
                .filter(id -> id.startsWith("P"))
                .map(id -> id.substring(1))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("P%04d", max + 1))
                .orElse("P0001");
    }

    private void rewriteAll(List<TAPosition> positions) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (TAPosition p : positions) {
            lines.add(String.join(",",
                    sanitize(p.positionId()),
                    sanitize(p.courseId()),
                    sanitize(p.courseName()),
                    sanitize(p.instructorName()),
                    sanitize(p.semesterId()),
                    sanitize(p.positionType()),
                    Integer.toString(p.headcount()),
                    sanitize(p.deadline()),
                    sanitize(p.status()),
                    sanitize(p.title()),
                    sanitize(p.responsibilities()),
                    sanitize(p.workingHours()),
                    sanitize(p.salaryInfo()),
                    sanitize(p.mandatoryRequirements()),
                    sanitize(p.preferredRequirements()),
                    sanitize(p.bonusRequirements()),
                    sanitize(p.aiScreeningCriteria()),
                    sanitize(p.createdBy()),
                    sanitize(p.createdAt()),
                    sanitize(p.updatedAt())
            ));
        }

        try {
            Files.write(positionCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write ta_positions.csv", ex);
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
