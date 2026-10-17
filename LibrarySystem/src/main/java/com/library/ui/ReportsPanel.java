package com.library.ui;

import com.library.dao.ITransactionDAO;
import com.library.dao.TransactionDAO;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReportsPanel extends JPanel {

    private final BookService bookService = new BookService();
    private final MemberService memberService = new MemberService();
    private final TransactionService transactionService = new TransactionService();
    private final ITransactionDAO transactionDAO = new TransactionDAO();

    // Currently Issued / Overdue
    private DefaultTableModel issuedModel;
    private DefaultTableModel overdueModel;

    // Member History
    private JComboBox<Member> memberCombo;
    private DefaultTableModel memberHistoryModel;

    // Book History
    private JComboBox<Book> bookCombo;
    private DefaultTableModel bookHistoryModel;

    // Unavailable Books
    private DefaultTableModel unavailableModel;

    private static final String[] ISSUED_COLUMNS = {
        "Txn ID", "Book", "Member", "Issued", "Due"
    };
    private static final String[] OVERDUE_COLUMNS = {
        "Txn ID", "Book", "Member", "Due", "Days Overdue", "Fine So Far"
    };
    private static final String[] MEMBER_HISTORY_COLUMNS = {
        "Book", "Issued", "Due", "Returned", "Status", "Fine"
    };
    private static final String[] BOOK_HISTORY_COLUMNS = {
        "Member", "Issued", "Due", "Returned", "Status"
    };
    private static final String[] UNAVAILABLE_COLUMNS = {
        "Book", "Issued To", "Due Date", "Status"
    };

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane reportTabs = new JTabbedPane();
        reportTabs.addTab("Currently Issued", buildIssuedTable());
        reportTabs.addTab("Overdue", buildOverdueTable());
        reportTabs.addTab("Member History", buildMemberHistoryTab());
        reportTabs.addTab("Book History", buildBookHistoryTab());
        reportTabs.addTab("Unavailable Books", buildUnavailableTable());
        add(reportTabs, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh All");
        refreshBtn.addActionListener(e -> refresh());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refreshBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    // ── Tab 1: Currently Issued ──────────────────────────────
    private JComponent buildIssuedTable() {
        issuedModel = new DefaultTableModel(ISSUED_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(issuedModel);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    // ── Tab 2: Overdue ────────────────────────────────────────
    private JComponent buildOverdueTable() {
        overdueModel = new DefaultTableModel(OVERDUE_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(overdueModel);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    // ── Tab 3: Member History ────────────────────────────────
    private JComponent buildMemberHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        memberCombo = new JComboBox<>();
        JButton viewBtn = new JButton("View");
        viewBtn.addActionListener(e -> loadMemberHistory());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Member:"));
        top.add(memberCombo);
        top.add(viewBtn);
        panel.add(top, BorderLayout.NORTH);

        memberHistoryModel = new DefaultTableModel(MEMBER_HISTORY_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(memberHistoryModel);
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private void loadMemberHistory() {
        Member selected = (Member) memberCombo.getSelectedItem();
        memberHistoryModel.setRowCount(0);
        if (selected == null) return;

        try {
            List<Transaction> history = transactionDAO.getTransactionsByMember(selected.getId());
            for (Transaction t : history) {
                memberHistoryModel.addRow(new Object[]{
                    t.getBookTitle(),
                    t.getIssueDate(),
                    t.getDueDate(),
                    t.getReturnDate() != null ? t.getReturnDate() : "—",
                    t.getStatus(),
                    String.format("₹%.2f", t.getFineAmount())
                });
            }
        } catch (Exception e) {
            showError("Failed to load member history: " + e.getMessage());
        }
    }

    // ── Tab 4: Book History ──────────────────────────────────
    private JComponent buildBookHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        bookCombo = new JComboBox<>();
        JButton viewBtn = new JButton("View");
        viewBtn.addActionListener(e -> loadBookHistory());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Book:"));
        top.add(bookCombo);
        top.add(viewBtn);
        panel.add(top, BorderLayout.NORTH);

        bookHistoryModel = new DefaultTableModel(BOOK_HISTORY_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(bookHistoryModel);
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private void loadBookHistory() {
        Book selected = (Book) bookCombo.getSelectedItem();
        bookHistoryModel.setRowCount(0);
        if (selected == null) return;

        try {
            List<Transaction> history = transactionService.getTransactionsByBook(selected.getBookId());
            for (Transaction t : history) {
                bookHistoryModel.addRow(new Object[]{
                    t.getMemberName(),
                    t.getIssueDate(),
                    t.getDueDate(),
                    t.getReturnDate() != null ? t.getReturnDate() : "—",
                    t.getStatus()
                });
            }
        } catch (Exception e) {
            showError("Failed to load book history: " + e.getMessage());
        }
    }

    // ── Tab 5: Unavailable Books ──────────────────────────────
    private JComponent buildUnavailableTable() {
        unavailableModel = new DefaultTableModel(UNAVAILABLE_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(unavailableModel);
        table.setRowHeight(24);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder(
            "Books with no copies available — expected return (due) date per copy currently out"));
        wrapper.add(new JScrollPane(table), BorderLayout.CENTER);
        return wrapper;
    }

    private void loadUnavailableBooks() {
        unavailableModel.setRowCount(0);
        try {
            LocalDate today = LocalDate.now();
            List<Transaction> unavailable = transactionService.getUnavailableBooksTransactions();
            for (Transaction t : unavailable) {
                String status = t.isOverdue()
                    ? "Overdue by " + ChronoUnit.DAYS.between(t.getDueDate(), today) + " days"
                    : "On time";
                unavailableModel.addRow(new Object[]{
                    t.getBookTitle(), t.getMemberName(), t.getDueDate(), status
                });
            }
        } catch (Exception e) {
            showError("Failed to load unavailable books: " + e.getMessage());
        }
    }

    // ── Refresh everything ────────────────────────────────────
    public void refresh() {
        refreshIssuedAndOverdue();
        refreshDropdowns();
        loadUnavailableBooks();
    }

    private void refreshIssuedAndOverdue() {
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
            showError("Failed to load reports: " + e.getMessage());
        }
    }

    private void refreshDropdowns() {
        Object selectedMember = memberCombo.getSelectedItem();
        memberCombo.removeAllItems();
        for (Member m : memberService.getAllMembers()) {
            memberCombo.addItem(m);
        }
        if (selectedMember != null) memberCombo.setSelectedItem(selectedMember);

        Object selectedBook = bookCombo.getSelectedItem();
        bookCombo.removeAllItems();
        for (Book b : bookService.getAllBooks()) {
            bookCombo.addItem(b);
        }
        if (selectedBook != null) bookCombo.setSelectedItem(selectedBook);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}