package com.sfgroup81.tams.ui.ta;

import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.service.ResumeUploadService;
import com.sfgroup81.tams.service.SessionContext;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;

public class ResumeUploadPanel extends JPanel {
    private final JTextField selectedFileField = new JTextField(36);
    private Path selectedFile;
    private final ResumeUploadService uploadService = new ResumeUploadService();

    public ResumeUploadPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        selectedFileField.setEditable(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Selected Resume:"), gbc);
        gbc.gridx = 1;
        add(selectedFileField, gbc);

        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseFile());
        gbc.gridx = 2;
        add(browseButton, gbc);

        JButton uploadButton = new JButton("Upload");
        uploadButton.addActionListener(e -> uploadResume());
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(uploadButton, gbc);
    }

    private void browseFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile().toPath();
            selectedFileField.setText(selectedFile.toString());
        }
    }

    private void uploadResume() {
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Please login first.", "Not Logged In", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String autoFilename = uploadService.uploadResume(user.userId(), selectedFile);
            JOptionPane.showMessageDialog(this, "Resume uploaded as: " + autoFilename);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Upload Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
