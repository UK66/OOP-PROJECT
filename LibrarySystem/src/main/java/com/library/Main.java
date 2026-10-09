package com.library;

import com.formdev.flatlaf.FlatDarkLaf;
import com.library.ui.MainFrame;

import javax.swing.*;

/**
 * Application entry point.
 * Initialises the FlatLaf look-and-feel, checks database availability,
 * and launches the main window on the Swing Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {

        // — 1. Apply FlatLaf dark theme (modern Swing look) —
        FlatDarkLaf.setup();

        // UI tuning
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 6);

        // — 2. Check Database Connectivity —
        if (!DBConnection.checkConnection()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Cannot connect to the MySQL database.\n\n" +
                    "Please ensure that:\n" +
                    " • MySQL service is running on localhost:3306\n" +
                    " • Database 'library_db' has been created (run schema.sql)\n" +
                    " • Credentials in src/main/resources/db.properties are correct\n\n" +
                    "The application will continue starting, but database operations may fail.",
                    "Database Connection Notice",
                    JOptionPane.WARNING_MESSAGE);
        }

        // — 3. Launch GUI on the Event Dispatch Thread —
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}