package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.AiScreeningResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AiScreeningResultCsvRepository {
    private static final String HEADER = "result_id,position_id,application_id,semester_id,model_name,match_score,matched_skills,missing_skills,summary,strengths,risks,prompt_hash,generated_at";

    private final Path resultCsv;

    public AiScreeningResultCsvRepository() {
        this(Path.of("data"));
    }

    public AiScreeningResultCsvRepository(Path dataDir) {
        this.resultCsv = dataDir.resolve("ai_screening_results.csv");
    }

    public List<AiScreeningResult> findAll() {
        try {
            if (Files.notExists(resultCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(resultCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }
            List<AiScreeningResult> results = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 13) {
                    continue;
                }
                results.add(new AiScreeningResult(
                        cols[0], cols[1], cols[2], cols[3], cols[4], parseDouble(cols[5]), cols[6], cols[7], cols[8], cols[9], cols[10], cols[11], cols[12]
                ));
            }
            return results;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read ai_screening_results.csv", ex);
        }
    }

    public Optional<AiScreeningResult> findByPositionAndApplication(String positionId, String applicationId) {
        return findAll().stream()
                .filter(item -> item.positionId().equals(positionId) && item.applicationId().equals(applicationId))
                .max(Comparator.comparing(AiScreeningResult::generatedAt));
    }

    public AiScreeningResult saveOrUpdate(AiScreeningResult result) {
        List<AiScreeningResult> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.positionId().equals(result.positionId()) && item.applicationId().equals(result.applicationId()));
        all.add(result);
        rewriteAll(all);
        return result;
    }

    public String nextResultId() {
        return findAll().stream()
                .map(AiScreeningResult::resultId)
                .filter(id -> id.startsWith("ASR"))
                .map(id -> id.substring(3))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("ASR%04d", max + 1))
                .orElse("ASR0001");
    }

    private void rewriteAll(List<AiScreeningResult> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (AiScreeningResult item : rows) {
            lines.add(String.join(",",
                    sanitize(item.resultId()),
                    sanitize(item.positionId()),
                    sanitize(item.applicationId()),
                    sanitize(item.semesterId()),
                    sanitize(item.modelName()),
                    Double.toString(item.matchScore()),
                    sanitize(item.matchedSkills()),
                    sanitize(item.missingSkills()),
                    sanitize(item.summary()),
                    sanitize(item.strengths()),
                    sanitize(item.risks()),
                    sanitize(item.promptHash()),
                    sanitize(item.generatedAt())
            ));
        }
        try {
            Files.write(resultCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write ai_screening_results.csv", ex);
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
