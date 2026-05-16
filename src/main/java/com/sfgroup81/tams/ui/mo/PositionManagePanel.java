package com.sfgroup81.tams.ui.mo;

import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.service.PositionService;
import com.sfgroup81.tams.service.PositionUpsertRequest;
import com.sfgroup81.tams.service.SessionContext;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class PositionManagePanel extends JPanel {
    private final JTextField positionIdField = new JTextField();
    private final JTextField courseIdField = new JTextField();
    private final JTextField courseNameField = new JTextField();
    private final JTextField instructorField = new JTextField();
    private final JTextField semesterIdField = new JTextField();
    private final JTextField typeField = new JTextField();
    private final JTextField headcountField = new JTextField();
    private final JTextField deadlineField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JTextArea responsibilitiesArea = new JTextArea(4, 24);
    private final JTextArea hoursArea = new JTextArea(3, 24);
    private final JTextArea salaryArea = new JTextArea(3, 24);
    private final JTextArea mandatoryArea = new JTextArea(3, 24);
    private final JTextArea preferredArea = new JTextArea(3, 24);
    private final JTextArea bonusArea = new JTextArea(3, 24);
    private final JTextArea aiCriteriaArea = new JTextArea(4, 24);
    private final JTextArea previewArea = new JTextArea(12, 28);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Course", "Instructor", "Deadline", "Status", "Headcount"}, 0
    );
    private final JTable table = new JTable(tableModel);

    private final PositionService positionService;

    public PositionManagePanel() {
        this(new PositionService(new com.sfgroup81.tams.repository.PositionCsvRepository()), null);
    }

    public PositionManagePanel(PositionService positionService, Runnable onBack) {
        this.positionService = positionService;
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        configureTextAreas();
        add(PrototypeUi.createHeader("Job requirements", onBack), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JScrollPane formScrollPane = new JScrollPane(buildFormPanel());
        formScrollPane.setBorder(null);
        formScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        formScrollPane.setPreferredSize(new Dimension(430, 1));
        content.add(formScrollPane, BorderLayout.WEST);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(new JScrollPane(previewArea), BorderLayout.EAST);
        add(content, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelected();
            }
        });
        previewArea.setEditable(false);
        refreshTable();
    }

    private void configureTextAreas() {
        for (JTextArea area : List.of(
                responsibilitiesArea,
                hoursArea,
                salaryArea,
                mandatoryArea,
                preferredArea,
                bonusArea,
                aiCriteriaArea,
                previewArea
        )) {
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
        }
    }

    private JPanel buildFormPanel() {
        JPanel formPanel = PrototypeUi.createVerticalCard();
        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        addField(formPanel, gbc, row++, "Position ID", positionIdField);
        addField(formPanel, gbc, row++, "Course ID", courseIdField);
        addField(formPanel, gbc, row++, "Course Name", courseNameField);
        addField(formPanel, gbc, row++, "Instructor", instructorField);
        addField(formPanel, gbc, row++, "Semester", semesterIdField);
        addField(formPanel, gbc, row++, "Position Type", typeField);
        addField(formPanel, gbc, row++, "Headcount", headcountField);
        addField(formPanel, gbc, row++, "Deadline", deadlineField);
        addField(formPanel, gbc, row++, "Title", titleField);
        addField(formPanel, gbc, row++, "Responsibilities", areaScroll(responsibilitiesArea, 86));
        addField(formPanel, gbc, row++, "Working Hours", areaScroll(hoursArea, 68));
        addField(formPanel, gbc, row++, "Salary", areaScroll(salaryArea, 68));
        addField(formPanel, gbc, row++, "Mandatory requirements", areaScroll(mandatoryArea, 82));
        addField(formPanel, gbc, row++, "Preferred requirements", areaScroll(preferredArea, 82));
        addField(formPanel, gbc, row++, "Additional requirements", areaScroll(bonusArea, 82));
        addField(formPanel, gbc, row++, "AI skill weights", areaScroll(aiCriteriaArea, 86));

        JButton saveDraftButton = PrototypeUi.secondaryButton("Save Draft");
        saveDraftButton.addActionListener(e -> save("DRAFT"));
        JButton publishButton = PrototypeUi.primaryButton("Save and Publish");
        publishButton.addActionListener(e -> save("PUBLISHED"));
        JButton unpublishButton = PrototypeUi.secondaryButton("Unpublish Selected");
        unpublishButton.addActionListener(e -> unpublishSelected());

        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(saveDraftButton, gbc);
        gbc.gridx = 1;
        formPanel.add(publishButton, gbc);
        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.gridwidth = 2;
        formPanel.add(unpublishButton, gbc);
        return formPanel;
    }

    private JScrollPane areaScroll(JTextArea area, int height) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(260, height));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        return scrollPane;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    private void save(String status) {
        try {
            String operatorUserId = currentUserId().isBlank() ? "SYSTEM" : currentUserId();
            TAPosition saved = positionService.savePosition(new PositionUpsertRequest(
                    positionIdField.getText(),
                    courseIdField.getText(),
                    courseNameField.getText(),
                    instructorField.getText(),
                    semesterIdField.getText(),
                    typeField.getText(),
                    Integer.parseInt(headcountField.getText().trim()),
                    deadlineField.getText(),
                    status,
                    titleField.getText(),
                    responsibilitiesArea.getText(),
                    hoursArea.getText(),
                    salaryArea.getText(),
                    mandatoryArea.getText(),
                    preferredArea.getText(),
                    bonusArea.getText(),
                    aiCriteriaArea.getText(),
                    operatorUserId
            ), operatorUserId);
            positionIdField.setText(saved.positionId());
            JOptionPane.showMessageDialog(this, "Saved as " + status + " with ID " + saved.positionId());
            refreshTable();
            previewArea.setText(buildPreview(saved));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void unpublishSelected() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a position row first.");
            return;
        }
        String positionId = tableModel.getValueAt(selectedRow, 0).toString();
        try {
            positionService.unpublish(positionId, currentUserId());
            refreshTable();
            JOptionPane.showMessageDialog(this, "Position " + positionId + " is now UNPUBLISHED.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unpublish Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String currentUserId = currentUserId();
        List<TAPosition> positions = currentUserId.isBlank()
                ? positionService.listAll()
                : positionService.listByCreator(currentUserId);
        for (TAPosition p : positions) {
            tableModel.addRow(new Object[]{
                    p.positionId(), p.courseName(), p.instructorName(), p.deadline(), p.status(), p.headcount()
            });
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        String positionId = tableModel.getValueAt(row, 0).toString();
        TAPosition position = positionService.getById(positionId);
        positionIdField.setText(position.positionId());
        courseIdField.setText(position.courseId());
        courseNameField.setText(position.courseName());
        instructorField.setText(position.instructorName());
        semesterIdField.setText(position.semesterId());
        typeField.setText(position.positionType());
        headcountField.setText(Integer.toString(position.headcount()));
        deadlineField.setText(position.deadline());
        titleField.setText(position.title());
        responsibilitiesArea.setText(position.responsibilities());
        hoursArea.setText(position.workingHours());
        salaryArea.setText(position.salaryInfo());
        mandatoryArea.setText(position.mandatoryRequirements());
        preferredArea.setText(position.preferredRequirements());
        bonusArea.setText(position.bonusRequirements());
        aiCriteriaArea.setText(position.aiScreeningCriteria());
        previewArea.setText(buildPreview(position));
    }

    private String buildPreview(TAPosition position) {
        return """
                Title: %s
                Course: %s (%s)
                Instructor: %s
                Type: %s
                Hours: %s
                Salary: %s

                Responsibilities:
                %s

                Mandatory:
                %s

                Preferred:
                %s

                Additional:
                %s

                AI Screening Skill Weights:
                %s
                """.formatted(
                position.title(),
                position.courseName(),
                position.courseId(),
                position.instructorName(),
                position.positionType(),
                position.workingHours(),
                position.salaryInfo(),
                position.responsibilities(),
                formatRequirementBlock(position.mandatoryRequirements()),
                formatRequirementBlock(position.preferredRequirements()),
                formatRequirementBlock(position.bonusRequirements()),
                formatRequirementBlock(position.aiScreeningCriteria())
        );
    }

    private String formatRequirementBlock(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String[] lines = value.split("\\R");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(" - ").append(trimmed);
        }
        return builder.isEmpty() ? "-" : builder.toString();
    }

    private String currentUserId() {
        return SessionContext.getCurrentUser() == null ? "" : SessionContext.getCurrentUser().userId();
    }
}
