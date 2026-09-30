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

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Books", bookPanel);
        tabbedPane.addTab("Members", memberPanel);
        tabbedPane.addTab("Issue / Return", issueReturnPanel);

        // Refresh whichever tab becomes active, so cross-tab changes
        // (e.g. issuing a book) are always reflected when you switch back.
        tabbedPane.addChangeListener(e -> {
            int selected = tabbedPane.getSelectedIndex();
            switch (selected) {
                case 0 -> bookPanel.refreshTable();
                case 1 -> memberPanel.refreshTable();
                // IssueReturnPanel already refreshes itself after each action
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
    }
}