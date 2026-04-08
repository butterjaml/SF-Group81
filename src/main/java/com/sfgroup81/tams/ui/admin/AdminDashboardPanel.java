package com.sfgroup81.tams.ui.admin;

import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class AdminDashboardPanel extends JPanel {
    public AdminDashboardPanel(Runnable onUserManagement,
                               Runnable onCasualWork,
                               Runnable onAuditLog,
                               Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("Admin", null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(48, 120, 48, 120));

        JPanel menu = PrototypeUi.createVerticalCard();
        menu.setLayout(new GridLayout(3, 1, 0, 12));
        menu.add(createMenuButton("User Management", onUserManagement));
        menu.add(createMenuButton("Casual Work Posting", onCasualWork));
        menu.add(createMenuButton("Audit Log", onAuditLog));
        content.add(menu, BorderLayout.CENTER);

        JButton logoutButton = PrototypeUi.secondaryButton("Log Out");
        logoutButton.addActionListener(e -> onLogout.run());
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.add(logoutButton);
        content.add(south, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private JButton createMenuButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        button.setBackground(PrototypeUi.CARD_BACKGROUND);
        button.addActionListener(e -> action.run());
        return button;
    }
}
