package com.library.ui;

import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class IssueReturnPanel extends JPanel {

    private final BookService bookService = new BookService();
    private final MemberService memberService = new MemberService();
    private final TransactionService transactionService = new TransactionService();

    private JComboBox<Member> memberCombo;
    private JComboBox<Book> bookCombo;

    private JTable activeTable;
    private DefaultTableModel activeTableModel;

    private static final String[] COLUMNS = {
        "Txn ID", "Book", "Member", "Issued", "Due"
    };

    public IssueReturnPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildIssuePanel(), BorderLayout.NORTH);
        add(buildActiveTable(), BorderLayout.CENTER);
        add(buildReturnBar(), BorderLayout.SOUTH);

        refreshDropdowns();
        refreshActiveTable();
    }

    // ── Issue section ────────────────────────────────────────
    private JComponent buildIssuePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Issue a Book"));

        memberCombo = new JComboBox<>();
        bookCombo = new JComboBox<>();
        memberCombo.setPreferredSize(new Dimension(200, 28));
        bookCombo.setPreferredSize(new Dimension(220, 28));

        JButton issueBtn = new JButton("Issue");
        issueBtn.addActionListener(e -> onIssue());

        JButton refreshBtn = new JButton("Refresh Lists");
        refreshBtn.addActionListener(e -> refreshDropdowns());

        panel.add(new JLabel("Member:"));
        panel.add(memberCombo);
        panel.add(new JLabel("Book:"));
        panel.add(bookCombo);
        panel.add(issueBtn);
        panel.add(refreshBtn);

        return panel;
    }

    // ── Active issues table ──────────────────────────────────
    private JComponent buildActiveTable() {
        activeTableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        activeTable = new JTable(activeTableModel);
        activeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeTable.setRowHeight(24);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder("Currently Issued Books"));
        wrapper.add(new JScrollPane(activeTable), BorderLayout.CENTER);
        return wrapper;
    }

    // ── Return section ───────────────────────────────────────
    private JComponent buildReturnBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton returnBtn = new JButton("Return Selected");
        returnBtn.addActionListener(e -> onReturn());
        panel.add(returnBtn);
        return panel;
    }

    // ── Data loading ─────────────────────────────────────────
    private void refreshDropdowns() {
        memberCombo.removeAllItems();
        for (Member m : memberService.getAllMembers()) {
            if (m.isActive()) {
                memberCombo.addItem(m);
            }
        }

        bookCombo.removeAllItems();
        for (Book b : bookService.getAllBooks()) {
            if (b.isAvailable()) {
                bookCombo.addItem(b);
            }
        }
    }

    private void refreshActiveTable() {
        activeTableModel.setRowCount(0);
        try {
            // Pull every member's active issues by scanning all members —
            // simplest approach at this project's scale; a dedicated
            // "get all issued transactions" DAO method would be cleaner
            // at larger scale.
            List<Member> members = memberService.getAllMembers();
            for (Member m : members) {
                List<Transaction> active = transactionService.getActiveIssuesForMember(m.getId());
                for (Transaction t : active) {
                    activeTableModel.addRow(new Object[]{
                        t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                        t.getIssueDate(), t.getDueDate()
                    });
                }
            }
        } catch (Exception e) {
            showError("Failed to load active issues: " + e.getMessage());
        }
    }

    // ── Event handlers ───────────────────────────────────────
    private void onIssue() {
        Member selectedMember = (Member) memberCombo.getSelectedItem();
        Book selectedBook = (Book) bookCombo.getSelectedItem();

        if (selectedMember == null || selectedBook == null) {
            showError("Select both a member and a book to issue.");
            return;
        }

        try {
            transactionService.issueBook(selectedBook.getBookId(), selectedMember.getId());
            JOptionPane.showMessageDialog(this,
                "\"" + selectedBook.getTitle() + "\" issued to " + selectedMember.getName() + ".",
                "Book Issued", JOptionPane.INFORMATION_MESSAGE);

            refreshDropdowns();   // book may no longer be available
            refreshActiveTable();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onReturn() {
        int row = activeTable.getSelectedRow();
        if (row == -1) {
            showError("Select a book from the list to return.");
            return;
        }

        int transactionId = (int) activeTableModel.getValueAt(row, 0);
        String bookTitle = (String) activeTableModel.getValueAt(row, 1);

        try {
            Transaction returned = transactionService.returnBook(transactionId);

            String message = "\"" + bookTitle + "\" returned successfully.";
            if (returned.getFineAmount() != null && returned.getFineAmount().signum() > 0) {
                message += String.format("%nOverdue fine: ₹%.2f", returned.getFineAmount());
            }

            JOptionPane.showMessageDialog(this, message, "Book Returned", JOptionPane.INFORMATION_MESSAGE);

            refreshDropdowns();   // book is available again
            refreshActiveTable();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}