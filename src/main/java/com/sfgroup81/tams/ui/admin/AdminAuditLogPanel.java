package com.sfgroup81.tams.ui.admin;

import com.sfgroup81.tams.model.AuditLogEntry;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.service.AuditLogFilter;
import com.sfgroup81.tams.service.AuditLogService;
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
import java.time.LocalDate;
import java.util.List;

public class AdminAuditLogPanel extends JPanel {
    private final User currentUser;
    private final AuditLogService auditLogService;

    private final JTextField fromDateField = new JTextField(10);
    private final JTextField toDateField = new JTextField(10);
    private final JTextField userField = new JTextField(12);
    private final JComboBox<String> eventTypeCombo = new JComboBox<>(new String[]{
            "", "LOGIN_SUCCESS", "REGISTRATION", "USER_CREATED", "ROLE_CHANGED", "ACCOUNT_STATUS_CHANGED",
            "PASSWORD_RESET", "ENROLLMENT_SUBMITTED", "APPLICATION_STATUS_CHANGED", "INTERVIEW_SCHEDULED",
            "INTERVIEW_RESPONSE_UPDATED", "CASUAL_WORK_POSTED", "CASUAL_WORK_APPLIED", "POSITION_CREATED",
            "POSITION_UPDATED", "POSITION_UNPUBLISHED", "CANDIDATE_EXPORT", "RESUME_EXPORT",
            "RANKED_CANDIDATE_EXPORT", "USER_MANAGEMENT_VIEWED", "AUDIT_LOG_VIEWED", "TA_FEEDBACK_SUBMITTED",
            "DATA_ACCESS", "REFERRAL_TAGGED", "SEMESTER_CREATED", "SEMESTER_VIEW_SWITCHED",
            "AI_SCREENING_RUN"
    });
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Time", "Event", "User", "IP Address", "Details"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextArea detailArea = new JTextArea();
    private List<AuditLogEntry> entries = List.of();

    public AdminAuditLogPanel(User currentUser, AuditLogService auditLogService, Runnable onBack) {
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;

        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("Audit log", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        auditLogService.record("AUDIT_LOG_VIEWED", currentUser.userId(), "Opened audit log page");
        refreshTable();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        content.add(buildFilters(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelected();
            }
        });
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel detailCard = PrototypeUi.createVerticalCard();
        detailCard.setPreferredSize(new Dimension(320, 0));
        detailCard.add(PrototypeUi.sectionTitle("Details"));
        PrototypeUi.addVerticalGap(detailCard, 12);
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailCard.add(new JScrollPane(detailArea));
        content.add(detailCard, BorderLayout.EAST);
        return content;
    }

    private JPanel buildFilters() {
        JPanel filters = PrototypeUi.createCard();
        filters.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addField(filters, gbc, 0, "From", fromDateField);
        addField(filters, gbc, 1, "To", toDateField);
        addField(filters, gbc, 2, "User", userField);
        addField(filters, gbc, 3, "Event", eventTypeCombo);

        JButton applyButton = PrototypeUi.primaryButton("Apply Filters");
        applyButton.addActionListener(e -> refreshTable());
        gbc.gridx = 8;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        filters.add(applyButton, gbc);

        JButton clearButton = PrototypeUi.secondaryButton("Clear");
        clearButton.addActionListener(e -> clearFilters());
        gbc.gridx = 9;
        filters.add(clearButton, gbc);
        return filters;
    }

    private void refreshTable() {
        try {
            entries = auditLogService.listEntries(new AuditLogFilter(
                    parseDate(fromDateField.getText()),
                    parseDate(toDateField.getText()),
                    userField.getText(),
                    (String) eventTypeCombo.getSelectedItem()
            ));
            tableModel.setRowCount(0);
            for (AuditLogEntry entry : entries) {
                tableModel.addRow(new Object[]{
                        entry.eventTime(),
                        entry.eventType(),
                        entry.userDisplay(),
                        entry.ipAddress(),
                        entry.details()
                });
            }
            if (!entries.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
                renderSelected();
            } else {
                detailArea.setText("No audit events match the selected filters.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Filter Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= entries.size()) {
            detailArea.setText("");
            return;
        }
        AuditLogEntry entry = entries.get(row);
        detailArea.setText("""
                Event: %s
                User: %s
                Time: %s
                IP Address: %s

                Details:
                %s
                """.formatted(
                entry.eventType(),
                entry.userDisplay(),
                entry.eventTime(),
                entry.ipAddress(),
                entry.details().isBlank() ? "-" : entry.details()
        ));
    }

    private void clearFilters() {
        fromDateField.setText("");
        toDateField.setText("");
        userField.setText("");
        eventTypeCombo.setSelectedItem("");
        refreshTable();
    }

    private LocalDate parseDate(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Date must use YYYY-MM-DD format, for example 2026-05-03");
        }
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int column, String label, java.awt.Component component) {
        gbc.gridx = column * 2;
        gbc.gridy = 0;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = column * 2 + 1;
        panel.add(component, gbc);
    }
}
