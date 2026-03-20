package com.sfgroup81.tams;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.ui.LoginFrame;
import javax.swing.SwingUtilities;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        DataBootstrap.initialize();
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
