package com.library.ui;

import com.library.model.Book;

import javax.swing.*;
import java.awt.*;

public class BookFormDialog extends JDialog {

    private JTextField titleField;
    private JTextField authorField;
    private JTextField isbnField;
    private JTextField categoryField;
    private JTextField totalCopiesField;

    private final Book existingBook; // null when adding
    private Book resultBook; // set only if user confirms with valid input

    public BookFormDialog(Frame owner, Book existingBook) {
        super(owner, existingBook == null ? "Add Book" : "Edit Book", true); // true = modal
        this.existingBook = existingBook;

        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        if (existingBook != null) {
            populateFields(existingBook);
        }

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JComponent buildFormPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));

        titleField = new JTextField();
        authorField = new JTextField();
        isbnField = new JTextField();
        categoryField = new JTextField();
        totalCopiesField = new JTextField();

        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Author:"));
        panel.add(authorField);
        panel.add(new JLabel("ISBN:"));
        panel.add(isbnField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryField);
        panel.add(new JLabel("Total Copies:"));
        panel.add(totalCopiesField);

        panel.setPreferredSize(new Dimension(320, 160));
        return panel;
    }

    private JComponent buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());

        panel.add(saveBtn);
        panel.add(cancelBtn);
        return panel;
    }

    private void populateFields(Book book) {
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        isbnField.setText(book.getIsbn());
        categoryField.setText(book.getCategory());
        totalCopiesField.setText(String.valueOf(book.getTotalCopies()));
    }

    private void onSave() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();
        String category = categoryField.getText().trim();
        String totalCopiesText = totalCopiesField.getText().trim();

        // Basic field-level checks here; deeper business rules (duplicate ISBN etc.)
        // are enforced in BookService, not duplicated here.
        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Title, Author, and ISBN are required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int totalCopies;
        try {
            totalCopies = Integer.parseInt(totalCopiesText);
            if (totalCopies < 1)
                throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Total Copies must be a whole number of at least 1.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (existingBook == null) {
            // Adding: availableCopies starts equal to totalCopies
            resultBook = new Book(title, author, isbn, category, totalCopies);
        } else {
            // Editing: preserve id and availableCopies (not editable in this form —
            // available copies change only through issue/return, not manual edit)
            resultBook = existingBook;
            resultBook.setTitle(title);
            resultBook.setAuthor(author);
            resultBook.setIsbn(isbn);
            resultBook.setCategory(category);
            resultBook.setTotalCopies(totalCopies);
        }

        dispose();
    }

    /** Returns the book to save, or null if the user cancelled. */
    public Book getResultBook() {
        return resultBook;
    }
}