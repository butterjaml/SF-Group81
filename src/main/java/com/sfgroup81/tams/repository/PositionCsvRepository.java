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
            List<List<String>> rows = parseCsv(Files.readString(positionCsv, StandardCharsets.UTF_8));
            if (rows.size() <= 1) {
                return List.of();
            }

            List<TAPosition> positions = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                List<String> cols = rows.get(i);
                if (cols.isEmpty()) {
                    continue;
                }
                if (cols.size() < 19) {
                    continue;
                }
                if (cols.size() >= 20) {
                    positions.add(new TAPosition(
                            cols.get(0),
                            cols.get(1),
                            cols.get(2),
                            cols.get(3),
                            cols.get(4),
                            cols.get(5),
                            parseInt(cols.get(6)),
                            cols.get(7),
                            cols.get(8),
                            cols.get(9),
                            cols.get(10),
                            cols.get(11),
                            cols.get(12),
                            cols.get(13),
                            cols.get(14),
                            cols.get(15),
                            cols.get(16),
                            cols.get(17),
                            cols.get(18),
                            cols.get(19)
                    ));
                } else {
                    positions.add(new TAPosition(
                            cols.get(0),
                            cols.get(1),
                            cols.get(2),
                            cols.get(3),
                            cols.get(4),
                            cols.get(5),
                            parseInt(cols.get(6)),
                            cols.get(7),
                            cols.get(8),
                            cols.get(9),
                            cols.get(10),
                            cols.get(11),
                            cols.get(12),
                            cols.get(13),
                            cols.get(14),
                            cols.get(15),
                            "",
                            cols.get(16),
                            cols.get(17),
                            cols.get(18)
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
                    encode(p.positionId()),
                    encode(p.courseId()),
                    encode(p.courseName()),
                    encode(p.instructorName()),
                    encode(p.semesterId()),
                    encode(p.positionType()),
                    Integer.toString(p.headcount()),
                    encode(p.deadline()),
                    encode(p.status()),
                    encode(p.title()),
                    encode(p.responsibilities()),
                    encode(p.workingHours()),
                    encode(p.salaryInfo()),
                    encode(p.mandatoryRequirements()),
                    encode(p.preferredRequirements()),
                    encode(p.bonusRequirements()),
                    encode(p.aiScreeningCriteria()),
                    encode(p.createdBy()),
                    encode(p.createdAt()),
                    encode(p.updatedAt())
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

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }

            if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                row.add(field.toString());
                field.setLength(0);
                if (!isBlankRow(row)) {
                    rows.add(row);
                }
                row = new ArrayList<>();
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                field.append(c);
            }
        }

        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            if (!isBlankRow(row)) {
                rows.add(row);
            }
        }
        return rows;
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(value -> value == null || value.isBlank());
    }

    private String encode(String value) {
        String safe = value == null ? "" : value.trim();
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
