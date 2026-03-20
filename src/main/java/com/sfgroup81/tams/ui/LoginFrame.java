package com.sfgroup81.tams.ui;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.ui.auth.LoginPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class LoginFrame extends JFrame {
    public LoginFrame() {
        setTitle("TA Management System - Login");
        setSize(760, 460);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setContentPane(new LoginPanel(this::onLoginSuccess));
    }

    private void onLoginSuccess(User user) {
        JPanel roleMenu = MenuRouter.buildRoleMenu(user);
        setContentPane(roleMenu);
        revalidate();
        repaint();
    }
}
