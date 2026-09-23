package com.library;

import com.formdev.flatlaf.FlatDarkLaf;
import com.library.ui.MainFrame;

import javax.swing.*;

/**
 * Application entry point.
 * Initialises the FlatLaf look-and-feel, then launches the main window
 * on the Swing Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {

        // — 1. Apply FlatLaf dark theme (modern Swing look) —
        FlatDarkLaf.setup();

        // Optional: override specific UI properties for fine-tuning
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 6);

        // — 2. Launch GUI on the Event Dispatch Thread —
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}