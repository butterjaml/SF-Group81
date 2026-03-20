package com.sfgroup81.tams.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;

public class BootstrapFrame extends JFrame {
    public BootstrapFrame() {
        setTitle("TA Management System");
        setSize(720, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel label = new JLabel("Project baseline is ready. Login and registration will be added in Sprint 1.", JLabel.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
