package com.sfgroup81.tams.ui;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.ui.auth.LoginPanel;
import com.sfgroup81.tams.ui.mo.PositionManagePanel;
import com.sfgroup81.tams.ui.ta.CourseSelectPanel;
import com.sfgroup81.tams.ui.ta.ResumeUploadPanel;

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
        } else if ("One-stop Enrollment".equals(action) && currentUser != null && currentUser.role().name().equals("TA")) {
            setContentPane(new CourseSelectPanel());
        } else if ("Upload Resume".equals(action) && currentUser != null && currentUser.role().name().equals("TA")) {
            setContentPane(new ResumeUploadPanel());
        } else {
            setContentPane(new JLabel("Feature not implemented yet: " + action, JLabel.CENTER));
        }
        revalidate();
        repaint();
    }
}
