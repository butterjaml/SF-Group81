package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.CourseOption;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.service.CourseSelectionService;
import com.sfgroup81.tams.service.SessionContext;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

public class CourseSelectPanel extends JPanel {
    private final DefaultListModel<CourseOption> availableModel = new DefaultListModel<>();
    private final JList<CourseOption> availableList = new JList<>(availableModel);
    private final JTextArea selectedPreview = new JTextArea();
    private final CourseSelectionService service = new CourseSelectionService(
            new PositionCsvRepository(),
            new ApplicationPreferenceCsvRepository()
    );

    public CourseSelectPanel() {
        setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("One-stop Enrollment: Course Preferences (Multi-select)", JLabel.CENTER);
        add(title, BorderLayout.NORTH);

        availableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelectedPreview();
            }
        });

        selectedPreview.setEditable(false);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(availableList),
                new JScrollPane(selectedPreview)
        );
        split.setResizeWeight(0.6);
        add(split, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        JButton refreshButton = new JButton("Refresh Courses");
        refreshButton.addActionListener(e -> loadCoursesAndRestoreSelection());
        JButton saveButton = new JButton("Save Preferences");
        saveButton.addActionListener(e -> savePreferences());
        actions.add(refreshButton);
        actions.add(saveButton);
        add(actions, BorderLayout.SOUTH);

        loadCoursesAndRestoreSelection();
    }

    private void loadCoursesAndRestoreSelection() {
        availableModel.clear();
        List<CourseOption> options = service.listAvailableCourses();
        for (CourseOption option : options) {
            availableModel.addElement(option);
        }

        User user = SessionContext.getCurrentUser();
        if (user == null) {
            selectedPreview.setText("Please login first.");
            return;
        }

        List<String> savedCourseIds = service.getSelectedCourseIds(user.userId());
        List<Integer> selectedIndexes = new ArrayList<>();
        for (int i = 0; i < availableModel.size(); i++) {
            if (savedCourseIds.contains(availableModel.get(i).courseId())) {
                selectedIndexes.add(i);
            }
        }
        int[] indexArray = selectedIndexes.stream().mapToInt(Integer::intValue).toArray();
        availableList.setSelectedIndices(indexArray);
        renderSelectedPreview();
    }

    private void renderSelectedPreview() {
        List<CourseOption> selected = availableList.getSelectedValuesList();
        if (selected.isEmpty()) {
            selectedPreview.setText("No course selected.");
            return;
        }

        StringBuilder builder = new StringBuilder("Selected courses:\n");
        for (int i = 0; i < selected.size(); i++) {
            CourseOption option = selected.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(option.courseId())
                    .append(" - ")
                    .append(option.displayTitle())
                    .append('\n');
        }
        selectedPreview.setText(builder.toString());
    }

    private void savePreferences() {
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Please login first.", "Not Logged In", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> selectedCourseIds = availableList.getSelectedValuesList()
                .stream()
                .map(CourseOption::courseId)
                .toList();

        try {
            service.saveCoursePreferences(user.userId(), selectedCourseIds);
            JOptionPane.showMessageDialog(this, "Course preferences saved.");
            renderSelectedPreview();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
