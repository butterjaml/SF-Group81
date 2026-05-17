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
import com.sfgroup81.tams.service.CandidateReviewView;
import com.sfgroup81.tams.service.CandidateScreeningService;
import com.sfgroup81.tams.service.CandidateScreeningView;
import com.sfgroup81.tams.service.CandidateSortOption;
import com.sfgroup81.tams.service.InterviewService;
import com.sfgroup81.tams.service.SemesterService;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
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
    private final SemesterService semesterService;

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
    private final JLabel activeFilterLabel = new JLabel("Filters: None");
    private final JTextArea aiReferenceArea = new JTextArea(10, 24);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Application", "Applicant", "Student ID", "AI Match", "GPA", "Experience", "Referral", "Status", "Applied At"}, 0
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
    private SwingWorker<List<CandidateScreeningView>, Void> refreshWorker;
    private boolean refreshQueued;
    private boolean suppressRefresh;
    private JDialog loadingDialog;

    public MOCandidateManagementPanel(User currentUser,
                                      PositionCsvRepository positionRepository,
                                      ApplicationStatusHistoryCsvRepository historyRepository,
                                      ApplicationReviewService reviewService,
                                      InterviewService interviewService,
                                      CandidateInsightService candidateInsightService,
                                      CandidateScreeningService candidateScreeningService,
                                      AuditLogService auditLogService,
                                      SemesterService semesterService,
                                      Runnable onBack) {
        this.currentUser = currentUser;
        this.positionRepository = positionRepository;
        this.historyRepository = historyRepository;
        this.reviewService = reviewService;
        this.interviewService = interviewService;
        this.candidateInsightService = candidateInsightService;
        this.candidateScreeningService = candidateScreeningService;
        this.auditLogService = auditLogService;
        this.semesterService = semesterService;

        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("Candidate Details/Comparison", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadPositions();
        SwingUtilities.invokeLater(this::refreshCandidates);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        content.add(buildFilterCard(), BorderLayout.NORTH);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTableSection(), buildRightPanel());
        horizontalSplit.setBorder(null);
        horizontalSplit.setContinuousLayout(true);
        horizontalSplit.setOneTouchExpandable(true);
        horizontalSplit.setResizeWeight(0.68);
        horizontalSplit.setDividerLocation(0.68);
        horizontalSplit.setMinimumSize(new Dimension(0, 260));

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontalSplit, buildComparisonSection());
        verticalSplit.setBorder(null);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setOneTouchExpandable(true);
        verticalSplit.setResizeWeight(0.72);
        verticalSplit.setDividerLocation(0.72);
        verticalSplit.setMinimumSize(new Dimension(0, 0));
        content.add(verticalSplit, BorderLayout.CENTER);
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

    private JPanel buildAiReferencePanel() {
        JPanel card = PrototypeUi.createCard();
        card.setLayout(new BorderLayout(8, 8));
        card.add(PrototypeUi.sectionTitle("AI Skill Reference"), BorderLayout.NORTH);
        aiReferenceArea.setEditable(false);
        aiReferenceArea.setLineWrap(true);
        aiReferenceArea.setWrapStyleWord(true);
        card.add(sizedScrollPane(aiReferenceArea, 92), BorderLayout.CENTER);
        JButton rerunButton = PrototypeUi.primaryButton("Refresh AI Screening");
        rerunButton.addActionListener(e -> refreshCandidates());
        card.add(rerunButton, BorderLayout.SOUTH);
        constrainHeight(card);
        return card;
    }

    private JPanel buildTableSection() {
        JPanel section = new JPanel(new BorderLayout(12, 12));
        section.setOpaque(false);

        JPanel actions = new JPanel(new GridLayout(1, 4, 8, 0));
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(buildStatusActions());
        stack.add(Box.createVerticalStrut(10));
        stack.add(buildAiReferencePanel());
        stack.add(Box.createVerticalStrut(10));
        stack.add(sizedScrollPane(detailArea, 160));
        stack.add(Box.createVerticalStrut(10));
        stack.add(sizedScrollPane(noteArea, 96));
        stack.add(Box.createVerticalStrut(10));
        stack.add(buildInterviewPanel());
        stack.add(Box.createVerticalStrut(10));
        stack.add(buildReferralPanel());

        JScrollPane scrollPane = new JScrollPane(stack);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        right.add(scrollPane, BorderLayout.CENTER);
        return right;
    }

    private JPanel buildStatusActions() {
        JPanel statusActions = PrototypeUi.createCard();
        statusActions.setLayout(new GridLayout(1, 3, 8, 0));
        statusActions.add(createStatusButton("Pending", ApplicationStatus.PENDING_REVIEW));
        statusActions.add(createStatusButton("Hired", ApplicationStatus.HIRED));
        statusActions.add(createStatusButton("Rejected", ApplicationStatus.REJECTED));
        constrainHeight(statusActions);
        return statusActions;
    }

    private JPanel buildInterviewPanel() {
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
        constrainHeight(interviewRow);
        return interviewRow;
    }

    private JPanel buildReferralPanel() {
        JPanel referralRow = PrototypeUi.createCard();
        referralRow.setLayout(new GridLayout(0, 1, 6, 6));
        referralRow.add(new JLabel("Internal referrals"));
        referralRow.add(sizedScrollPane(referralSummaryArea, 72));
        JButton tagReferralButton = PrototypeUi.secondaryButton("Add Internal Referral");
        tagReferralButton.addActionListener(e -> tagReferral());
        referralRow.add(tagReferralButton);
        constrainHeight(referralRow);
        return referralRow;
    }

    private JScrollPane sizedScrollPane(Component component, int height) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setPreferredSize(new Dimension(318, height));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        return scrollPane;
    }

    private void constrainHeight(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
    }

    private JPanel buildComparisonSection() {
        JPanel section = new JPanel(new BorderLayout(12, 12));
        section.setOpaque(false);

        JLabel title = PrototypeUi.sectionTitle("Candidate comparison");
        section.add(title, BorderLayout.NORTH);

        comparisonGrid.setOpaque(false);
        JScrollPane comparisonScroll = new JScrollPane(comparisonGrid);
        comparisonScroll.setPreferredSize(new Dimension(0, 190));
        comparisonScroll.getVerticalScrollBar().setUnitIncrement(16);
        section.add(comparisonScroll, BorderLayout.CENTER);
        section.setMinimumSize(new Dimension(0, 120));
        return section;
    }

    private JButton createStatusButton(String text, ApplicationStatus status) {
        JButton button = PrototypeUi.primaryButton(text);
        button.addActionListener(e -> updateSelectedStatus(status));
        return button;
    }

    private void loadPositions() {
        List<TAPosition> positions = new ArrayList<>(positionRepository.findAll().stream()
                .filter(position -> semesterService == null || semesterService.matchesViewedSemester(position.semesterId()))
                .filter(position -> currentUser != null && position.createdBy().equals(currentUser.userId()))
                .toList());
        suppressRefresh = true;
        try {
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
        } finally {
            suppressRefresh = false;
        }
    }

    private void refreshCandidates() {
        if (suppressRefresh) {
            return;
        }
        if (refreshWorker != null && !refreshWorker.isDone()) {
            refreshQueued = true;
            return;
        }
        tableModel.setRowCount(0);
        TAPosition selected = (TAPosition) positionCombo.getSelectedItem();
        if (selected == null) {
            currentCandidates = List.of();
            detailArea.setText("No positions available.");
            comparisonGrid.removeAll();
            return;
        }

        CandidateFilterCriteria criteria = buildCriteria();
        CandidateSortOption sortOption = (CandidateSortOption) sortCombo.getSelectedItem();
        aiReferenceArea.setText(formatAiReference(selected));
        detailArea.setText("AI is thinking. Candidate recommendations are loading...");
        noteArea.setText("");
        referralSummaryArea.setText("");
        comparisonGrid.removeAll();
        comparisonGrid.revalidate();
        comparisonGrid.repaint();
        showLoadingDialog();

        refreshWorker = new SwingWorker<>() {
            @Override
            protected List<CandidateScreeningView> doInBackground() {
                return candidateScreeningService.screenCandidates(
                        selected,
                        criteria,
                        null,
                        sortOption
                );
            }

            @Override
            protected void done() {
                try {
                    applyCandidateRows(get());
                } catch (Exception ex) {
                    currentCandidates = List.of();
                    detailArea.setText("Candidate recommendations could not be loaded.");
                    JOptionPane.showMessageDialog(MOCandidateManagementPanel.this,
                            ex.getMessage(),
                            "Candidate Screening Failed",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    hideLoadingDialog();
                    refreshWorker = null;
                    if (refreshQueued) {
                        refreshQueued = false;
                        refreshCandidates();
                    }
                }
            }
        };
        refreshWorker.execute();
    }

    private void applyCandidateRows(List<CandidateScreeningView> candidates) {
        currentCandidates = candidates == null ? List.of() : candidates;
        tableModel.setRowCount(0);
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

    private void showLoadingDialog() {
        if (!isShowing()) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) {
            return;
        }
        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }
        JDialog dialog = new JDialog(owner, "AI Screening", Dialog.ModalityType.MODELESS);
        JPanel panel = PrototypeUi.createVerticalCard();
        panel.add(PrototypeUi.sectionTitle("AI is thinking"));
        PrototypeUi.addVerticalGap(panel, 10);
        panel.add(new JLabel("Generating candidate recommendations. Please wait..."));
        PrototypeUi.addVerticalGap(panel, 12);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(progressBar);
        dialog.setContentPane(panel);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        loadingDialog = dialog;
        dialog.setVisible(true);
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dispose();
            loadingDialog = null;
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
        builder.append("AI Match Score: ").append(String.format("%.2f", view.recommendationScore())).append('\n');
        builder.append("GPA: ").append(String.format("%.2f", view.gpaValue())).append('\n');
        builder.append("Past TA Experience: ").append(view.hasPastTaExperience() ? "YES" : "NO").append('\n');
        builder.append("AI Skill Match: ").append(String.format("%.0f%%", view.skillMatchScore() * 100)).append('\n');
        builder.append("Matched Skills: ").append(view.matchedRequirementKeywords().isEmpty() ? "-" : String.join("; ", view.matchedRequirementKeywords())).append('\n');
        builder.append("Missing Skills: ").append(view.missingRequirementKeywords().isEmpty() ? "-" : String.join("; ", view.missingRequirementKeywords())).append('\n');
        builder.append("AI Summary: ").append(view.aiSummary().isBlank() ? "-" : view.aiSummary()).append('\n');
        builder.append("Strengths: ").append(view.strengths().isBlank() ? "-" : view.strengths()).append('\n');
        builder.append("Risks: ").append(view.risks().isBlank() ? "-" : view.risks()).append('\n');
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
        card.add(new JLabel("AI Match: " + String.format("%.2f", view.recommendationScore())));
        card.add(new JLabel("GPA: " + String.format("%.2f", view.gpaValue())));
        card.add(new JLabel("Experience: " + (view.hasPastTaExperience() ? "YES" : "NO")));
        card.add(new JLabel("Major: " + (profile == null ? "-" : profile.major())));
        card.add(new JLabel("Availability: " + (profile == null ? "-" : profile.availability())));
        JTextArea summary = new JTextArea(
                "AI summary: " + (view.aiSummary().isBlank() ? "-" : view.aiSummary()) + "\n"
                        + "Matched skills: " + (view.matchedRequirementKeywords().isEmpty() ? "-" : String.join("; ", view.matchedRequirementKeywords())) + "\n"
                        + "Missing skills: " + (view.missingRequirementKeywords().isEmpty() ? "-" : String.join("; ", view.missingRequirementKeywords())) + "\n"
                        + "Referral: " + referralLabel(view.candidate())
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

    private String formatAiReference(TAPosition position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Semester: ").append(position.semesterId()).append('\n');
        builder.append("Weighted skills: ").append(position.aiScreeningCriteria() == null || position.aiScreeningCriteria().isBlank()
                ? "Not configured. The system falls back to the listed job requirements."
                : position.aiScreeningCriteria()).append("\n\n");
        builder.append("Mandatory: ").append(position.mandatoryRequirements().isBlank() ? "-" : position.mandatoryRequirements()).append("\n\n");
        builder.append("Preferred: ").append(position.preferredRequirements().isBlank() ? "-" : position.preferredRequirements()).append("\n\n");
        builder.append("Bonus: ").append(position.bonusRequirements().isBlank() ? "-" : position.bonusRequirements());
        return builder.toString();
    }
}
