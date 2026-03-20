package com.sfgroup81.tams.ui;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.ui.auth.LoginPanel;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;

public class LoginFrame extends JFrame {
    public LoginFrame() {
        setTitle("TA Management System - Login");
        setSize(760, 460);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setContentPane(new LoginPanel(this::onLoginSuccess));
    }

    private void onLoginSuccess(User user) {
        getContentPane().removeAll();
        add(new JLabel("Signed in as: " + user.role(), JLabel.CENTER), BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
