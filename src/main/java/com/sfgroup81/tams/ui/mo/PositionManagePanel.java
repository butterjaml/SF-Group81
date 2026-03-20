package com.sfgroup81.tams.ui.mo;

import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.service.PositionService;
import com.sfgroup81.tams.service.SessionContext;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

public class PositionManagePanel extends JPanel {
    private final JTextField positionIdField = new JTextField();
    private final JTextField courseIdField = new JTextField();
    private final JTextField semesterIdField = new JTextField();
    private final JTextField typeField = new JTextField();
    private final JTextField headcountField = new JTextField();
    private final JTextField deadlineField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JTextField descriptionField = new JTextField();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Course", "Semester", "Type", "Headcount", "Deadline", "Status", "Title"}, 0
    );
    private final JTable table = new JTable(tableModel);

    private final PositionService positionService = new PositionService(new PositionCsvRepository());

    public PositionManagePanel() {
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(10, 2, 8, 8));
        formPanel.add(new JLabel("Position ID (blank=create):"));
        formPanel.add(positionIdField);
        formPanel.add(new JLabel("Course ID:"));
        formPanel.add(courseIdField);
        formPanel.add(new JLabel("Semester ID:"));
        formPanel.add(semesterIdField);
        formPanel.add(new JLabel("Position Type:"));
        formPanel.add(typeField);
        formPanel.add(new JLabel("Headcount:"));
        formPanel.add(headcountField);
        formPanel.add(new JLabel("Deadline (yyyy-MM-dd):"));
        formPanel.add(deadlineField);
        formPanel.add(new JLabel("Title:"));
        formPanel.add(titleField);
        formPanel.add(new JLabel("Description:"));
        formPanel.add(descriptionField);

        JButton saveDraftButton = new JButton("Save Draft");
        saveDraftButton.addActionListener(e -> save("DRAFT"));
        JButton publishButton = new JButton("Publish");
        publishButton.addActionListener(e -> save("PUBLISHED"));
        formPanel.add(saveDraftButton);
        formPanel.add(publishButton);

        JButton unpublishButton = new JButton("Unpublish Selected");
        unpublishButton.addActionListener(e -> unpublishSelected());
        JButton refreshButton = new JButton("Refresh Status");
        refreshButton.addActionListener(e -> refreshTable());
        formPanel.add(unpublishButton);
        formPanel.add(refreshButton);

        add(formPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshTable();
    }

    private void save(String status) {
        try {
            TAPosition saved = positionService.savePosition(
                    positionIdField.getText(),
                    courseIdField.getText(),
                    semesterIdField.getText(),
                    typeField.getText(),
                    Integer.parseInt(headcountField.getText().trim()),
                    deadlineField.getText(),
                    titleField.getText(),
                    descriptionField.getText(),
                    SessionContext.getCurrentUser() == null ? "SYSTEM" : SessionContext.getCurrentUser().userId(),
                    status
            );
            positionIdField.setText(saved.positionId());
            JOptionPane.showMessageDialog(this, "Saved as " + status + " with ID " + saved.positionId());
            refreshTable();
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
            positionService.unpublish(positionId);
            refreshTable();
            JOptionPane.showMessageDialog(this, "Position " + positionId + " is now UNPUBLISHED.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unpublish Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<TAPosition> positions = positionService.listAll();
        for (TAPosition p : positions) {
            tableModel.addRow(new Object[]{
                    p.positionId(), p.courseId(), p.semesterId(), p.positionType(), p.headcount(), p.deadline(), p.status(), p.title()
            });
        }
    }
}
