package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.InterviewInvitation;
import com.sfgroup81.tams.model.InterviewResponseStatus;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.service.InterviewService;
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
import java.time.LocalDateTime;
import java.util.List;

public class TAInterviewManagementPanel extends JPanel {
    private final User currentUser;
    private final InterviewService interviewService;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Invitation", "Application", "Scheduled", "Location", "Status"}, 0
    );
    private final JTable table = new JTable(tableModel);
    private final JTextArea detailsArea = new JTextArea();
    private final JTextArea responseArea = new JTextArea(4, 24);
    private final JLabel reminderLabel = PrototypeUi.mutedLabel("");
    private List<InterviewInvitation> invitations = List.of();

    public TAInterviewManagementPanel(User currentUser, InterviewService interviewService, Runnable onBack) {
        this.currentUser = currentUser;
        this.interviewService = interviewService;
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("TA interview management", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshView();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        JPanel top = PrototypeUi.createVerticalCard();
        top.add(PrototypeUi.sectionTitle("Upcoming Reminder"));
        PrototypeUi.addVerticalGap(top, 6);
        top.add(reminderLabel);
        content.add(top, BorderLayout.NORTH);

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
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        right.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        right.add(new JScrollPane(responseArea), BorderLayout.SOUTH);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton confirmButton = PrototypeUi.primaryButton("Confirm");
        confirmButton.addActionListener(e -> respond(InterviewResponseStatus.CONFIRMED));
        JButton rescheduleButton = PrototypeUi.secondaryButton("Request Time Change");
        rescheduleButton.addActionListener(e -> respond(InterviewResponseStatus.RESCHEDULE_REQUESTED));
        actions.add(confirmButton);
        actions.add(rescheduleButton);
        right.add(actions, BorderLayout.NORTH);

        content.add(right, BorderLayout.EAST);
        return content;
    }

    private void refreshView() {
        invitations = interviewService.listForApplicant(currentUser.userId());
        tableModel.setRowCount(0);
        for (InterviewInvitation invitation : invitations) {
            tableModel.addRow(new Object[]{
                    invitation.invitationId(),
                    invitation.applicationId(),
                    invitation.scheduledAt(),
                    invitation.location(),
                    invitation.responseStatus()
            });
        }
        List<String> reminders = interviewService.listReminderMessages(currentUser.userId(), LocalDateTime.now());
        reminderLabel.setText(reminders.isEmpty() ? "No interview reminders right now." : reminders.getFirst());
        if (!invitations.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            renderSelected();
        } else {
            detailsArea.setText("No interview invitations yet.");
            responseArea.setText("");
        }
    }

    private void renderSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= invitations.size()) {
            detailsArea.setText("");
            return;
        }
        InterviewInvitation invitation = invitations.get(row);
        detailsArea.setText("""
                Interview Time: %s
                Location: %s
                Current Response: %s

                Notes:
                %s

                Your Response:
                %s
                """.formatted(
                invitation.scheduledAt(),
                invitation.location(),
                invitation.responseStatus(),
                invitation.notes().isBlank() ? "-" : invitation.notes(),
                invitation.responseNote().isBlank() ? "-" : invitation.responseNote()
        ));
        responseArea.setText(invitation.responseNote());
    }

    private void respond(InterviewResponseStatus status) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= invitations.size()) {
            JOptionPane.showMessageDialog(this, "Select an interview invitation first.");
            return;
        }
        InterviewInvitation invitation = invitations.get(row);
        try {
            interviewService.respondToInterview(
                    invitation.invitationId(),
                    status,
                    responseArea.getText(),
                    currentUser.userId()
            );
            refreshView();
            JOptionPane.showMessageDialog(this, "Interview response saved.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
