package com.sfgroup81.tams.ui.admin;

import com.sfgroup81.tams.model.CasualWorkApplication;
import com.sfgroup81.tams.model.CasualWorkPosting;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.UserCsvRepository;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class AdminCasualWorkPanel extends JPanel {
    private final User currentUser;
    private final CasualWorkService casualWorkService;
    private final UserCsvRepository userRepository;
    private final JTextField titleField = new JTextField();
    private final JTextField dateField = new JTextField();
    private final JTextField locationField = new JTextField();
    private final JTextField compensationField = new JTextField();
    private final JTextField headcountField = new JTextField();
    private final JTextArea skillsArea = new JTextArea(3, 20);
    private final JTextArea descriptionArea = new JTextArea(4, 20);
    private final DefaultTableModel postingModel = new DefaultTableModel(
            new Object[]{"Posting", "Title", "Date", "Location", "Compensation"}, 0
    );
    private final JTable postingTable = new JTable(postingModel);
    private final JTextArea applicationArea = new JTextArea();
    private List<CasualWorkPosting> postings = List.of();

    public AdminCasualWorkPanel(User currentUser,
                                CasualWorkService casualWorkService,
                                UserCsvRepository userRepository,
                                Runnable onBack) {
        this.currentUser = currentUser;
        this.casualWorkService = casualWorkService;
        this.userRepository = userRepository;
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("Temporary job management", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshPostings();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
        content.add(buildForm(), BorderLayout.WEST);

        postingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        postingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderApplications();
            }
        });
        content.add(new JScrollPane(postingTable), BorderLayout.CENTER);

        applicationArea.setEditable(false);
        applicationArea.setLineWrap(true);
        applicationArea.setWrapStyleWord(true);
        content.add(new JScrollPane(applicationArea), BorderLayout.EAST);
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
        addField(form, gbc, row++, "Title", titleField);
        addField(form, gbc, row++, "Work date", dateField);
        addField(form, gbc, row++, "Location", locationField);
        addField(form, gbc, row++, "Compensation", compensationField);
        addField(form, gbc, row++, "Headcount", headcountField);
        addField(form, gbc, row++, "Required skills", new JScrollPane(skillsArea));
        addField(form, gbc, row++, "Description", new JScrollPane(descriptionArea));

        JButton saveButton = PrototypeUi.primaryButton("Publish Posting");
        saveButton.addActionListener(e -> savePosting());
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        form.add(saveButton, gbc);
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

    private void savePosting() {
        try {
            casualWorkService.createPosting(
                    titleField.getText(),
                    descriptionArea.getText(),
                    dateField.getText(),
                    locationField.getText(),
                    skillsArea.getText(),
                    Integer.parseInt(headcountField.getText().trim()),
                    compensationField.getText(),
                    currentUser.userId()
            );
            clearForm();
            refreshPostings();
            JOptionPane.showMessageDialog(this, "Casual work posting published.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Publish Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshPostings() {
        postings = casualWorkService.listOpenPostings();
        postingModel.setRowCount(0);
        for (CasualWorkPosting posting : postings) {
            postingModel.addRow(new Object[]{
                    posting.postingId(),
                    posting.title(),
                    posting.workDate(),
                    posting.location(),
                    posting.compensation()
            });
        }
        if (!postings.isEmpty()) {
            postingTable.setRowSelectionInterval(0, 0);
            renderApplications();
        } else {
            applicationArea.setText("No casual work applications yet.");
        }
    }

    private void renderApplications() {
        int row = postingTable.getSelectedRow();
        if (row < 0 || row >= postings.size()) {
            applicationArea.setText("");
            return;
        }
        CasualWorkPosting posting = postings.get(row);
        List<CasualWorkApplication> applications = casualWorkService.listApplicationsForPosting(posting.postingId());
        if (applications.isEmpty()) {
            applicationArea.setText("No applications yet for " + posting.title() + ".");
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(posting.title()).append("\n\n");
        for (CasualWorkApplication application : applications) {
            builder.append(resolveUserName(application.userId()))
                    .append(" (")
                    .append(application.userId())
                    .append(")\n");
            builder.append("Applied at: ").append(application.appliedAt()).append('\n');
            builder.append("Statement: ").append(application.statement().isBlank() ? "-" : application.statement()).append("\n\n");
        }
        applicationArea.setText(builder.toString());
    }

    private String resolveUserName(String userId) {
        return userRepository.findAll().stream()
                .filter(user -> user.userId().equals(userId))
                .map(User::name)
                .findFirst()
                .orElse(userId);
    }

    private void clearForm() {
        titleField.setText("");
        dateField.setText("");
        locationField.setText("");
        compensationField.setText("");
        headcountField.setText("");
        skillsArea.setText("");
        descriptionArea.setText("");
    }
}
