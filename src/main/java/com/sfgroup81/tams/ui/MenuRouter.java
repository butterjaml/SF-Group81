package com.sfgroup81.tams.ui;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Consumer;

public final class MenuRouter {
    private MenuRouter() {
    }

    public static JPanel buildRoleMenu(User user, Consumer<String> actionHandler) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Role Menu: " + user.role(), JLabel.CENTER);
        container.add(title, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        for (String action : actionsByRole(user.role())) {
            JButton button = new JButton(action);
            button.setAlignmentX(JButton.LEFT_ALIGNMENT);
            button.addActionListener(e -> actionHandler.accept(action));
            menu.add(button);
        }

        container.add(menu, BorderLayout.CENTER);
        return container;
    }

    private static String[] actionsByRole(UserRole role) {
        return switch (role) {
            case TA -> new String[]{
                    "One-stop Enrollment",
                    "View Application Status",
                    "Upload Resume"
            };
            case MO -> new String[]{
                    "Job Posting Management",
                    "Candidate Screening",
                    "Interview Scheduling"
            };
            case ADMIN -> new String[]{
                    "Semester Management",
                    "Casual Work Posting",
                    "Audit Log"
            };
        };
    }
}
