package com.library.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null); // center on screen
        setMinimumSize(new Dimension(800, 500));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Books", new BookPanel());
        tabbedPane.addTab("Members", new MemberPanel());
        // Added on Day 5:
        // tabbedPane.addTab("Members", new MemberPanel());
        // tabbedPane.addTab("Issue / Return", new IssueReturnPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }
}