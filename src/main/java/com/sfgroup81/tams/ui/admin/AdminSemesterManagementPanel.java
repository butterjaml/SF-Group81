package com.sfgroup81.tams.ui.admin;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.SemesterRecord;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.service.SemesterService;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class AdminSemesterManagementPanel extends JPanel {
    private final User currentUser;
    private final SemesterService semesterService;
    private final List<TAPosition> allPositions;
    private final List<TAApplication> allApplications;
    private final List<ApplicantProfile> allProfiles;

    private final JTextField newSemesterField = new JTextField(10);
    private final JTextField notesField = new JTextField(20);
    private final JComboBox<String> switchSemesterCombo = new JComboBox<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Semester", "Role", "Archived", "Positions", "Applications", "Profiles"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextArea detailArea = new JTextArea();
    private List<SemesterRecord> semesters = List.of();

    public AdminSemesterManagementPanel(User currentUser,
                                        SemesterService semesterService,
                                        List<TAPosition> allPositions,
                                        List<TAApplication> allApplications,
                                        List<ApplicantProfile> allProfiles,
                                        Runnable onBack) {
        this.currentUser = currentUser;
        this.semesterService = semesterService;
        this.allPositions = new ArrayList<>(allPositions);
        this.allApplications = new ArrayList<>(allApplications);
        this.allProfiles = new ArrayList<>(allProfiles);

        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("Semester archive", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshTable();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        content.add(buildControlCard(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelected();
            }
        });
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel detailCard = PrototypeUi.createVerticalCard();
        detailCard.setPreferredSize(new Dimension(320, 0));
        detailCard.add(PrototypeUi.sectionTitle("Semester detail"));
        PrototypeUi.addVerticalGap(detailCard, 12);
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailCard.add(new JScrollPane(detailArea));
        content.add(detailCard, BorderLayout.EAST);
        return content;
    }

    private JPanel buildControlCard() {
        JPanel card = PrototypeUi.createCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addField(card, gbc, 0, 0, "New semester", newSemesterField);
        addField(card, gbc, 0, 2, "Notes", notesField);
        JButton createButton = PrototypeUi.primaryButton("Archive Current and Start New");
        createButton.addActionListener(e -> createSemester());
        gbc.gridx = 4;
        gbc.gridy = 0;
        card.add(createButton, gbc);

        addField(card, gbc, 1, 0, "View semester", switchSemesterCombo);
        JButton switchButton = PrototypeUi.secondaryButton("Switch View");
        switchButton.addActionListener(e -> switchView());
        gbc.gridx = 4;
        gbc.gridy = 1;
        card.add(switchButton, gbc);
        return card;
    }

    private void refreshTable() {
        semesters = semesterService.listSemesters();
        tableModel.setRowCount(0);
        switchSemesterCombo.removeAllItems();
        for (SemesterRecord semester : semesters) {
            switchSemesterCombo.addItem(semester.semesterId());
            tableModel.addRow(new Object[]{
                    semester.semesterId(),
                    semester.currentSemester() ? "Current" : semester.viewedSemester() ? "Viewed" : "-",
                    semester.archived() ? "YES" : "NO",
                    countPositions(semester.semesterId()),
                    countApplications(semester.semesterId()),
                    countProfiles(semester.semesterId())
            });
        }
        if (!semesters.isEmpty()) {
            switchSemesterCombo.setSelectedItem(semesterService.viewedSemesterId());
            table.setRowSelectionInterval(0, 0);
            renderSelected();
        } else {
            detailArea.setText("No semester records found.");
        }
    }

    private void createSemester() {
        try {
            semesterService.createAndSwitchToNewSemester(newSemesterField.getText(), currentUser.userId(), notesField.getText());
            newSemesterField.setText("");
            notesField.setText("");
            refreshTable();
            JOptionPane.showMessageDialog(this, "Semester archive created and current view switched.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Semester Create Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void switchView() {
        Object selected = switchSemesterCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a semester first.");
            return;
        }
        try {
            semesterService.switchViewedSemester(selected.toString(), currentUser.userId());
            refreshTable();
            JOptionPane.showMessageDialog(this, "Semester view updated. Other pages now use " + selected + " by default.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Switch Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= semesters.size()) {
            detailArea.setText("");
            return;
        }
        SemesterRecord semester = semesters.get(row);
        detailArea.setText("""
                Semester: %s
                Current semester: %s
                Viewed semester: %s
                Archived: %s
                Created by: %s
                Created at: %s
                Archived at: %s

                Recruitment snapshot:
                Positions: %d
                Applications: %d
                Profiles: %d

                Notes:
                %s
                """.formatted(
                semester.semesterId(),
                semester.currentSemester() ? "YES" : "NO",
                semester.viewedSemester() ? "YES" : "NO",
                semester.archived() ? "YES" : "NO",
                semester.createdBy().isBlank() ? "-" : semester.createdBy(),
                semester.createdAt().isBlank() ? "-" : semester.createdAt(),
                semester.archivedAt().isBlank() ? "-" : semester.archivedAt(),
                countPositions(semester.semesterId()),
                countApplications(semester.semesterId()),
                countProfiles(semester.semesterId()),
                semester.notes().isBlank() ? "-" : semester.notes()
        ));
    }

    private int countPositions(String semesterId) {
        return (int) allPositions.stream()
                .filter(item -> item.semesterId().equalsIgnoreCase(semesterId))
                .count();
    }

    private int countApplications(String semesterId) {
        return (int) allApplications.stream()
                .filter(item -> item.semesterId().equalsIgnoreCase(semesterId))
                .count();
    }

    private int countProfiles(String semesterId) {
        return (int) allProfiles.stream()
                .filter(item -> item.semesterId().equalsIgnoreCase(semesterId))
                .count();
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, int col, String label, java.awt.Component component) {
        gbc.gridx = col;
        gbc.gridy = row;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = col + 1;
        panel.add(component, gbc);
    }
}
