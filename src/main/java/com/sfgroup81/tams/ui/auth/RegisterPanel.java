package com.sfgroup81.tams.ui.auth;

import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.UserCsvRepository;
import com.sfgroup81.tams.service.RegistrationRequest;
import com.sfgroup81.tams.service.RegistrationService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class RegisterPanel extends JPanel {
    private final JTextField nameField = new JTextField(22);
    private final JTextField sidField = new JTextField(22);
    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JComboBox<UserRole> roleCombo = new JComboBox<>(UserRole.values());
    private final JComboBox<TACategory> taCategoryCombo = new JComboBox<>(new TACategory[]{TACategory.MODULAR, TACategory.NON_MODULAR});
    private final RegistrationService registrationService = new RegistrationService(new UserCsvRepository());
    private final Runnable onRegistrationSuccess;

    public RegisterPanel() {
        this(() -> {
        });
    }

    public RegisterPanel(Runnable onRegistrationSuccess) {
        this.onRegistrationSuccess = onRegistrationSuccess;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        addRow(gbc, 0, "Name", nameField);
        addRow(gbc, 1, "Staff/Student ID", sidField);
        addRow(gbc, 2, "Email", emailField);
        addRow(gbc, 3, "Password", passwordField);
        addRow(gbc, 4, "Role", roleCombo);
        addRow(gbc, 5, "TA Category", taCategoryCombo);

        roleCombo.addActionListener(e -> refreshTaCategoryState());
        refreshTaCategoryState();

        JButton submitButton = new JButton("Create Account");
        submitButton.addActionListener(e -> onRegister());
        gbc.gridx = 1;
        gbc.gridy = 6;
        add(submitButton, gbc);
    }

    private void addRow(GridBagConstraints gbc, int row, String label, java.awt.Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        add(comp, gbc);
    }

    private void onRegister() {
        try {
            User user = registrationService.register(new RegistrationRequest(
                    nameField.getText(),
                    sidField.getText(),
                    emailField.getText(),
                    new String(passwordField.getPassword()),
                    (UserRole) roleCombo.getSelectedItem(),
                    roleCombo.getSelectedItem() == UserRole.TA ? (TACategory) taCategoryCombo.getSelectedItem() : TACategory.NONE
            ));
            JOptionPane.showMessageDialog(this,
                    "Registration successful. User ID: " + user.userId(),
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            nameField.setText("");
            sidField.setText("");
            emailField.setText("");
            passwordField.setText("");
            taCategoryCombo.setSelectedItem(TACategory.MODULAR);
            onRegistrationSuccess.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Registration Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTaCategoryState() {
        boolean taSelected = roleCombo.getSelectedItem() == UserRole.TA;
        taCategoryCombo.setEnabled(taSelected);
        if (!taSelected) {
            taCategoryCombo.setSelectedItem(TACategory.MODULAR);
        }
    }
}
