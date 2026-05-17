package com.sfgroup81.tams.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataBootstrapTest {
    @TempDir
    Path tempDir;

    @Test
    void initializeShouldMigrateLegacyTaPositionRowsWithAiCriteriaColumn() throws Exception {
        Path positions = tempDir.resolve("ta_positions.csv");
        Files.write(positions, List.of(
                "position_id,course_id,course_name,instructor_name,semester_id,position_type,headcount,deadline,status,title,responsibilities,working_hours,salary_info,mandatory_requirements,preferred_requirements,bonus_requirements,created_by,created_at,updated_at",
                "P0001,COMP511,Software Testing,Grace Liu,2026S2,Lead TA,1,2026-05-25,PUBLISHED,COMP511 Lead TA,Lead labs,8 hours/week,120 yuan/hour,Grade A,Selenium,Mentoring,U0002,2026-04-01T09:00:00,2026-04-05T11:00:00"
        ));

        DataBootstrap.initialize(tempDir);

        List<String> lines = Files.readAllLines(positions);
        assertTrue(lines.get(0).contains("ai_screening_criteria"));
        assertEquals(20, splitSimpleCsv(lines.get(1)).size());
        assertEquals("", splitSimpleCsv(lines.get(1)).get(16));
        assertEquals("U0002", splitSimpleCsv(lines.get(1)).get(17));
    }

    private List<String> splitSimpleCsv(String line) {
        return java.util.Arrays.stream(line.split(",", -1))
                .map(value -> value.replaceAll("^\"|\"$", ""))
                .toList();
    }
}
