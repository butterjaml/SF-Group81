package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;

public class TAJobBrowserPanel extends JPanel {
    public TAJobBrowserPanel(List<TAPosition> positions,
                             Runnable onBack,
                             Consumer<String> onViewDetails,
                             Runnable onOpenEnrollment) {
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("TA Job Browsing", onBack), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        JButton shortcut = PrototypeUi.primaryButton("Open One-stop Registration");
        shortcut.addActionListener(e -> onOpenEnrollment.run());
        JPanel topActions = new JPanel();
        topActions.setOpaque(false);
        topActions.add(shortcut);
        content.add(topActions, BorderLayout.NORTH);

        if (positions.isEmpty()) {
            JPanel empty = PrototypeUi.createVerticalCard();
            empty.add(PrototypeUi.sectionTitle("No published positions"));
            PrototypeUi.addVerticalGap(empty, 8);
            empty.add(new JLabel("Ask an MO to publish TA jobs for the current semester."));
            content.add(empty, BorderLayout.CENTER);
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 2, 20, 20));
            grid.setOpaque(false);
            for (TAPosition position : positions) {
                JPanel card = PrototypeUi.createVerticalCard();
                card.add(PrototypeUi.sectionTitle(position.title().isBlank() ? position.courseName() : position.title()));
                PrototypeUi.addVerticalGap(card, 8);
                card.add(PrototypeUi.mutedLabel(position.courseId() + " | " + position.instructorName()));
                PrototypeUi.addVerticalGap(card, 8);
                card.add(new JLabel("Hours: " + emptyText(position.workingHours())));
                card.add(new JLabel("Salary: " + emptyText(position.salaryInfo())));
                card.add(new JLabel("Mandatory: " + emptyText(position.mandatoryRequirements())));
                card.add(new JLabel("Preferred: " + emptyText(position.preferredRequirements())));
                card.add(new JLabel("Additional: " + emptyText(position.bonusRequirements())));
                PrototypeUi.addVerticalGap(card, 12);
                JButton learnMore = PrototypeUi.secondaryButton("Learn More");
                learnMore.addActionListener(e -> onViewDetails.accept(position.positionId()));
                PrototypeUi.centerComponent(learnMore);
                card.add(learnMore);
                grid.add(card);
            }

            JScrollPane scrollPane = new JScrollPane(grid);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            content.add(scrollPane, BorderLayout.CENTER);
        }

        add(content, BorderLayout.CENTER);
    }

    private String emptyText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
