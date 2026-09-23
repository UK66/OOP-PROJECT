package com.library.ui;

import com.library.dao.ITransactionDAO;
import com.library.dao.TransactionDAO;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.MemberService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class MemberPanel extends JPanel {

    private final MemberService memberService = new MemberService();
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

    private void refreshTable() {
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
                showError("Could not remove member: " + ex.getMessage() +
                    "\n(This usually means they have transaction history — members with " +
                    "borrowing history can't be deleted.)");
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

        List<Transaction> history = transactionDAO.getTransactionsByMember(memberId);

        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                name + " has no borrowing history.",
                "Borrowing History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Transaction t : history) {
            sb.append(String.format("%s | Issued: %s | Due: %s | Status: %s | Fine: ₹%.2f%n",
                t.getBookTitle(), t.getIssueDate(), t.getDueDate(),
                t.getStatus(), t.getFineAmount()));
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 250));

        JOptionPane.showMessageDialog(this, scrollPane,
            "Borrowing History — " + name, JOptionPane.PLAIN_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}