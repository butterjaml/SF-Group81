package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.ApplicationStatusHistory;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.service.ApplicantApplicationView;
import com.sfgroup81.tams.service.ApplicationStatusService;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

public class TAApplicationStatusPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Application", "Course", "Priority", "Status", "Feedback"}, 0
    );
    private final JTable table = new JTable(model);
    private final JTextArea detailsArea = new JTextArea();
    private List<ApplicantApplicationView> views = List.of();

    public TAApplicationStatusPanel(User currentUser, ApplicationStatusService statusService, Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("TA Application Details/Progress", onBack), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        views = statusService.listForApplicant(currentUser.userId());
        populateTable();

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelectedDetails();
            }
        });

        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setPreferredSize(new Dimension(420, 0));
        content.add(detailsScroll, BorderLayout.EAST);

        if (!views.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            renderSelectedDetails();
        } else {
            content.add(new JLabel("No submitted TA applications yet.", JLabel.CENTER), BorderLayout.NORTH);
        }

        add(content, BorderLayout.CENTER);
    }

    private void populateTable() {
        model.setRowCount(0);
        for (ApplicantApplicationView view : views) {
            model.addRow(new Object[]{
                    view.application().applicationId(),
                    view.position() == null ? view.application().positionId() : view.position().courseName(),
                    view.application().priorityNo(),
                    view.application().status(),
                    view.application().feedback()
            });
        }
    }

    private void renderSelectedDetails() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= views.size()) {
            detailsArea.setText("");
            return;
        }
        ApplicantApplicationView view = views.get(row);
        StringBuilder builder = new StringBuilder();
        builder.append("Position: ").append(view.position() == null ? view.application().positionId() : view.position().title()).append('\n');
        builder.append("Status: ").append(view.application().status()).append('\n');
        builder.append("Feedback: ").append(view.application().feedback()).append("\n\n");
        builder.append("Progress Record:\n");
        for (ApplicationStatusHistory history : view.history()) {
            builder.append("- ")
                    .append(history.changedAt())
                    .append(" | ")
                    .append(history.status())
                    .append(" | ")
                    .append(history.note())
                    .append('\n');
        }
        detailsArea.setText(builder.toString());
    }
}
