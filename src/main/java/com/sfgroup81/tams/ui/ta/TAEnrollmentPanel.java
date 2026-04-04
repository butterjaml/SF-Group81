package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.ApplicantProfile;
import com.sfgroup81.tams.model.ResumeFileRecord;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.service.EnrollmentService;
import com.sfgroup81.tams.service.EnrollmentSubmission;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TAEnrollmentPanel extends JPanel {
    private final User currentUser;
    private final DefaultListModel<TAPosition> positionModel = new DefaultListModel<>();
    private final ApplicantProfileCsvRepository profileRepository;
    private final TAApplicationCsvRepository applicationRepository;
    private final ResumeFileCsvRepository resumeRepository;
    private final EnrollmentService enrollmentService;
    private final Runnable onSubmitted;

    private final JLabel[] stepLabels = new JLabel[5];
    private final CardLayout stepLayout = new CardLayout();
    private final JPanel stepContainer = new JPanel(stepLayout);
    private int currentStep = 0;

    private final JTextField phoneField = new JTextField(24);
    private final JTextField majorField = new JTextField(24);
    private final JTextField yearField = new JTextField(24);
    private final JTextField gpaField = new JTextField(24);
    private final JTextField skillsField = new JTextField(24);
    private final JTextField availabilityField = new JTextField(24);
    private final JTextArea notesArea = new JTextArea(6, 28);
    private final JTextField resumeField = new JTextField(28);
    private final JList<TAPosition> positionList = new JList<>(positionModel);
    private final JTextArea previewArea = new JTextArea();
    private final JLabel confirmLabel = new JLabel("Ready to submit your application package.");

    private Path selectedResumePath;

    public TAEnrollmentPanel(User currentUser,
                             List<TAPosition> positions,
                             ApplicantProfileCsvRepository profileRepository,
                             TAApplicationCsvRepository applicationRepository,
                             ResumeFileCsvRepository resumeRepository,
                             EnrollmentService enrollmentService,
                             Runnable onBack,
                             Runnable onSubmitted,
                             String preselectedPositionId) {
        this.currentUser = currentUser;
        this.profileRepository = profileRepository;
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.enrollmentService = enrollmentService;
        this.onSubmitted = onSubmitted;

        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("TA One-stop registration", onBack), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        for (TAPosition position : positions) {
            positionModel.addElement(position);
        }
        loadExistingData(preselectedPositionId);
        updateStep();
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
        body.add(buildStepBar(), BorderLayout.NORTH);

        stepContainer.setOpaque(false);
        stepContainer.add(buildPersonalStep(), "0");
        stepContainer.add(buildResumeStep(), "1");
        stepContainer.add(buildPositionStep(), "2");
        stepContainer.add(buildPreviewStep(), "3");
        stepContainer.add(buildConfirmStep(), "4");
        body.add(stepContainer, BorderLayout.CENTER);
        body.add(buildActions(), BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildStepBar() {
        JPanel bar = new JPanel(new java.awt.GridLayout(1, 5, 12, 12));
        bar.setOpaque(false);
        String[] names = {"Basic Info", "Upload Resume", "Course Select", "Preview", "Confirm"};
        for (int i = 0; i < names.length; i++) {
            JLabel label = new JLabel(names[i], JLabel.CENTER);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
            stepLabels[i] = label;
            bar.add(label);
        }
        return bar;
    }

    private JPanel buildPersonalStep() {
        JPanel card = PrototypeUi.createVerticalCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        addField(card, gbc, 0, "Account", new JLabel(currentUser.name() + " / " + currentUser.email()));
        addField(card, gbc, 1, "Phone", phoneField);
        addField(card, gbc, 2, "Major", majorField);
        addField(card, gbc, 3, "Year of Study", yearField);
        addField(card, gbc, 4, "GPA", gpaField);
        addField(card, gbc, 5, "Skills", skillsField);
        addField(card, gbc, 6, "Availability", availabilityField);

        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        addField(card, gbc, 7, "Notes", new JScrollPane(notesArea));
        return card;
    }

    private JPanel buildResumeStep() {
        JPanel card = PrototypeUi.createVerticalCard();
        resumeField.setEditable(false);
        card.add(PrototypeUi.sectionTitle("Upload Resume (PDF/DOC)"));
        PrototypeUi.addVerticalGap(card, 12);
        card.add(resumeField);
        PrototypeUi.addVerticalGap(card, 12);
        JButton browse = PrototypeUi.primaryButton("Browse Resume");
        browse.addActionListener(e -> browseResume());
        card.add(browse);
        return card;
    }

    private JPanel buildPositionStep() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setOpaque(false);

        JPanel info = PrototypeUi.createVerticalCard();
        info.add(PrototypeUi.sectionTitle("Select up to 3 TA positions"));
        PrototypeUi.addVerticalGap(info, 8);
        info.add(new JLabel("The system will create one application record per selected position."));
        card.add(info, BorderLayout.NORTH);

        positionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        positionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TAPosition position) {
                    label.setText(position.positionId() + " | " + position.courseId() + " | " + position.courseName() + " | " + position.instructorName());
                }
                return label;
            }
        });
        positionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && positionList.getSelectedIndices().length > 3) {
                int[] selected = positionList.getSelectedIndices();
                positionList.setSelectedIndices(java.util.Arrays.copyOf(selected, 3));
                JOptionPane.showMessageDialog(this, "You can select up to 3 positions.");
            }
        });
        card.add(new JScrollPane(positionList), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPreviewStep() {
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.add(new JScrollPane(previewArea), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildConfirmStep() {
        JPanel card = PrototypeUi.createVerticalCard();
        card.add(confirmLabel);
        PrototypeUi.addVerticalGap(card, 12);
        card.add(new JLabel("Click Submit on the bottom-right to finish."));
        return card;
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton previous = PrototypeUi.secondaryButton("Previous");
        previous.addActionListener(e -> {
            currentStep = Math.max(0, currentStep - 1);
            updateStep();
        });

        JButton next = PrototypeUi.primaryButton("Next");
        next.addActionListener(e -> {
            if (currentStep < 4) {
                currentStep++;
                updateStep();
            }
        });

        JButton submit = PrototypeUi.primaryButton("Submit");
        submit.addActionListener(e -> submitEnrollment());

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.add(previous);
        actions.add(left, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(next);
        right.add(submit);
        actions.add(right, BorderLayout.EAST);
        return actions;
    }

    private void addField(JPanel parent, GridBagConstraints gbc, int row, String label, java.awt.Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        parent.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        parent.add(component, gbc);
    }

    private void browseResume() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedResumePath = chooser.getSelectedFile().toPath();
            resumeField.setText(selectedResumePath.toString());
        }
    }

    private void loadExistingData(String preselectedPositionId) {
        profileRepository.findByUserId(currentUser.userId()).ifPresent(this::fillProfile);

        List<TAApplication> applications = applicationRepository.findByUserId(currentUser.userId());
        if (!applications.isEmpty()) {
            List<Integer> selected = new ArrayList<>();
            for (TAApplication application : applications) {
                for (int i = 0; i < positionModel.size(); i++) {
                    if (positionModel.get(i).positionId().equals(application.positionId())) {
                        selected.add(i);
                        break;
                    }
                }
            }
            positionList.setSelectedIndices(selected.stream().mapToInt(Integer::intValue).toArray());
            ResumeFileRecord record = resumeRepository.findByApplicationId(applications.get(0).applicationId()).orElse(null);
            if (record != null) {
                resumeField.setText(record.autoFilename());
                Path existingPath = Path.of(record.filePath());
                if (Files.exists(existingPath)) {
                    selectedResumePath = existingPath;
                }
            }
        }

        if (preselectedPositionId != null) {
            for (int i = 0; i < positionModel.size(); i++) {
                if (positionModel.get(i).positionId().equals(preselectedPositionId)) {
                    positionList.addSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private void fillProfile(ApplicantProfile profile) {
        phoneField.setText(profile.phone());
        majorField.setText(profile.major());
        yearField.setText(profile.yearOfStudy());
        gpaField.setText(profile.gpa());
        skillsField.setText(profile.skills());
        availabilityField.setText(profile.availability());
        notesArea.setText(profile.notes());
    }

    private void updateStep() {
        for (int i = 0; i < stepLabels.length; i++) {
            if (i < currentStep) {
                stepLabels[i].setBackground(PrototypeUi.SUCCESS_GREEN);
                stepLabels[i].setForeground(java.awt.Color.WHITE);
            } else if (i == currentStep) {
                stepLabels[i].setBackground(PrototypeUi.HEADER_BLUE);
                stepLabels[i].setForeground(java.awt.Color.WHITE);
            } else {
                stepLabels[i].setBackground(new java.awt.Color(229, 233, 239));
                stepLabels[i].setForeground(PrototypeUi.TEXT_DARK);
            }
        }
        if (currentStep >= 3) {
            String preview = buildPreviewText();
            previewArea.setText(preview);
            confirmLabel.setText("<html><body style='width:520px'>" + preview.replace("\n", "<br>") + "</body></html>");
        }
        stepLayout.show(stepContainer, Integer.toString(currentStep));
    }

    private String buildPreviewText() {
        StringBuilder builder = new StringBuilder();
        builder.append("Applicant: ").append(currentUser.name()).append(" (").append(currentUser.staffOrStudentId()).append(")\n");
        builder.append("Phone: ").append(phoneField.getText()).append('\n');
        builder.append("Major / Year / GPA: ").append(majorField.getText()).append(" / ")
                .append(yearField.getText()).append(" / ").append(gpaField.getText()).append('\n');
        builder.append("Skills: ").append(skillsField.getText()).append('\n');
        builder.append("Availability: ").append(availabilityField.getText()).append('\n');
        builder.append("Resume: ").append(resumeField.getText()).append('\n');
        builder.append("Selected Positions:\n");
        int order = 1;
        for (TAPosition position : positionList.getSelectedValuesList()) {
            builder.append(order++).append(". ").append(position.positionId()).append(" - ")
                    .append(position.courseName()).append(" / ").append(position.instructorName()).append('\n');
        }
        builder.append("Notes: ").append(notesArea.getText()).append('\n');
        return builder.toString();
    }

    private void submitEnrollment() {
        try {
            enrollmentService.submit(new EnrollmentSubmission(
                    currentUser.userId(),
                    phoneField.getText(),
                    majorField.getText(),
                    yearField.getText(),
                    gpaField.getText(),
                    skillsField.getText(),
                    availabilityField.getText(),
                    notesArea.getText(),
                    selectedResumePath,
                    positionList.getSelectedValuesList().stream().map(TAPosition::positionId).toList()
            ));
            JOptionPane.showMessageDialog(this, "Application package submitted successfully.");
            onSubmitted.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Submit Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
