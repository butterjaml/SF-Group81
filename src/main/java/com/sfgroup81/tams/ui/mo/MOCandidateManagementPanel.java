package com.sfgroup81.tams.ui.mo;

import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.ApplicationStatusHistory;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import com.sfgroup81.tams.service.ApplicationReviewService;
import com.sfgroup81.tams.service.CandidateInsightService;
import com.sfgroup81.tams.service.CandidateReviewView;
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
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MOCandidateManagementPanel extends JPanel {
    private final User currentUser;
    private final PositionCsvRepository positionRepository;
    private final ApplicationStatusHistoryCsvRepository historyRepository;
    private final ApplicationReviewService reviewService;
    private final InterviewService interviewService;
    private final CandidateInsightService candidateInsightService;

    private final JComboBox<TAPosition> positionCombo = new JComboBox<>();
    private final JCheckBox recommendedOnlyCheck = new JCheckBox("Internally Recommended Only");
    private final JTextField interviewTimeField = new JTextField("2026-04-01T10:00:00");
    private final JTextField interviewLocationField = new JTextField("Room 301");
    private final JTextField recommenderField = new JTextField();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Application", "Applicant", "Student ID", "Status", "Interview Reply", "Referral", "Reputation", "Major"}, 0
    );
    private final JTable table = new JTable(tableModel);
    private final JTextArea noteArea = new JTextArea(5, 20);
    private final JTextArea detailArea = new JTextArea();
    private List<CandidateReviewView> currentCandidates = List.of();

    public MOCandidateManagementPanel(User currentUser,
                                      PositionCsvRepository positionRepository,
                                      TAApplicationCsvRepository applicationRepository,
                                      ApplicationStatusHistoryCsvRepository historyRepository,
                                      ApplicantProfileCsvRepository profileRepository,
                                      UserCsvRepository userRepository,
                                      ApplicationReviewService reviewService,
                                      InterviewService interviewService,
                                      CandidateInsightService candidateInsightService,
                                      Runnable onBack) {
        this.currentUser = currentUser;
        this.positionRepository = positionRepository;
        this.historyRepository = historyRepository;
        this.reviewService = reviewService;
        this.interviewService = interviewService;
        this.candidateInsightService = candidateInsightService;

        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("MO candidates management", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadPositions();
        refreshApplications();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.add(new JLabel("Position:"));
        positionCombo.addActionListener(e -> refreshApplications());
        top.add(positionCombo);
        recommendedOnlyCheck.setOpaque(false);
        recommendedOnlyCheck.addActionListener(e -> refreshApplications());
        top.add(recommendedOnlyCheck);

        JButton exportButton = PrototypeUi.secondaryButton("Export CSV");
        exportButton.addActionListener(e -> exportSelectedPosition());
        top.add(exportButton);
        content.add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelectedDetails();
            }
        });
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel right = PrototypeUi.createVerticalCard();
        right.setLayout(new BorderLayout(8, 8));
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        right.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        right.add(new JScrollPane(noteArea), BorderLayout.SOUTH);

        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 8));
        actions.setOpaque(false);
        JPanel statusActions = new JPanel();
        statusActions.setOpaque(false);
        statusActions.add(createStatusButton("Pending", ApplicationStatus.PENDING_REVIEW));
        statusActions.add(createStatusButton("Hired", ApplicationStatus.HIRED));
        statusActions.add(createStatusButton("Rejected", ApplicationStatus.REJECTED));
        actions.add(statusActions);

        JPanel interviewRow = new JPanel(new GridLayout(0, 1, 6, 6));
        interviewRow.setOpaque(false);
        interviewRow.add(new JLabel("Interview time (ISO date-time):"));
        interviewRow.add(interviewTimeField);
        interviewRow.add(new JLabel("Interview location:"));
        interviewRow.add(interviewLocationField);
        JButton scheduleInterviewButton = PrototypeUi.primaryButton("Arrange Interview");
        scheduleInterviewButton.addActionListener(e -> scheduleInterview());
        interviewRow.add(scheduleInterviewButton);
        actions.add(interviewRow);

        JPanel referralRow = new JPanel(new GridLayout(0, 1, 6, 6));
        referralRow.setOpaque(false);
        referralRow.add(new JLabel("Recommender name:"));
        referralRow.add(recommenderField);
        JButton tagReferralButton = PrototypeUi.secondaryButton("Tag Internal Referral");
        tagReferralButton.addActionListener(e -> tagReferral());
        referralRow.add(tagReferralButton);
        actions.add(referralRow);

        right.add(actions, BorderLayout.NORTH);

        content.add(right, BorderLayout.EAST);
        return content;
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

    private void refreshApplications() {
        tableModel.setRowCount(0);
        TAPosition selected = (TAPosition) positionCombo.getSelectedItem();
        if (selected == null) {
            currentCandidates = List.of();
            detailArea.setText("No positions available.");
            return;
        }

        currentCandidates = candidateInsightService.listCandidatesForPosition(selected.positionId(), recommendedOnlyCheck.isSelected());
        for (CandidateReviewView candidate : currentCandidates) {
            tableModel.addRow(new Object[]{
                    candidate.application().applicationId(),
                    candidate.user() == null ? candidate.application().userId() : candidate.user().name(),
                    candidate.user() == null ? "-" : candidate.user().staffOrStudentId(),
                    candidate.application().status(),
                    interviewService.findLatestInvitationForApplication(candidate.application().applicationId())
                            .map(invitation -> invitation.responseStatus().name())
                            .orElse("-"),
                    candidate.referral().map(referral -> referral.recommenderName()).orElse("-"),
                    candidate.reputationScore(),
                    candidate.profile() == null ? "-" : candidate.profile().major()
            });
        }
        if (!currentCandidates.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            renderSelectedDetails();
        } else {
            detailArea.setText("No applicants match this filter.");
        }
    }

    private void renderSelectedDetails() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentCandidates.size()) {
            detailArea.setText("");
            return;
        }
        CandidateReviewView candidate = currentCandidates.get(row);
        List<ApplicationStatusHistory> history = historyRepository.findByApplicationId(candidate.application().applicationId());

        StringBuilder builder = new StringBuilder();
        builder.append("Applicant: ").append(candidate.user() == null ? candidate.application().userId() : candidate.user().name()).append('\n');
        builder.append("Student ID: ").append(candidate.user() == null ? "-" : candidate.user().staffOrStudentId()).append('\n');
        builder.append("Major / GPA: ").append(candidate.profile() == null ? "-" : candidate.profile().major()).append(" / ")
                .append(candidate.profile() == null ? "-" : candidate.profile().gpa()).append('\n');
        builder.append("Skills: ").append(candidate.profile() == null ? "-" : candidate.profile().skills()).append('\n');
        builder.append("Availability: ").append(candidate.profile() == null ? "-" : candidate.profile().availability()).append('\n');
        builder.append("Internal Referral: ").append(candidate.referral().map(referral -> referral.recommenderName()).orElse("No")).append('\n');
        builder.append("Interview Reply: ").append(interviewService.findLatestInvitationForApplication(candidate.application().applicationId())
                .map(invitation -> invitation.responseStatus().name() + " / " + (invitation.responseNote().isBlank() ? "-" : invitation.responseNote()))
                .orElse("No response yet")).append('\n');
        builder.append("Reputation Score: ").append(candidate.reputationScore()).append("\n\n");
        builder.append("History:\n");
        for (ApplicationStatusHistory item : history) {
            builder.append("- ").append(item.changedAt()).append(" | ").append(item.status()).append(" | ").append(item.note()).append('\n');
        }
        detailArea.setText(builder.toString());
        noteArea.setText(candidate.application().feedback());
        recommenderField.setText(candidate.referral().map(referral -> referral.recommenderName()).orElse(""));
    }

    private void updateSelectedStatus(ApplicationStatus status) {
        CandidateReviewView candidate = selectedCandidate();
        if (candidate == null) {
            JOptionPane.showMessageDialog(this, "Select an applicant first.");
            return;
        }
        try {
            reviewService.updateStatus(candidate.application().applicationId(), status, noteArea.getText(), currentUser.userId());
            refreshApplications();
            JOptionPane.showMessageDialog(this, "Application status updated to " + status + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scheduleInterview() {
        CandidateReviewView candidate = selectedCandidate();
        if (candidate == null) {
            JOptionPane.showMessageDialog(this, "Select an applicant first.");
            return;
        }
        try {
            interviewService.scheduleInterview(
                    candidate.application().applicationId(),
                    interviewTimeField.getText(),
                    interviewLocationField.getText(),
                    noteArea.getText(),
                    currentUser.userId()
            );
            refreshApplications();
            JOptionPane.showMessageDialog(this, "Interview invitation arranged.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Interview Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tagReferral() {
        CandidateReviewView candidate = selectedCandidate();
        if (candidate == null) {
            JOptionPane.showMessageDialog(this, "Select an applicant first.");
            return;
        }
        try {
            candidateInsightService.tagInternalReferral(
                    candidate.application().userId(),
                    recommenderField.getText(),
                    noteArea.getText(),
                    currentUser.userId()
            );
            refreshApplications();
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
            Path exportPath = candidateInsightService.exportCandidates(selected.positionId(), Path.of("data", "exports")).filePath();
            JOptionPane.showMessageDialog(this, "Candidate CSV exported to " + exportPath + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private CandidateReviewView selectedCandidate() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentCandidates.size()) {
            return null;
        }
        return currentCandidates.get(row);
    }
}
