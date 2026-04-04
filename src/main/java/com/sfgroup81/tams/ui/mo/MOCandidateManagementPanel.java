package com.sfgroup81.tams.ui.mo;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ApplicationStatus;
import com.sfgroup81.tams.model.ApplicationStatusHistory;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import com.sfgroup81.tams.service.ApplicationReviewService;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MOCandidateManagementPanel extends JPanel {
    private final User currentUser;
    private final PositionCsvRepository positionRepository;
    private final TAApplicationCsvRepository applicationRepository;
    private final ApplicationStatusHistoryCsvRepository historyRepository;
    private final ApplicantProfileCsvRepository profileRepository;
    private final UserCsvRepository userRepository;
    private final ApplicationReviewService reviewService;

    private final JComboBox<TAPosition> positionCombo = new JComboBox<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Application", "Applicant", "Student ID", "Priority", "Status", "Major", "GPA"}, 0
    );
    private final JTable table = new JTable(tableModel);
    private final JTextArea noteArea = new JTextArea(4, 20);
    private final JTextArea detailArea = new JTextArea();
    private List<TAApplication> currentApplications = List.of();

    public MOCandidateManagementPanel(User currentUser,
                                      PositionCsvRepository positionRepository,
                                      TAApplicationCsvRepository applicationRepository,
                                      ApplicationStatusHistoryCsvRepository historyRepository,
                                      ApplicantProfileCsvRepository profileRepository,
                                      UserCsvRepository userRepository,
                                      ApplicationReviewService reviewService,
                                      Runnable onBack) {
        this.currentUser = currentUser;
        this.positionRepository = positionRepository;
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.reviewService = reviewService;

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
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 36, 24, 36));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.add(new JLabel("Position:"));
        positionCombo.addActionListener(e -> refreshApplications());
        top.add(positionCombo);
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

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(createStatusButton("Pending", ApplicationStatus.PENDING_REVIEW));
        actions.add(createStatusButton("Interview", ApplicationStatus.INTERVIEW));
        actions.add(createStatusButton("Hired", ApplicationStatus.HIRED));
        actions.add(createStatusButton("Rejected", ApplicationStatus.REJECTED));
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
            currentApplications = List.of();
            detailArea.setText("No positions available.");
            return;
        }

        currentApplications = applicationRepository.findByPositionId(selected.positionId());
        for (TAApplication application : currentApplications) {
            Optional<User> user = userRepository.findAll().stream()
                    .filter(item -> item.userId().equals(application.userId()))
                    .findFirst();
            ApplicantProfile profile = profileRepository.findByUserId(application.userId()).orElse(null);
            tableModel.addRow(new Object[]{
                    application.applicationId(),
                    user.map(User::name).orElse(application.userId()),
                    user.map(User::staffOrStudentId).orElse("-"),
                    application.priorityNo(),
                    application.status(),
                    profile == null ? "-" : profile.major(),
                    profile == null ? "-" : profile.gpa()
            });
        }
        if (!currentApplications.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            renderSelectedDetails();
        } else {
            detailArea.setText("No applicants yet for this position.");
        }
    }

    private void renderSelectedDetails() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentApplications.size()) {
            detailArea.setText("");
            return;
        }
        TAApplication application = currentApplications.get(row);
        User user = userRepository.findAll().stream()
                .filter(item -> item.userId().equals(application.userId()))
                .findFirst()
                .orElse(null);
        ApplicantProfile profile = profileRepository.findByUserId(application.userId()).orElse(null);
        List<ApplicationStatusHistory> history = historyRepository.findByApplicationId(application.applicationId());

        StringBuilder builder = new StringBuilder();
        builder.append("Applicant: ").append(user == null ? application.userId() : user.name()).append('\n');
        builder.append("Student ID: ").append(user == null ? "-" : user.staffOrStudentId()).append('\n');
        builder.append("Major / GPA: ").append(profile == null ? "-" : profile.major()).append(" / ")
                .append(profile == null ? "-" : profile.gpa()).append('\n');
        builder.append("Skills: ").append(profile == null ? "-" : profile.skills()).append('\n');
        builder.append("Availability: ").append(profile == null ? "-" : profile.availability()).append("\n\n");
        builder.append("History:\n");
        for (ApplicationStatusHistory item : history) {
            builder.append("- ").append(item.changedAt()).append(" | ").append(item.status()).append(" | ").append(item.note()).append('\n');
        }
        detailArea.setText(builder.toString());
        noteArea.setText(application.feedback());
    }

    private void updateSelectedStatus(ApplicationStatus status) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentApplications.size()) {
            JOptionPane.showMessageDialog(this, "Select an applicant first.");
            return;
        }
        try {
            TAApplication application = currentApplications.get(row);
            reviewService.updateStatus(application.applicationId(), status, noteArea.getText(), currentUser.userId());
            refreshApplications();
            JOptionPane.showMessageDialog(this, "Application status updated to " + status + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
