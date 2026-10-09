package com.library.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 500));

        BookPanel bookPanel = new BookPanel();
        MemberPanel memberPanel = new MemberPanel();
        IssueReturnPanel issueReturnPanel = new IssueReturnPanel();
        ReportsPanel reportsPanel = new ReportsPanel();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Books", bookPanel);
        tabbedPane.addTab("Members", memberPanel);
        tabbedPane.addTab("Issue / Return", issueReturnPanel);
        tabbedPane.addTab("Reports", reportsPanel);

        // Refresh whichever tab becomes active, so cross-tab changes
        // are always reflected when switching tabs
        tabbedPane.addChangeListener(e -> {
            int selected = tabbedPane.getSelectedIndex();
            switch (selected) {
                case 0 -> bookPanel.refreshTable();
                case 1 -> memberPanel.refreshTable();
                case 2 -> issueReturnPanel.refreshAll();
                case 3 -> reportsPanel.refresh();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
    }
}