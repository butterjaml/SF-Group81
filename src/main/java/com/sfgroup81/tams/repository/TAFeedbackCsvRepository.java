package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.TAFeedback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TAFeedbackCsvRepository {
    private static final String HEADER = "feedback_id,ta_user_id,mo_user_id,position_id,communication_rating,teaching_rating,reliability_rating,comment,submitted_at";
    private final Path feedbackCsv;

    public TAFeedbackCsvRepository() {
        this(Path.of("data"));
    }

    public TAFeedbackCsvRepository(Path dataDir) {
        this.feedbackCsv = dataDir.resolve("ta_feedback.csv");
    }

    public List<TAFeedback> findAll() {
        try {
            if (Files.notExists(feedbackCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(feedbackCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }
            List<TAFeedback> feedbackList = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 9) {
                    continue;
                }
                feedbackList.add(new TAFeedback(
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        parseInt(cols[4]),
                        parseInt(cols[5]),
                        parseInt(cols[6]),
                        cols[7],
                        cols[8]
                ));
            }
            return feedbackList;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read ta_feedback.csv", ex);
        }
    }

    public List<TAFeedback> findByTaUserId(String taUserId) {
        return findAll().stream().filter(item -> item.taUserId().equals(taUserId)).toList();
    }

    public boolean exists(String moUserId, String taUserId, String positionId) {
        return findAll().stream().anyMatch(item ->
                item.moUserId().equals(moUserId)
                        && item.taUserId().equals(taUserId)
                        && item.positionId().equals(positionId)
        );
    }

    public TAFeedback save(TAFeedback feedback) {
        List<TAFeedback> all = new ArrayList<>(findAll());
        all.add(feedback);
        rewriteAll(all);
        return feedback;
    }

    public String nextFeedbackId() {
        return findAll().stream()
                .map(TAFeedback::feedbackId)
                .filter(id -> id.startsWith("FB"))
                .map(id -> id.substring(2))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("FB%04d", max + 1))
                .orElse("FB0001");
    }

    private void rewriteAll(List<TAFeedback> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (TAFeedback item : rows) {
            lines.add(String.join(",",
                    sanitize(item.feedbackId()),
                    sanitize(item.taUserId()),
                    sanitize(item.moUserId()),
                    sanitize(item.positionId()),
                    Integer.toString(item.communicationRating()),
                    Integer.toString(item.teachingRating()),
                    Integer.toString(item.reliabilityRating()),
                    sanitize(item.comment()),
                    sanitize(item.submittedAt())
            ));
        }
        try {
            Files.write(feedbackCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write ta_feedback.csv", ex);
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
