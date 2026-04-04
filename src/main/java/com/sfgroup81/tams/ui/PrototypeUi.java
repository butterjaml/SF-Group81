package com.sfgroup81.tams.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public final class PrototypeUi {
    public static final Color HEADER_BLUE = new Color(96, 108, 228);
    public static final Color CARD_BACKGROUND = new Color(241, 245, 252);
    public static final Color PANEL_BACKGROUND = new Color(248, 249, 252);
    public static final Color ACCENT_ORANGE = new Color(255, 111, 66);
    public static final Color TEXT_DARK = new Color(33, 37, 62);
    public static final Color SUCCESS_GREEN = new Color(45, 179, 93);
    public static final Color DANGER_RED = new Color(223, 58, 78);

    private PrototypeUi() {
    }

    public static JPanel createPage(String title, Runnable onBack) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PANEL_BACKGROUND);
        root.add(createHeader(title, onBack), BorderLayout.NORTH);
        return root;
    }

    public static JPanel createHeader(String title, Runnable onBack) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        if (onBack != null) {
            JButton backButton = new JButton("<");
            backButton.addActionListener(e -> onBack.run());
            styleSecondaryButton(backButton);
            backButton.setPreferredSize(new Dimension(56, 40));
            header.add(backButton, BorderLayout.WEST);
        } else {
            header.add(Box.createHorizontalStrut(56), BorderLayout.WEST);
        }

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        header.add(titleLabel, BorderLayout.CENTER);
        header.add(Box.createHorizontalStrut(56), BorderLayout.EAST);
        return header;
    }

    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 230, 240)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        return card;
    }

    public static JPanel createVerticalCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        return card;
    }

    public static JPanel createCardGrid(int rows, int cols) {
        JPanel grid = new JPanel(new GridLayout(rows, cols, 20, 20));
        grid.setOpaque(false);
        return grid;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_DARK);
        label.setFont(new Font("SansSerif", Font.BOLD, 20));
        return label;
    }

    public static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(110, 118, 132));
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return label;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        stylePrimaryButton(button);
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        styleSecondaryButton(button);
        return button;
    }

    public static void stylePrimaryButton(JButton button) {
        button.setBackground(HEADER_BLUE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
    }

    public static void styleSecondaryButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(HEADER_BLUE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(HEADER_BLUE));
    }

    public static void addVerticalGap(JPanel panel, int height) {
        panel.add(Box.createVerticalStrut(height));
    }

    public static void centerComponent(JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
    }
}
