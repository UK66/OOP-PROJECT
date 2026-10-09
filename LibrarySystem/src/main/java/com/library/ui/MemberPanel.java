package com.library.ui;

import com.library.dao.ITransactionDAO;
import com.library.dao.TransactionDAO;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.MemberService;
import com.library.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.List;

public class MemberPanel extends JPanel {

    private final MemberService memberService = new MemberService();
    private final TransactionService transactionService = new TransactionService();
    private final ITransactionDAO transactionDAO = new TransactionDAO();

    private JTable table;
    private DefaultTableModel tableModel;

    private static final String[] COLUMNS = {
        "ID", "Name", "Email", "Contact", "Joined", "Active"
    };

    public MemberPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTable(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JComponent buildTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    private JComponent buildButtonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton addBtn = new JButton("Add Member");
        JButton editBtn = new JButton("Edit Member");
        JButton deleteBtn = new JButton("Remove Member");
        JButton historyBtn = new JButton("View History");

        addBtn.addActionListener(this::onAdd);
        editBtn.addActionListener(this::onEdit);
        deleteBtn.addActionListener(this::onDelete);
        historyBtn.addActionListener(this::onViewHistory);

        panel.add(historyBtn);
        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deleteBtn);
        return panel;
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<Member> members = memberService.getAllMembers();
            for (Member m : members) {
                tableModel.addRow(new Object[]{
                    m.getId(), m.getName(), m.getEmail(), m.getContact(),
                    m.getMembershipDate(), m.isActive() ? "Yes" : "No"
                });
            }
        } catch (Exception e) {
            showError("Failed to load members: " + e.getMessage());
        }
    }

    private void onAdd(ActionEvent e) {
        MemberFormDialog dialog = new MemberFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);

        Member newMember = dialog.getResultMember();
        if (newMember != null) {
            try {
                memberService.addMember(newMember);
                refreshTable();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void onEdit(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Select a member to edit first.");
            return;
        }

        int memberId = (int) tableModel.getValueAt(row, 0);
        Member existing = memberService.getMemberById(memberId);
        if (existing == null) {
            showError("Member no longer exists — refreshing list.");
            refreshTable();
            return;
        }

        MemberFormDialog dialog = new MemberFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), existing);
        dialog.setVisible(true);

        Member updated = dialog.getResultMember();
        if (updated != null) {
            try {
                memberService.updateMember(updated);
                refreshTable();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void onDelete(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Select a member to remove first.");
            return;
        }

        int memberId = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove member \"" + name + "\"? This cannot be undone.",
            "Confirm Remove",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                memberService.deleteMember(memberId);
                refreshTable();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void onViewHistory(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Select a member to view history first.");
            return;
        }

        int memberId = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        showHistoryDialog(memberId, name);
    }

    private void showHistoryDialog(int memberId, String name) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Borrowing History — " + name, true);
        dialog.setLayout(new BorderLayout(10, 10));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] cols = {"Txn ID", "Book", "Issued", "Due", "Returned", "Status", "Fine"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable historyTable = new JTable(model);
        historyTable.setRowHeight(22);

        JLabel summaryLabel = new JLabel();

        Runnable loadData = () -> {
            model.setRowCount(0);
            try {
                List<Transaction> history = transactionDAO.getTransactionsByMember(memberId);
                BigDecimal totalFine = BigDecimal.ZERO;
                for (Transaction t : history) {
                    BigDecimal fine = t.getFineAmount() != null ? t.getFineAmount() : BigDecimal.ZERO;
                    if (t.getStatus() == Transaction.Status.RETURNED) {
                        totalFine = totalFine.add(fine);
                    }
                    model.addRow(new Object[]{
                        t.getTransactionId(),
                        t.getBookTitle(),
                        t.getIssueDate(),
                        t.getDueDate(),
                        t.getReturnDate() != null ? t.getReturnDate() : "—",
                        t.getStatus(),
                        String.format("₹%.2f", fine)
                    });
                }
                summaryLabel.setText(String.format("Total Outstanding Fine: ₹%.2f (%d transactions)",
                        totalFine, history.size()));
            } catch (Exception ex) {
                showError("Failed to load history: " + ex.getMessage());
            }
        };

        loadData.run();

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(summaryLabel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton payFineBtn = new JButton("Pay / Clear Fine");
        JButton closeBtn = new JButton("Close");

        payFineBtn.addActionListener(ev -> {
            try {
                transactionService.clearAllFinesForMember(memberId);
                JOptionPane.showMessageDialog(dialog,
                        "Outstanding fines cleared for " + name + ".",
                        "Fines Cleared", JOptionPane.INFORMATION_MESSAGE);
                loadData.run();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        closeBtn.addActionListener(ev -> dialog.dispose());

        btnPanel.add(payFineBtn);
        btnPanel.add(closeBtn);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        dialog.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setSize(650, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}