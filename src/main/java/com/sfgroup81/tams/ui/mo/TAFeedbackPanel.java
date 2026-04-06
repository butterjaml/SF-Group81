package com.sfgroup81.tams.ui.mo;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.service.TAFeedbackAssignment;
import com.sfgroup81.tams.service.TAFeedbackService;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class TAFeedbackPanel extends JPanel {
    private final User currentUser;
    private final TAFeedbackService feedbackService;
    private final JComboBox<TAFeedbackAssignment> assignmentCombo = new JComboBox<>();
    private final JComboBox<Integer> communicationCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
    private final JComboBox<Integer> teachingCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
    private final JComboBox<Integer> reliabilityCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
    private final JTextArea commentArea = new JTextArea(5, 24);
    private final JTextArea summaryArea = new JTextArea();

    public TAFeedbackPanel(User currentUser, TAFeedbackService feedbackService, Runnable onBack) {
        this.currentUser = currentUser;
        this.feedbackService = feedbackService;
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("TA Feedback", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshAssignments();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
        content.add(buildForm(), BorderLayout.WEST);

        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        content.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        return content;
    }

    private JPanel buildForm() {
        JPanel form = PrototypeUi.createVerticalCard();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        addField(form, gbc, row++, "TA Assignment", assignmentCombo);
        addField(form, gbc, row++, "Communication", communicationCombo);
        addField(form, gbc, row++, "Teaching", teachingCombo);
        addField(form, gbc, row++, "Reliability", reliabilityCombo);
        addField(form, gbc, row++, "Comments", new JScrollPane(commentArea));

        JButton submitButton = PrototypeUi.primaryButton("Submit Feedback");
        submitButton.addActionListener(e -> submitFeedback());
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        form.add(submitButton, gbc);
        return form;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    private void refreshAssignments() {
        List<TAFeedbackAssignment> assignments = feedbackService.listPendingAssignments(currentUser.userId());
        assignmentCombo.setModel(new DefaultComboBoxModel<>(assignments.toArray(new TAFeedbackAssignment[0])));
        assignmentCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TAFeedbackAssignment assignment) {
                    label.setText(assignment.taName() + " - " + assignment.positionTitle());
                }
                return label;
            }
        });
        if (assignments.isEmpty()) {
            summaryArea.setText("No pending TA feedback forms. Once a TA is hired for one of your positions, they will appear here until feedback is submitted.");
        } else {
            summaryArea.setText("Select each hired TA and submit end-of-semester feedback. The average of communication, teaching, and reliability becomes part of the candidate's reputation score for future applications.");
        }
    }

    private void submitFeedback() {
        TAFeedbackAssignment assignment = (TAFeedbackAssignment) assignmentCombo.getSelectedItem();
        if (assignment == null) {
            JOptionPane.showMessageDialog(this, "No TA assignment is waiting for feedback.");
            return;
        }
        try {
            feedbackService.submitFeedback(
                    currentUser.userId(),
                    assignment.taUserId(),
                    assignment.positionId(),
                    (Integer) communicationCombo.getSelectedItem(),
                    (Integer) teachingCombo.getSelectedItem(),
                    (Integer) reliabilityCombo.getSelectedItem(),
                    commentArea.getText()
            );
            commentArea.setText("");
            refreshAssignments();
            JOptionPane.showMessageDialog(this, "Feedback submitted.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Submit Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
