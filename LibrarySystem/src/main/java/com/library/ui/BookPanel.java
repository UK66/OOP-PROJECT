package com.library.ui;

import com.library.model.Book;
import com.library.service.BookService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class BookPanel extends JPanel {

    private final BookService bookService = new BookService();

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    private static final String[] COLUMNS = {
        "ID", "Title", "Author", "ISBN", "Category", "Total", "Available"
    };

    public BookPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        refreshTable();
    }

    // ── Top bar: search ─────────────────────────────────────
    private JComponent buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        searchField = new JTextField();
        JButton searchBtn = new JButton("Search");
        JButton clearBtn = new JButton("Clear");

        searchBtn.addActionListener(this::onSearch);
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btns.add(searchBtn);
        btns.add(clearBtn);

        panel.add(new JLabel("Search: "), BorderLayout.WEST);
        panel.add(searchField, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.EAST);
        return panel;
    }

    // ── Center: table ────────────────────────────────────────
    private JComponent buildTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // read-only table — edits happen via dialog
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    // ── Bottom: action buttons ──────────────────────────────
    private JComponent buildButtonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton addBtn = new JButton("Add Book");
        JButton editBtn = new JButton("Edit Book");
        JButton deleteBtn = new JButton("Delete Book");

        addBtn.addActionListener(this::onAdd);
        editBtn.addActionListener(this::onEdit);
        deleteBtn.addActionListener(this::onDelete);

        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deleteBtn);
        return panel;
    }

    // ── Data loading ─────────────────────────────────────────
    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<Book> books = bookService.getAllBooks();
            for (Book b : books) {
                tableModel.addRow(new Object[]{
                    b.getBookId(), b.getTitle(), b.getAuthor(),
                    b.getIsbn(), b.getCategory(), b.getTotalCopies(), b.getAvailableCopies()
                });
            }
        } catch (Exception e) {
            showError("Failed to load books: " + e.getMessage());
        }
    }

    // ── Event handlers ───────────────────────────────────────
    private void onSearch(ActionEvent e) {
        String keyword = searchField.getText().trim();
        tableModel.setRowCount(0);
        try {
            List<Book> results = bookService.searchBooks(keyword);
            for (Book b : results) {
                tableModel.addRow(new Object[]{
                    b.getBookId(), b.getTitle(), b.getAuthor(),
                    b.getIsbn(), b.getCategory(), b.getTotalCopies(), b.getAvailableCopies()
                });
            }
        } catch (Exception ex) {
            showError("Search failed: " + ex.getMessage());
        }
    }

    private void onAdd(ActionEvent e) {
        BookFormDialog dialog = new BookFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);

        Book newBook = dialog.getResultBook();
        if (newBook != null) {
            try {
                bookService.addBook(newBook);
                refreshTable();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void onEdit(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Select a book to edit first.");
            return;
        }

        int bookId = (int) tableModel.getValueAt(row, 0);
        Book existing = bookService.getBookById(bookId);
        if (existing == null) {
            showError("Book no longer exists — refreshing list.");
            refreshTable();
            return;
        }

        BookFormDialog dialog = new BookFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), existing);
        dialog.setVisible(true);

        Book updated = dialog.getResultBook();
        if (updated != null) {
            try {
                bookService.updateBook(updated);
                refreshTable();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void onDelete(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Select a book to delete first.");
            return;
        }

        int bookId = (int) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete \"" + title + "\"? This cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                bookService.deleteBook(bookId);
                refreshTable();
            } catch (Exception ex) {
                showError("Could not delete book: " + ex.getMessage());
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}