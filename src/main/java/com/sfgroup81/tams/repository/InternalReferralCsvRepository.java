package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.InternalReferral;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class InternalReferralCsvRepository {
    private static final String HEADER = "referral_id,user_id,recommender_name,note,tagged_by,updated_at";
    private final Path referralCsv;

    public InternalReferralCsvRepository() {
        this(Path.of("data"));
    }

    public InternalReferralCsvRepository(Path dataDir) {
        this.referralCsv = dataDir.resolve("internal_referrals.csv");
    }

    public List<InternalReferral> findAll() {
        try {
            if (Files.notExists(referralCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(referralCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }
            List<InternalReferral> referrals = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 6) {
                    continue;
                }
                referrals.add(new InternalReferral(cols[0], cols[1], cols[2], cols[3], cols[4], cols[5]));
            }
            return referrals;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read internal_referrals.csv", ex);
        }
    }

    public Optional<InternalReferral> findByUserId(String userId) {
        return findAll().stream().filter(item -> item.userId().equals(userId)).findFirst();
    }

    public InternalReferral saveOrUpdate(InternalReferral referral) {
        List<InternalReferral> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.userId().equals(referral.userId()));
        all.add(referral);
        rewriteAll(all);
        return referral;
    }

    public String nextReferralId() {
        return findAll().stream()
                .map(InternalReferral::referralId)
                .filter(id -> id.startsWith("RF"))
                .map(id -> id.substring(2))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("RF%04d", max + 1))
                .orElse("RF0001");
    }

    private void rewriteAll(List<InternalReferral> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (InternalReferral item : rows) {
            lines.add(String.join(",",
                    sanitize(item.referralId()),
                    sanitize(item.userId()),
                    sanitize(item.recommenderName()),
                    sanitize(item.note()),
                    sanitize(item.taggedBy()),
                    sanitize(item.updatedAt())
            ));
        }
        try {
            Files.write(referralCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write internal_referrals.csv", ex);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
