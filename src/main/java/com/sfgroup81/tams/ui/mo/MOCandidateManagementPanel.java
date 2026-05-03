package com.sfgroup81.tams.ui.mo;

import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.ApplicationStatusHistory;
import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ResumeFileRecord;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.service.ApplicationReviewService;
import com.sfgroup81.tams.service.AuditLogService;
import com.sfgroup81.tams.service.CandidateFilterCriteria;
import com.sfgroup81.tams.service.CandidateInsightService;
import com.sfgroup81.tams.service.CandidateRankingWeights;
import com.sfgroup81.tams.service.CandidateReviewView;
import com.sfgroup81.tams.service.CandidateScreeningService;
import com.sfgroup81.tams.service.CandidateScreeningView;
import com.sfgroup81.tams.service.CandidateSortOption;
import com.sfgroup81.tams.service.InterviewService;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MOCandidateManagementPanel extends JPanel {
    private static final int MAX_COMPARE = 4;

    private final User currentUser;
    private final PositionCsvRepository positionRepository;
    private final ApplicationStatusHistoryCsvRepository historyRepository;
    private final ApplicationReviewService reviewService;
    private final InterviewService interviewService;
    private final CandidateInsightService candidateInsightService;
    private final CandidateScreeningService candidateScreeningService;
    private final AuditLogService auditLogService;

    private final JComboBox<TAPosition> positionCombo = new JComboBox<>();
    private final JTextField nameFilterField = new JTextField(10);
    private final JTextField studentIdFilterField = new JTextField(10);
    private final JTextField skillFilterField = new JTextField(10);
    private final JTextField yearFilterField = new JTextField(8);
    private final JTextField availabilityFilterField = new JTextField(10);
    private final JTextField minGpaField = new JTextField(5);
    private final JTextField maxGpaField = new JTextField(5);
    private final JCheckBox recommendedOnlyCheck = new JCheckBox("Internally recommended");
    private final JCheckBox experiencedOnlyCheck = new JCheckBox("Past TA experience");
    private final JComboBox<CandidateSortOption> sortCombo = new JComboBox<>(CandidateSortOption.values());
    private final JSpinner gpaWeightSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 100, 5));
    private final JSpinner experienceWeightSpinner = new JSpinner(new SpinnerNumberModel(25, 0, 100, 5));
    private final JSpinner skillWeightSpinner = new JSpinner(new SpinnerNumberModel(20, 0, 100, 5));
    private final JSpinner referralWeightSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 100, 5));
    private final JSpinner reputationWeightSpinner = new JSpinner(new SpinnerNumberModel(15, 0, 100, 5));
    private final JLabel activeFilterLabel = new JLabel("Filters: None");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Application", "Applicant", "Student ID", "Score", "GPA", "Experience", "Referral", "Status", "Applied At"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextArea detailArea = new JTextArea();
    private final JTextArea noteArea = new JTextArea(5, 20);
    private final JTextArea referralSummaryArea = new JTextArea(3, 20);
    private final JTextField interviewTimeField = new JTextField("2026-05-10T10:00:00");
    private final JTextField interviewLocationField = new JTextField("Room 301");
    private final JTextField interviewLinkField = new JTextField();
    private final JPanel comparisonGrid = new JPanel(new GridLayout(1, MAX_COMPARE, 12, 12));

    private List<CandidateScreeningView> currentCandidates = List.of();

    public MOCandidateManagementPanel(User currentUser,
                                      PositionCsvRepository positionRepository,
                                      ApplicationStatusHistoryCsvRepository historyRepository,
                                      ApplicationReviewService reviewService,
                                      InterviewService interviewService,
                                      CandidateInsightService candidateInsightService,
                                      CandidateScreeningService candidateScreeningService,
                                      AuditLogService auditLogService,
                                      Runnable onBack) {
        this.currentUser = currentUser;
        this.positionRepository = positionRepository;
        this.historyRepository = historyRepository;
        this.reviewService = reviewService;
        this.interviewService = interviewService;
        this.candidateInsightService = candidateInsightService;
        this.candidateScreeningService = candidateScreeningService;
        this.auditLogService = auditLogService;

        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("Candidate Details/Comparison", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadPositions();
        refreshCandidates();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel top = new JPanel(new BorderLayout(16, 16));
        top.setOpaque(false);
        top.add(buildFilterCard(), BorderLayout.CENTER);
        top.add(buildWeightCard(), BorderLayout.EAST);
        content.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(16, 16));
        center.setOpaque(false);
        center.add(buildTableSection(), BorderLayout.CENTER);
        center.add(buildRightPanel(), BorderLayout.EAST);
        content.add(center, BorderLayout.CENTER);

        content.add(buildComparisonSection(), BorderLayout.SOUTH);
        return content;
    }

    private JPanel buildFilterCard() {
        JPanel card = PrototypeUi.createCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        addField(card, gbc, row, 0, "Position", positionCombo);
        addField(card, gbc, row++, 2, "Sort", sortCombo);
        addField(card, gbc, row, 0, "Name", nameFilterField);
        addField(card, gbc, row++, 2, "Student ID", studentIdFilterField);
        addField(card, gbc, row, 0, "Skill", skillFilterField);
        addField(card, gbc, row++, 2, "Year", yearFilterField);
        addField(card, gbc, row, 0, "Availability", availabilityFilterField);
        addField(card, gbc, row++, 2, "GPA Min", minGpaField);
        addField(card, gbc, row++, 2, "GPA Max", maxGpaField);

        recommendedOnlyCheck.setOpaque(false);
        experiencedOnlyCheck.setOpaque(false);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        card.add(recommendedOnlyCheck, gbc);
        gbc.gridx = 2;
        card.add(experiencedOnlyCheck, gbc);

        JButton clearButton = PrototypeUi.secondaryButton("Clear Filters");
        clearButton.addActionListener(e -> clearFilters());
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        card.add(clearButton, gbc);

        activeFilterLabel.setForeground(new Color(88, 98, 120));
        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.gridwidth = 5;
        card.add(activeFilterLabel, gbc);

        positionCombo.addActionListener(e -> refreshCandidates());
        sortCombo.addActionListener(e -> refreshCandidates());
        recommendedOnlyCheck.addActionListener(e -> refreshCandidates());
        experiencedOnlyCheck.addActionListener(e -> refreshCandidates());
        bindRefresh(nameFilterField, studentIdFilterField, skillFilterField, yearFilterField, availabilityFilterField, minGpaField, maxGpaField);
        return card;
    }

    private JPanel buildWeightCard() {
        JPanel card = PrototypeUi.createVerticalCard();
        card.setPreferredSize(new Dimension(260, 0));
        card.add(PrototypeUi.sectionTitle("Ranking Weights"));
        PrototypeUi.addVerticalGap(card, 12);
        card.add(weightRow("GPA", gpaWeightSpinner));
        PrototypeUi.addVerticalGap(card, 8);
        card.add(weightRow("Experience", experienceWeightSpinner));
        PrototypeUi.addVerticalGap(card, 8);
        card.add(weightRow("Skill Match", skillWeightSpinner));
        PrototypeUi.addVerticalGap(card, 8);
        card.add(weightRow("Referral", referralWeightSpinner));
        PrototypeUi.addVerticalGap(card, 8);
        card.add(weightRow("Reputation", reputationWeightSpinner));

        gpaWeightSpinner.addChangeListener(e -> refreshCandidates());
        experienceWeightSpinner.addChangeListener(e -> refreshCandidates());
        skillWeightSpinner.addChangeListener(e -> refreshCandidates());
        referralWeightSpinner.addChangeListener(e -> refreshCandidates());
        reputationWeightSpinner.addChangeListener(e -> refreshCandidates());
        return card;
    }

    private JPanel weightRow(String label, JSpinner spinner) {
        JPanel row = new JPanel(new BorderLayout(8, 8));
        row.setOpaque(false);
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(spinner, BorderLayout.EAST);
        return row;
    }

    private JPanel buildTableSection() {
        JPanel section = new JPanel(new BorderLayout(12, 12));
        section.setOpaque(false);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton exportRankedButton = PrototypeUi.primaryButton("Export Ranked CSV");
        exportRankedButton.addActionListener(e -> exportRankedCandidates());
        JButton exportBasicButton = PrototypeUi.secondaryButton("Export Basic CSV");
        exportBasicButton.addActionListener(e -> exportSelectedPosition());
        JButton viewResumeButton = PrototypeUi.secondaryButton("View Resume");
        viewResumeButton.addActionListener(e -> viewSelectedResume());
        JButton exportResumesButton = PrototypeUi.secondaryButton("Download Resumes");
        exportResumesButton.addActionListener(e -> exportResumesForSelectedPosition());
        actions.add(exportRankedButton);
        actions.add(exportBasicButton);
        actions.add(viewResumeButton);
        actions.add(exportResumesButton);
        section.add(actions, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelectedDetails();
                renderComparisonCards();
            }
        });
        section.add(new JScrollPane(table), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildRightPanel() {
        JPanel right = PrototypeUi.createVerticalCard();
        right.setPreferredSize(new Dimension(360, 0));
        right.setLayout(new BorderLayout(8, 8));

        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        referralSummaryArea.setEditable(false);
        referralSummaryArea.setLineWrap(true);
        referralSummaryArea.setWrapStyleWord(true);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        top.add(new JScrollPane(noteArea), BorderLayout.SOUTH);
        right.add(top, BorderLayout.CENTER);
        right.add(buildActionPanel(), BorderLayout.SOUTH);
        return right;
    }

    private JPanel buildActionPanel() {
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 8));
        actions.setOpaque(false);

        JPanel statusActions = new JPanel();
        statusActions.setOpaque(false);
        statusActions.add(createStatusButton("Pending", ApplicationStatus.PENDING_REVIEW));
        statusActions.add(createStatusButton("Hired", ApplicationStatus.HIRED));
        statusActions.add(createStatusButton("Rejected", ApplicationStatus.REJECTED));
        actions.add(statusActions);

        JPanel interviewRow = PrototypeUi.createCard();
        interviewRow.setLayout(new GridLayout(0, 1, 6, 6));
        interviewRow.add(new JLabel("Interview time (ISO):"));
        interviewRow.add(interviewTimeField);
        interviewRow.add(new JLabel("Location:"));
        interviewRow.add(interviewLocationField);
        interviewRow.add(new JLabel("Online link:"));
        interviewRow.add(interviewLinkField);
        JButton scheduleInterviewButton = PrototypeUi.primaryButton("Arrange Interview");
        scheduleInterviewButton.addActionListener(e -> scheduleInterview());
        interviewRow.add(scheduleInterviewButton);
        actions.add(interviewRow);

        JPanel referralRow = PrototypeUi.createCard();
        referralRow.setLayout(new GridLayout(0, 1, 6, 6));
        referralRow.add(new JLabel("Internal referrals"));
        referralRow.add(new JScrollPane(referralSummaryArea));
        JButton tagReferralButton = PrototypeUi.secondaryButton("Add Internal Referral");
        tagReferralButton.addActionListener(e -> tagReferral());
        referralRow.add(tagReferralButton);
        actions.add(referralRow);

        return actions;
    }

    private JPanel buildComparisonSection() {
        JPanel section = new JPanel(new BorderLayout(12, 12));
        section.setOpaque(false);

        JLabel title = PrototypeUi.sectionTitle("Candidate comparison");
        section.add(title, BorderLayout.NORTH);

        comparisonGrid.setOpaque(false);
        section.add(new JScrollPane(comparisonGrid), BorderLayout.CENTER);
        return section;
    }

    private JButton createStatusButton(String text, ApplicationStatus status) {
        JButton button = PrototypeUi.primaryButton(text);
        button.addActionListener(e -> updateSelectedStatus(status));
        return button;
    }

    private void loadPositions() {
        List<TAPosition> positions = new ArrayList<>(positionRepository.findAll().stream()
                .filter(position -> position.createdBy().equals(currentUser.userId()))
                .toList());
        if (positions.isEmpty()) {
            positions = new ArrayList<>(positionRepository.findAll());
        }
        positionCombo.setModel(new DefaultComboBoxModel<>(positions.toArray(new TAPosition[0])));
        positionCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TAPosition position) {
                    label.setText(position.positionId() + " - " + position.courseName());
                }
                return label;
            }
        });
    }

    private void refreshCandidates() {
        tableModel.setRowCount(0);
        TAPosition selected = (TAPosition) positionCombo.getSelectedItem();
        if (selected == null) {
            currentCandidates = List.of();
            detailArea.setText("No positions available.");
            comparisonGrid.removeAll();
            return;
        }

        currentCandidates = candidateScreeningService.screenCandidates(
                selected,
                buildCriteria(),
                buildWeights(),
                (CandidateSortOption) sortCombo.getSelectedItem()
        );

        for (CandidateScreeningView view : currentCandidates) {
            tableModel.addRow(new Object[]{
                    view.candidate().application().applicationId(),
                    view.candidate().user() == null ? view.candidate().application().userId() : view.candidate().user().name(),
                    view.candidate().user() == null ? "-" : view.candidate().user().staffOrStudentId(),
                    String.format("%.2f", view.recommendationScore()),
                    String.format("%.2f", view.gpaValue()),
                    view.hasPastTaExperience() ? "YES" : "NO",
                    referralLabel(view.candidate()),
                    view.candidate().application().status(),
                    view.candidate().application().submittedAt()
            });
        }

        updateActiveFilterLabel();
        if (!currentCandidates.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            renderSelectedDetails();
            renderComparisonCards();
        } else {
            detailArea.setText("No applicants match the current screening filters.");
            comparisonGrid.removeAll();
            comparisonGrid.revalidate();
            comparisonGrid.repaint();
        }
    }

    private void renderSelectedDetails() {
        CandidateScreeningView view = selectedScreeningView();
        if (view == null) {
            detailArea.setText("");
            noteArea.setText("");
            referralSummaryArea.setText("");
            return;
        }
        CandidateReviewView candidate = view.candidate();
        List<ApplicationStatusHistory> history = historyRepository.findByApplicationId(candidate.application().applicationId());
        ApplicantProfile profile = candidate.profile();

        StringBuilder builder = new StringBuilder();
        builder.append("Applicant: ").append(candidate.user() == null ? candidate.application().userId() : candidate.user().name()).append('\n');
        builder.append("Student ID: ").append(candidate.user() == null ? "-" : candidate.user().staffOrStudentId()).append('\n');
        builder.append("Recommendation Score: ").append(String.format("%.2f", view.recommendationScore())).append('\n');
        builder.append("GPA: ").append(String.format("%.2f", view.gpaValue())).append('\n');
        builder.append("Past TA Experience: ").append(view.hasPastTaExperience() ? "YES" : "NO").append('\n');
        builder.append("Skill Match: ").append(String.format("%.0f%%", view.skillMatchScore() * 100)).append('\n');
        builder.append("Matched Requirements: ").append(view.matchedRequirementKeywords().isEmpty() ? "-" : String.join("; ", view.matchedRequirementKeywords())).append('\n');
        builder.append("Major / Year: ").append(profile == null ? "-" : profile.major()).append(" / ").append(profile == null ? "-" : profile.yearOfStudy()).append('\n');
        builder.append("Availability: ").append(profile == null ? "-" : profile.availability()).append('\n');
        builder.append("Resume: ").append(resumeLabel(candidate.application().applicationId())).append('\n');
        builder.append("Interview Reply: ").append(interviewSummary(candidate)).append("\n\n");
        builder.append("History:\n");
        for (ApplicationStatusHistory item : history) {
            builder.append("- ").append(item.changedAt()).append(" | ").append(item.status()).append(" | ").append(item.note()).append('\n');
        }
        detailArea.setText(builder.toString());
        noteArea.setText(candidate.application().feedback());
        referralSummaryArea.setText(referralLabel(candidate));
    }

    private void renderComparisonCards() {
        comparisonGrid.removeAll();
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            comparisonGrid.add(emptyCompareCard("Select up to 4 candidates to compare side-by-side."));
        } else {
            for (int i = 0; i < Math.min(rows.length, MAX_COMPARE); i++) {
                comparisonGrid.add(compareCard(currentCandidates.get(rows[i])));
            }
            for (int i = rows.length; i < MAX_COMPARE; i++) {
                comparisonGrid.add(emptyCompareCard("Comparison slot"));
            }
        }
        comparisonGrid.revalidate();
        comparisonGrid.repaint();
    }

    private JPanel compareCard(CandidateScreeningView view) {
        JPanel card = PrototypeUi.createVerticalCard();
        card.add(PrototypeUi.sectionTitle(view.candidate().user() == null ? view.candidate().application().userId() : view.candidate().user().name()));
        PrototypeUi.addVerticalGap(card, 8);
        ApplicantProfile profile = view.candidate().profile();
        card.add(new JLabel("Score: " + String.format("%.2f", view.recommendationScore())));
        card.add(new JLabel("GPA: " + String.format("%.2f", view.gpaValue())));
        card.add(new JLabel("Experience: " + (view.hasPastTaExperience() ? "YES" : "NO")));
        card.add(new JLabel("Major: " + (profile == null ? "-" : profile.major())));
        card.add(new JLabel("Availability: " + (profile == null ? "-" : profile.availability())));
        JTextArea summary = new JTextArea(
                "Skills: " + (profile == null ? "-" : profile.skills()) + "\n"
                        + "Referral: " + referralLabel(view.candidate()) + "\n"
                        + "Matched requirements: " + (view.matchedRequirementKeywords().isEmpty() ? "-" : String.join("; ", view.matchedRequirementKeywords()))
        );
        summary.setEditable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        card.add(new JScrollPane(summary));
        return card;
    }

    private JPanel emptyCompareCard(String text) {
        JPanel card = PrototypeUi.createVerticalCard();
        JLabel label = new JLabel("<html><body style='width:180px'>" + text + "</body></html>");
        label.setForeground(new Color(112, 120, 136));
        card.add(label);
        return card;
    }

    private void exportRankedCandidates() {
        TAPosition position = (TAPosition) positionCombo.getSelectedItem();
        if (position == null) {
            JOptionPane.showMessageDialog(this, "Select a position first.");
            return;
        }
        try {
            Path exportPath = candidateScreeningService.exportRankedCandidates(
                    position,
                    currentCandidates,
                    Path.of("data", "exports"),
                    currentUser.userId()
            );
            JOptionPane.showMessageDialog(this, "Ranked candidate CSV exported to " + exportPath + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSelectedStatus(ApplicationStatus status) {
        CandidateScreeningView view = selectedScreeningView();
        if (view == null) {
            JOptionPane.showMessageDialog(this, "Select an applicant first.");
            return;
        }
        try {
            reviewService.updateStatus(view.candidate().application().applicationId(), status, noteArea.getText(), currentUser.userId());
            refreshCandidates();
            JOptionPane.showMessageDialog(this, "Application status updated to " + status + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scheduleInterview() {
        CandidateScreeningView view = selectedScreeningView();
        if (view == null) {
            JOptionPane.showMessageDialog(this, "Select an applicant first.");
            return;
        }
        try {
            interviewService.scheduleInterview(
                    view.candidate().application().applicationId(),
                    interviewTimeField.getText(),
                    interviewLocationField.getText(),
                    noteArea.getText(),
                    interviewLinkField.getText(),
                    currentUser.userId()
            );
            refreshCandidates();
            JOptionPane.showMessageDialog(this, "Interview invitation arranged.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Interview Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tagReferral() {
        CandidateScreeningView view = selectedScreeningView();
        if (view == null) {
            JOptionPane.showMessageDialog(this, "Select an applicant first.");
            return;
        }
        String recommenderName = JOptionPane.showInputDialog(this, "Recommender name:");
        if (recommenderName == null) {
            return;
        }
        try {
            candidateInsightService.tagInternalReferral(
                    view.candidate().application().userId(),
                    recommenderName,
                    noteArea.getText(),
                    currentUser.userId()
            );
            refreshCandidates();
            JOptionPane.showMessageDialog(this, "Internal referral saved.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Referral Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportSelectedPosition() {
        TAPosition selected = (TAPosition) positionCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a position first.");
            return;
        }
        try {
            Path exportPath = candidateInsightService.exportCandidates(selected.positionId(), Path.of("data", "exports"), currentUser.userId()).filePath();
            JOptionPane.showMessageDialog(this, "Candidate CSV exported to " + exportPath + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportResumesForSelectedPosition() {
        TAPosition selected = (TAPosition) positionCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a position first.");
            return;
        }
        try {
            Path exportPath = candidateInsightService.exportResumes(selected.positionId(), Path.of("data", "exports"), currentUser.userId()).filePath();
            JOptionPane.showMessageDialog(this, "Resume files exported to " + exportPath + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Resume Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewSelectedResume() {
        CandidateScreeningView view = selectedScreeningView();
        if (view == null) {
            JOptionPane.showMessageDialog(this, "Select an applicant first.");
            return;
        }
        Optional<ResumeFileRecord> resume = candidateInsightService.findResumeForApplication(view.candidate().application().applicationId());
        if (resume.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No resume uploaded for this application.");
            return;
        }
        Path resumePath = Path.of(resume.get().filePath());
        if (Files.notExists(resumePath)) {
            JOptionPane.showMessageDialog(this, "Resume file is missing: " + resumePath, "Resume Missing", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                JOptionPane.showMessageDialog(this, "Resume file path: " + resumePath);
                return;
            }
            Desktop.getDesktop().open(resumePath.toFile());
            auditLogService.record("DATA_ACCESS", currentUser.userId(), "Viewed candidate resume " + resumePath.getFileName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Open Resume Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFilters() {
        nameFilterField.setText("");
        studentIdFilterField.setText("");
        skillFilterField.setText("");
        yearFilterField.setText("");
        availabilityFilterField.setText("");
        minGpaField.setText("");
        maxGpaField.setText("");
        recommendedOnlyCheck.setSelected(false);
        experiencedOnlyCheck.setSelected(false);
        sortCombo.setSelectedItem(CandidateSortOption.RECOMMENDATION_SCORE);
        refreshCandidates();
    }

    private CandidateFilterCriteria buildCriteria() {
        return new CandidateFilterCriteria(
                nameFilterField.getText(),
                studentIdFilterField.getText(),
                parseDouble(minGpaField.getText()),
                parseDouble(maxGpaField.getText()),
                skillFilterField.getText(),
                yearFilterField.getText(),
                availabilityFilterField.getText(),
                recommendedOnlyCheck.isSelected(),
                experiencedOnlyCheck.isSelected()
        );
    }

    private CandidateRankingWeights buildWeights() {
        return new CandidateRankingWeights(
                (Integer) gpaWeightSpinner.getValue(),
                (Integer) experienceWeightSpinner.getValue(),
                (Integer) skillWeightSpinner.getValue(),
                (Integer) referralWeightSpinner.getValue(),
                (Integer) reputationWeightSpinner.getValue()
        );
    }

    private void updateActiveFilterLabel() {
        List<String> parts = new ArrayList<>();
        if (!nameFilterField.getText().isBlank()) {
            parts.add("name contains \"" + nameFilterField.getText().trim() + "\"");
        }
        if (!studentIdFilterField.getText().isBlank()) {
            parts.add("student ID contains \"" + studentIdFilterField.getText().trim() + "\"");
        }
        if (!skillFilterField.getText().isBlank()) {
            parts.add("skill contains \"" + skillFilterField.getText().trim() + "\"");
        }
        if (!yearFilterField.getText().isBlank()) {
            parts.add("year contains \"" + yearFilterField.getText().trim() + "\"");
        }
        if (!availabilityFilterField.getText().isBlank()) {
            parts.add("availability contains \"" + availabilityFilterField.getText().trim() + "\"");
        }
        if (parseDouble(minGpaField.getText()) != null) {
            parts.add("GPA >= " + minGpaField.getText().trim());
        }
        if (parseDouble(maxGpaField.getText()) != null) {
            parts.add("GPA <= " + maxGpaField.getText().trim());
        }
        if (recommendedOnlyCheck.isSelected()) {
            parts.add("internally recommended");
        }
        if (experiencedOnlyCheck.isSelected()) {
            parts.add("past TA experience");
        }
        activeFilterLabel.setText(parts.isEmpty() ? "Filters: None" : "Filters: " + String.join(" | ", parts));
    }

    private String referralLabel(CandidateReviewView candidate) {
        return candidate.referral()
                .filter(referral -> !referral.displayRecommenders().isBlank())
                .map(referral -> referral.displayRecommenders())
                .orElse("-");
    }

    private String resumeLabel(String applicationId) {
        return candidateInsightService.findResumeForApplication(applicationId)
                .map(ResumeFileRecord::autoFilename)
                .orElse("-");
    }

    private String interviewSummary(CandidateReviewView candidate) {
        return interviewService.findLatestInvitationForApplication(candidate.application().applicationId())
                .map(invitation -> {
                    String note = invitation.responseNote().isBlank() ? "-" : invitation.responseNote();
                    String link = invitation.onlineLink().isBlank() ? "" : " / Link: " + invitation.onlineLink();
                    return invitation.responseStatus().name() + " / " + note + link;
                })
                .orElse("No response yet");
    }

    private CandidateScreeningView selectedScreeningView() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentCandidates.size()) {
            return null;
        }
        return currentCandidates.get(row);
    }

    private Double parseDouble(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, int col, String label, java.awt.Component component) {
        gbc.gridx = col;
        gbc.gridy = row;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = col + 1;
        panel.add(component, gbc);
    }

    private void bindRefresh(JTextField... fields) {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshCandidates();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshCandidates();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshCandidates();
            }
        };
        for (JTextField field : fields) {
            field.getDocument().addDocumentListener(listener);
        }
    }
}
