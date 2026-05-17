package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.CasualWorkPosting;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.service.CasualWorkService;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

public class CasualWorkBrowserPanel extends JPanel {
    private final User currentUser;
    private final CasualWorkService casualWorkService;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Posting", "Title", "Date", "Location", "Compensation"}, 0
    );
    private final JTable table = new JTable(tableModel);
    private final JTextArea detailsArea = new JTextArea();
    private final JTextArea statementArea = new JTextArea(4, 24);
    private final boolean canApply;
    private List<CasualWorkPosting> postings = List.of();

    public CasualWorkBrowserPanel(User currentUser, CasualWorkService casualWorkService, Runnable onBack) {
        this.currentUser = currentUser;
        this.casualWorkService = casualWorkService;
        this.canApply = currentUser != null && casualWorkService.canApplyCasualWork(currentUser.userId());
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("Temporary work", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshPostings();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        JPanel hint = PrototypeUi.createVerticalCard();
        hint.add(PrototypeUi.sectionTitle("Quick Apply"));
        PrototypeUi.addVerticalGap(hint, 6);
        hint.add(new JLabel(canApply
                ? "You can apply directly, or leave a short statement for the Admin."
                : "Open postings are visible. Applications are limited to TAs hired for the current semester."));
        content.add(hint, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelected();
            }
        });
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel right = PrototypeUi.createVerticalCard();
        right.setLayout(new BorderLayout(8, 8));
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        statementArea.setLineWrap(true);
        statementArea.setWrapStyleWord(true);
        right.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        right.add(new JScrollPane(statementArea), BorderLayout.SOUTH);

        JButton applyButton = PrototypeUi.primaryButton("Apply");
        applyButton.addActionListener(e -> applyForSelectedPosting());
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(applyButton);
        right.add(actions, BorderLayout.NORTH);

        content.add(right, BorderLayout.EAST);
        return content;
    }

    private void refreshPostings() {
        postings = casualWorkService.listOpenPostings();
        tableModel.setRowCount(0);
        for (CasualWorkPosting posting : postings) {
            tableModel.addRow(new Object[]{
                    posting.postingId(),
                    posting.title(),
                    posting.workDate(),
                    posting.location(),
                    posting.compensation()
            });
        }
        if (!postings.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            renderSelected();
        } else {
            detailsArea.setText("No temporary work postings are available.");
            statementArea.setText("");
        }
    }

    private void renderSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= postings.size()) {
            detailsArea.setText("");
            return;
        }
        CasualWorkPosting posting = postings.get(row);
        detailsArea.setText("""
                %s

                Date: %s
                Location: %s
                Required skills: %s
                Slots: %s
                Compensation: %s

                Description:
                %s
                """.formatted(
                posting.title(),
                posting.workDate(),
                posting.location(),
                posting.requiredSkills().isBlank() ? "-" : posting.requiredSkills(),
                posting.headcount(),
                posting.compensation(),
                posting.description().isBlank() ? "-" : posting.description()
        ));
    }

    private void applyForSelectedPosting() {
        if (!canApply) {
            JOptionPane.showMessageDialog(this,
                    "Casual work is available only to TAs hired for the current semester.",
                    "Apply Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0 || row >= postings.size()) {
            JOptionPane.showMessageDialog(this, "Select a posting first.");
            return;
        }
        try {
            casualWorkService.apply(postings.get(row).postingId(), currentUser.userId(), statementArea.getText());
            JOptionPane.showMessageDialog(this, "Application submitted.");
            statementArea.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Apply Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
