package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.CasualWorkPosting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CasualWorkPostingCsvRepository {
    private static final String HEADER = "posting_id,title,description,work_date,location,required_skills,headcount,compensation,status,created_by,created_at,updated_at";
    private final Path postingCsv;

    public CasualWorkPostingCsvRepository() {
        this(Path.of("data"));
    }

    public CasualWorkPostingCsvRepository(Path dataDir) {
        this.postingCsv = dataDir.resolve("casual_work_postings.csv");
    }

    public List<CasualWorkPosting> findAll() {
        try {
            if (Files.notExists(postingCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(postingCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }
            List<CasualWorkPosting> postings = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 12) {
                    continue;
                }
                postings.add(new CasualWorkPosting(
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
                        cols[11]
                ));
            }
            return postings;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read casual_work_postings.csv", ex);
        }
    }

    public Optional<CasualWorkPosting> findById(String postingId) {
        return findAll().stream().filter(item -> item.postingId().equals(postingId)).findFirst();
    }

    public CasualWorkPosting saveOrUpdate(CasualWorkPosting posting) {
        List<CasualWorkPosting> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.postingId().equals(posting.postingId()));
        all.add(posting);
        rewriteAll(all);
        return posting;
    }

    public String nextPostingId() {
        return findAll().stream()
                .map(CasualWorkPosting::postingId)
                .filter(id -> id.startsWith("CW"))
                .map(id -> id.substring(2))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("CW%04d", max + 1))
                .orElse("CW0001");
    }

    private void rewriteAll(List<CasualWorkPosting> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (CasualWorkPosting item : rows) {
            lines.add(String.join(",",
                    sanitize(item.postingId()),
                    sanitize(item.title()),
                    sanitize(item.description()),
                    sanitize(item.workDate()),
                    sanitize(item.location()),
                    sanitize(item.requiredSkills()),
                    Integer.toString(item.headcount()),
                    sanitize(item.compensation()),
                    sanitize(item.status()),
                    sanitize(item.createdBy()),
                    sanitize(item.createdAt()),
                    sanitize(item.updatedAt())
            ));
        }
        try {
            Files.write(postingCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write casual_work_postings.csv", ex);
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
