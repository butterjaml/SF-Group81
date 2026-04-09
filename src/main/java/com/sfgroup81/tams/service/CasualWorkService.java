package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.CasualWorkApplication;
import com.sfgroup81.tams.model.CasualWorkPosting;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.CasualWorkApplicationCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkPostingCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CasualWorkService {
    private final CasualWorkPostingCsvRepository postingRepository;
    private final CasualWorkApplicationCsvRepository applicationRepository;
    private final UserCsvRepository userRepository;

    public CasualWorkService(CasualWorkPostingCsvRepository postingRepository,
                             CasualWorkApplicationCsvRepository applicationRepository,
                             UserCsvRepository userRepository) {
        this.postingRepository = postingRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    public CasualWorkPosting createPosting(String title,
                                           String description,
                                           String workDate,
                                           String location,
                                           String requiredSkills,
                                           int headcount,
                                           String compensation,
                                           String createdBy) {
        User admin = requireUser(createdBy, UserRole.ADMIN);
        if (headcount <= 0) {
            throw new IllegalArgumentException("Headcount must be positive");
        }
        String now = now();
        return postingRepository.saveOrUpdate(new CasualWorkPosting(
                postingRepository.nextPostingId(),
                safe(title),
                safe(description),
                safe(workDate),
                safe(location),
                safe(requiredSkills),
                headcount,
                safe(compensation),
                "OPEN",
                admin.userId(),
                now,
                now
        ));
    }

    public CasualWorkApplication apply(String postingId, String userId, String statement) {
        requireNonModularTa(userId);
        CasualWorkPosting posting = postingRepository.findById(postingId)
                .orElseThrow(() -> new IllegalArgumentException("Casual work posting not found: " + postingId));
        if (!"OPEN".equalsIgnoreCase(posting.status())) {
            throw new IllegalArgumentException("Posting is not open: " + postingId);
        }
        if (applicationRepository.exists(postingId, userId)) {
            throw new IllegalArgumentException("Application already exists for this posting");
        }
        return applicationRepository.save(new CasualWorkApplication(
                applicationRepository.nextApplicationId(),
                postingId,
                userId,
                safe(statement),
                now()
        ));
    }

    public List<CasualWorkPosting> listOpenPostings() {
        return postingRepository.findAll().stream()
                .filter(item -> "OPEN".equalsIgnoreCase(item.status()))
                .toList();
    }

    public List<CasualWorkApplication> listApplicationsForPosting(String postingId) {
        return applicationRepository.findByPostingId(postingId);
    }

    private User requireUser(String userId, UserRole role) {
        return userRepository.findAll().stream()
                .filter(user -> user.userId().equals(userId))
                .findFirst()
                .filter(user -> user.role() == role)
                .orElseThrow(() -> new IllegalArgumentException("User role mismatch for " + userId));
    }

    private User requireNonModularTa(String userId) {
        User user = requireUser(userId, UserRole.TA);
        if (user.taCategory() != TACategory.NON_MODULAR) {
            throw new IllegalArgumentException("Only non-modular TAs can apply for casual work");
        }
        return user;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
