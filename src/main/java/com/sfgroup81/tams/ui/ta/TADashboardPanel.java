package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

public class TADashboardPanel extends JPanel {
    public TADashboardPanel(Runnable onBrowseJobs,
                            Runnable onViewStatus,
                            Runnable onOpenEnrollment,
                            Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("TA", null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(24, 24));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));

        JPanel notice = PrototypeUi.createVerticalCard();
        notice.setPreferredSize(new Dimension(220, 0));
        JLabel line1 = PrototypeUi.sectionTitle("Before Applying");
        JLabel line2 = new JLabel("<html><body style='width:160px'>Complete your one-stop registration and upload your resume before submitting TA applications.</body></html>");
        line2.setForeground(PrototypeUi.DANGER_RED);
        notice.add(line1);
        PrototypeUi.addVerticalGap(notice, 12);
        notice.add(line2);
        content.add(notice, BorderLayout.WEST);

        JPanel menu = PrototypeUi.createVerticalCard();
        menu.setLayout(new GridLayout(5, 1, 0, 10));
        menu.add(createMenuButton("Job application", onBrowseJobs));
        menu.add(createMenuButton("My application", onViewStatus));
        menu.add(createMenuButton("Interview management", () -> javax.swing.JOptionPane.showMessageDialog(this, "Interview confirmation is planned for Sprint2.")));
        menu.add(createMenuButton("Temporary work", () -> javax.swing.JOptionPane.showMessageDialog(this, "Temporary work postings start in Sprint2.")));
        menu.add(createMenuButton("Personal Center", onOpenEnrollment));
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
        button.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        button.setBackground(PrototypeUi.CARD_BACKGROUND);
        button.addActionListener(e -> action.run());
        return button;
    }
}
