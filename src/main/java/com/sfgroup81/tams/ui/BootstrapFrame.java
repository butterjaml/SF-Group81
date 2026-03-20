package com.sfgroup81.tams.ui;

import com.sfgroup81.tams.ui.auth.RegisterPanel;

import javax.swing.JFrame;

public class BootstrapFrame extends JFrame {
    public BootstrapFrame() {
        setTitle("TA Management System - Registration");
        setSize(760, 460);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setContentPane(new RegisterPanel());
    }
}
