package com.sfgroup81.tams.ui.auth;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.Consumer;

public class AuthLandingPanel extends JPanel {
    public AuthLandingPanel(Consumer<User> onLoginSuccess) {
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(40, 120, 40, 120));

        JLabel title = new JLabel("TA Management System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(PrototypeUi.TEXT_DARK);
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(24, 80, 24, 80));

        JPanel card = PrototypeUi.createVerticalCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                card.getBorder(),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sign In", new LoginPanel(onLoginSuccess));
        tabs.addTab("Register", new RegisterPanel(() -> tabs.setSelectedIndex(0)));
        card.add(tabs, BorderLayout.CENTER);

        JLabel hint = new JLabel("Sprint1 supports TA, MO, and Admin registration with role-based menus.", SwingConstants.CENTER);
        hint.setForeground(PrototypeUi.HEADER_BLUE);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 14));
        card.add(hint, BorderLayout.SOUTH);

        center.add(card, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }
}
