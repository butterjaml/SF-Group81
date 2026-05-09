package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.NotificationEntry;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;

public class TADashboardPanel extends JPanel {
    public TADashboardPanel(Runnable onBrowseJobs,
                            Runnable onViewStatus,
                            Runnable onInterviewManagement,
                            Runnable onOpenTemporaryWork,
                            boolean showTemporaryWork,
                            Runnable onOpenEnrollment,
                            List<NotificationEntry> notifications,
                            Runnable onMarkAllNotificationsRead,
                            Consumer<NotificationEntry> onOpenNotification,
                            Consumer<NotificationEntry> onMarkNotificationRead,
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
        menu.setLayout(new GridLayout(showTemporaryWork ? 5 : 4, 1, 0, 10));
        menu.add(createMenuButton("Job application", onBrowseJobs));
        menu.add(createMenuButton("My application", onViewStatus));
        menu.add(createMenuButton("Interview management", onInterviewManagement));
        if (showTemporaryWork) {
            menu.add(createMenuButton("Temporary work", onOpenTemporaryWork));
        }
        menu.add(createMenuButton("Personal Center", onOpenEnrollment));
        content.add(menu, BorderLayout.CENTER);

        content.add(buildNotificationPanel(
                notifications == null ? List.of() : notifications,
                onMarkAllNotificationsRead,
                onOpenNotification,
                onMarkNotificationRead
        ), BorderLayout.EAST);

        JButton logoutButton = PrototypeUi.secondaryButton("Log Out");
        logoutButton.addActionListener(e -> onLogout.run());
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.add(logoutButton);
        content.add(south, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private JPanel buildNotificationPanel(List<NotificationEntry> notifications,
                                          Runnable onMarkAllNotificationsRead,
                                          Consumer<NotificationEntry> onOpenNotification,
                                          Consumer<NotificationEntry> onMarkNotificationRead) {
        JPanel panel = PrototypeUi.createVerticalCard();
        panel.setPreferredSize(new Dimension(360, 0));
        panel.add(PrototypeUi.sectionTitle("Notifications"));
        PrototypeUi.addVerticalGap(panel, 8);

        long unreadCount = notifications.stream().filter(item -> !item.isRead()).count();
        panel.add(PrototypeUi.mutedLabel(unreadCount == 0
                ? "All caught up."
                : unreadCount + " unread update(s) waiting after recent status changes."));
        PrototypeUi.addVerticalGap(panel, 12);

        JButton markAllButton = PrototypeUi.secondaryButton("Mark All Read");
        markAllButton.setEnabled(unreadCount > 0);
        markAllButton.addActionListener(e -> onMarkAllNotificationsRead.run());
        panel.add(markAllButton);
        PrototypeUi.addVerticalGap(panel, 12);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (notifications.isEmpty()) {
            list.add(PrototypeUi.mutedLabel("No notifications yet."));
        } else {
            for (NotificationEntry notification : notifications.stream().limit(6).toList()) {
                list.add(notificationCard(notification, onOpenNotification, onMarkNotificationRead));
                list.add(Box.createVerticalStrut(10));
            }
        }
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(320, 420));
        panel.add(scrollPane);
        return panel;
    }

    private JPanel notificationCard(NotificationEntry notification,
                                    Consumer<NotificationEntry> onOpenNotification,
                                    Consumer<NotificationEntry> onMarkNotificationRead) {
        JPanel card = PrototypeUi.createVerticalCard();
        card.setAlignmentX(LEFT_ALIGNMENT);

        JLabel status = new JLabel(notification.isRead() ? "READ" : "UNREAD");
        status.setForeground(notification.isRead() ? PrototypeUi.TEXT_DARK : PrototypeUi.HEADER_BLUE);
        card.add(status);
        card.add(new JLabel(notification.title().isBlank() ? "Notification" : notification.title()));
        card.add(PrototypeUi.mutedLabel(notification.createdAt()));

        JTextArea body = new JTextArea(notification.message().isBlank() ? "-" : notification.message());
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        card.add(body);

        JLabel linkLabel = PrototypeUi.mutedLabel("Related link: " + relatedPageLabel(notification.relatedPage()));
        card.add(linkLabel);
        PrototypeUi.addVerticalGap(card, 8);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton openButton = PrototypeUi.primaryButton("Open");
        openButton.addActionListener(e -> onOpenNotification.accept(notification));
        actions.add(openButton);
        if (!notification.isRead()) {
            JButton readButton = PrototypeUi.secondaryButton("Mark Read");
            readButton.addActionListener(e -> onMarkNotificationRead.accept(notification));
            actions.add(readButton);
        }
        card.add(actions);
        return card;
    }

    private String relatedPageLabel(String relatedPage) {
        if ("TA_INTERVIEWS".equalsIgnoreCase(relatedPage)) {
            return "Interview management";
        }
        return "My application";
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
