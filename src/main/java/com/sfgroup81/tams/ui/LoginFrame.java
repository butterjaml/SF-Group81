package com.sfgroup81.tams.ui;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.ui.auth.LoginPanel;
import com.sfgroup81.tams.ui.mo.PositionManagePanel;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LoginFrame extends JFrame {
    private User currentUser;

    public LoginFrame() {
        setTitle("TA Management System - Login");
        setSize(760, 460);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setContentPane(new LoginPanel(this::onLoginSuccess));
    }

    private void onLoginSuccess(User user) {
        this.currentUser = user;
        JPanel roleMenu = MenuRouter.buildRoleMenu(user, this::handleAction);
        setContentPane(roleMenu);
        revalidate();
        repaint();
    }

    private void handleAction(String action) {
        if ("Job Posting Management".equals(action) && currentUser != null && currentUser.role().name().equals("MO")) {
            setContentPane(new PositionManagePanel());
        } else {
            setContentPane(new JLabel("Feature not implemented yet: " + action, JLabel.CENTER));
        }
        revalidate();
        repaint();
    }
}
