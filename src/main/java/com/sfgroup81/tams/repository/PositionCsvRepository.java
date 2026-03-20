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
    private static final Path POSITION_CSV = Path.of("data", "ta_positions.csv");

    public List<TAPosition> findAll() {
        try {
            if (Files.notExists(POSITION_CSV)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(POSITION_CSV, StandardCharsets.UTF_8);
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
                if (cols.length < 12) {
                    continue;
                }
                positions.add(new TAPosition(
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        parseInt(cols[4]),
                        cols[5],
                        cols[6],
                        cols[7],
                        cols[8],
                        cols[9],
                        cols[10],
                        cols[11]
                ));
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
        lines.add("position_id,course_id,semester_id,position_type,headcount,deadline,status,title,description,created_by,created_at,updated_at");
        for (TAPosition p : positions) {
            lines.add(String.join(",",
                    sanitize(p.positionId()),
                    sanitize(p.courseId()),
                    sanitize(p.semesterId()),
                    sanitize(p.positionType()),
                    Integer.toString(p.headcount()),
                    sanitize(p.deadline()),
                    sanitize(p.status()),
                    sanitize(p.title()),
                    sanitize(p.description()),
                    sanitize(p.createdBy()),
                    sanitize(p.createdAt()),
                    sanitize(p.updatedAt())
            ));
        }

        try {
            Files.write(POSITION_CSV, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
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
