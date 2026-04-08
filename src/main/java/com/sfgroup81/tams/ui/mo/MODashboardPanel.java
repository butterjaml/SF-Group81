package com.sfgroup81.tams.ui.mo;

import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class MODashboardPanel extends JPanel {
    public MODashboardPanel(Runnable onManagePositions,
                            Runnable onManageCandidates,
                            Runnable onFeedback,
                            int pendingFeedbackCount,
                            Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("MO", null), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(48, 120, 48, 120));

        if (pendingFeedbackCount > 0) {
            JPanel notice = PrototypeUi.createVerticalCard();
            notice.add(PrototypeUi.sectionTitle("Semester-end Prompt"));
            PrototypeUi.addVerticalGap(notice, 8);
            notice.add(PrototypeUi.mutedLabel("You have " + pendingFeedbackCount + " TA feedback form(s) due after position deadlines."));
            content.add(notice, BorderLayout.NORTH);
        }

        JPanel menu = PrototypeUi.createVerticalCard();
        menu.setLayout(new GridLayout(3, 1, 0, 12));
        menu.add(createMenuButton("My Positions", onManagePositions));
        menu.add(createMenuButton("Candidate Management", onManageCandidates));
        menu.add(createMenuButton("TA Evaluation Feedback", onFeedback));
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
