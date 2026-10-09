package com.library.ui;

import com.library.model.Transaction;
import com.library.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReportsPanel extends JPanel {

    private final TransactionService transactionService = new TransactionService();

    private DefaultTableModel issuedModel;
    private DefaultTableModel overdueModel;

    private static final String[] ISSUED_COLUMNS = {
        "Txn ID", "Book", "Member", "Issued", "Due"
    };
    private static final String[] OVERDUE_COLUMNS = {
        "Txn ID", "Book", "Member", "Due", "Days Overdue", "Fine So Far"
    };

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane reportTabs = new JTabbedPane();
        reportTabs.addTab("Currently Issued", buildIssuedTable());
        reportTabs.addTab("Overdue", buildOverdueTable());
        add(reportTabs, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refreshBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    private JComponent buildIssuedTable() {
        issuedModel = new DefaultTableModel(ISSUED_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(issuedModel);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    private JComponent buildOverdueTable() {
        overdueModel = new DefaultTableModel(OVERDUE_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(overdueModel);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    public void refresh() {
        issuedModel.setRowCount(0);
        overdueModel.setRowCount(0);

        try {
            List<Transaction> issued = transactionService.getAllActiveTransactions();
            for (Transaction t : issued) {
                issuedModel.addRow(new Object[]{
                    t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                    t.getIssueDate(), t.getDueDate()
                });
            }

            LocalDate today = LocalDate.now();
            List<Transaction> overdue = transactionService.getOverdueTransactions();
            for (Transaction t : overdue) {
                long daysOverdue = ChronoUnit.DAYS.between(t.getDueDate(), today);
                overdueModel.addRow(new Object[]{
                    t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                    t.getDueDate(), daysOverdue,
                    String.format("₹%.2f", t.calculateFine(today))
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load reports: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}