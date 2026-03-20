package com.sfgroup81.tams;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.ui.BootstrapFrame;
import javax.swing.SwingUtilities;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        DataBootstrap.initialize();
        SwingUtilities.invokeLater(() -> {
            BootstrapFrame frame = new BootstrapFrame();
            frame.setVisible(true);
        });
    }
}
