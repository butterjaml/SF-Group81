package com.sfgroup81.tams.repository;

import com.sfgroup81.tams.model.InterviewInvitation;
import com.sfgroup81.tams.model.InterviewResponseStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class InterviewInvitationCsvRepository {
    private static final String HEADER = "invitation_id,application_id,scheduled_at,location,notes,response_status,response_note,created_by,updated_at";
    private final Path invitationCsv;

    public InterviewInvitationCsvRepository() {
        this(Path.of("data"));
    }

    public InterviewInvitationCsvRepository(Path dataDir) {
        this.invitationCsv = dataDir.resolve("interview_invitations.csv");
    }

    public List<InterviewInvitation> findAll() {
        try {
            if (Files.notExists(invitationCsv)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(invitationCsv, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return List.of();
            }
            List<InterviewInvitation> invitations = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",", -1);
                if (cols.length < 9) {
                    continue;
                }
                invitations.add(new InterviewInvitation(
                        cols[0],
                        cols[1],
                        cols[2],
                        cols[3],
                        cols[4],
                        InterviewResponseStatus.valueOf(cols[5]),
                        cols[6],
                        cols[7],
                        cols[8]
                ));
            }
            return invitations;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read interview_invitations.csv", ex);
        }
    }

    public Optional<InterviewInvitation> findById(String invitationId) {
        return findAll().stream().filter(item -> item.invitationId().equals(invitationId)).findFirst();
    }

    public List<InterviewInvitation> findByApplicationId(String applicationId) {
        return findAll().stream()
                .filter(item -> item.applicationId().equals(applicationId))
                .sorted(Comparator.comparing(InterviewInvitation::scheduledAt))
                .toList();
    }

    public InterviewInvitation saveOrUpdate(InterviewInvitation invitation) {
        List<InterviewInvitation> all = new ArrayList<>(findAll());
        all.removeIf(item -> item.invitationId().equals(invitation.invitationId()));
        all.add(invitation);
        rewriteAll(all);
        return invitation;
    }

    public String nextInvitationId() {
        return findAll().stream()
                .map(InterviewInvitation::invitationId)
                .filter(id -> id.startsWith("IV"))
                .map(id -> id.substring(2))
                .filter(num -> num.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .map(max -> String.format("IV%04d", max + 1))
                .orElse("IV0001");
    }

    private void rewriteAll(List<InterviewInvitation> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (InterviewInvitation item : rows) {
            lines.add(String.join(",",
                    sanitize(item.invitationId()),
                    sanitize(item.applicationId()),
                    sanitize(item.scheduledAt()),
                    sanitize(item.location()),
                    sanitize(item.notes()),
                    item.responseStatus().name(),
                    sanitize(item.responseNote()),
                    sanitize(item.createdBy()),
                    sanitize(item.updatedAt())
            ));
        }
        try {
            Files.write(invitationCsv, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write interview_invitations.csv", ex);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
