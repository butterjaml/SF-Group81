package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

public class TAJobDetailPanel extends JPanel {
    public TAJobDetailPanel(TAPosition position, Runnable onBack, Runnable onApply) {
        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("TA Job Details", onBack), BorderLayout.NORTH);

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setText(buildDetails(position));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(32, 120, 32, 120));
        center.add(new JScrollPane(details), BorderLayout.CENTER);

        JButton applyButton = PrototypeUi.primaryButton("Click to Sign Up");
        applyButton.addActionListener(e -> onApply.run());
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(applyButton);
        center.add(actions, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    private String buildDetails(TAPosition position) {
        return """
                1. Job Title: %s

                2. Course / Instructor: %s (%s)

                3. Position Type: %s

                4. Job Responsibilities:
                %s

                5. Detailed Working Hours:
                %s

                6. Salary Disclosure:
                %s

                7. Recruitment Requirements:
                Mandatory: %s
                Preferred: %s
                Bonus: %s
                """.formatted(
                fallback(position.title(), position.courseName()),
                fallback(position.courseName(), "-"),
                fallback(position.instructorName(), "-"),
                fallback(position.positionType(), "-"),
                fallback(position.responsibilities(), "-"),
                fallback(position.workingHours(), "-"),
                fallback(position.salaryInfo(), "-"),
                fallback(position.mandatoryRequirements(), "-"),
                fallback(position.preferredRequirements(), "-"),
                fallback(position.bonusRequirements(), "-")
        );
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
