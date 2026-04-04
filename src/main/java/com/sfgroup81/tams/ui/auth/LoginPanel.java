package com.sfgroup81.tams.ui.auth;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.UserCsvRepository;
import com.sfgroup81.tams.service.AuthService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {
    private final JTextField emailField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final AuthService authService = new AuthService(new UserCsvRepository());
    private final Consumer<User> onLoginSuccess;

    public LoginPanel(Consumer<User> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        addRow(gbc, 0, "Email", emailField);
        addRow(gbc, 1, "Password", passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> onLogin());
        gbc.gridx = 1;
        gbc.gridy = 2;
        add(loginButton, gbc);
    }

    private void addRow(GridBagConstraints gbc, int row, String label, java.awt.Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        add(comp, gbc);
    }

    private void onLogin() {
        try {
            User user = authService.login(emailField.getText(), new String(passwordField.getPassword()));
            JOptionPane.showMessageDialog(this,
                    "Welcome, " + user.name(),
                    "Login Success",
                    JOptionPane.INFORMATION_MESSAGE);
            passwordField.setText("");
            onLoginSuccess.accept(user);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
