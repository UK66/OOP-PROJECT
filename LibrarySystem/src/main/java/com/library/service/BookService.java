package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.IBookDAO;
import com.library.exception.DatabaseException;
import com.library.model.Book;

import java.util.List;
import java.util.regex.Pattern;

public class BookService {
    // ISBN-10 (last character may be X) or ISBN-13, after hyphens/spaces are removed
    private static final Pattern ISBN_PATTERN = Pattern.compile("^(\\d{9}[\\dXx]|\\d{13})$");

    private final IBookDAO bookDAO;

    public BookService() {
        this(new BookDAO());
    }

    public BookService(IBookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }

    public void addBook(Book book) {
        validateBook(book);

        // Business rule: ISBN must be unique
        Book existing = bookDAO.getBookByIsbn(book.getIsbn());
        if (existing != null) {
            throw new IllegalArgumentException("A book with ISBN " + book.getIsbn() + " already exists.");
        }

        bookDAO.addBook(book);
    }

    public Book getBookById(int bookId) {
        return bookDAO.getBookById(bookId);
    }

    public List<Book> getAllBooks() {
        return bookDAO.getAllBooks();
    }

    public void updateBook(Book book) {
        validateBook(book);

        Book current = bookDAO.getBookById(book.getBookId());
        if (current == null) {
            throw new IllegalArgumentException("Book no longer exists (ID: " + book.getBookId() + ").");
        }

        // Check duplicate ISBN across OTHER books
        Book existingWithIsbn = bookDAO.getBookByIsbn(book.getIsbn());
        if (existingWithIsbn != null && existingWithIsbn.getBookId() != book.getBookId()) {
            throw new IllegalArgumentException(
                    "Another book with ISBN " + book.getIsbn() + " already exists (\""
                            + existingWithIsbn.getTitle() + "\").");
        }

        // Adjust copy counts safely
        int borrowedCopies = current.getTotalCopies() - current.getAvailableCopies();
        if (book.getTotalCopies() < borrowedCopies) {
            throw new IllegalArgumentException(String.format(
                    "Cannot set total copies to %d because %d copy(s) are currently issued to members.",
                    book.getTotalCopies(), borrowedCopies));
        }

        book.setAvailableCopies(book.getTotalCopies() - borrowedCopies);
        bookDAO.updateBook(book);
    }

    public void deleteBook(int bookId) {
        Book book = bookDAO.getBookById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book not found.");
        }

        int borrowedCopies = book.getTotalCopies() - book.getAvailableCopies();
        if (borrowedCopies > 0) {
            throw new IllegalStateException(String.format(
                    "Cannot delete \"%s\" because %d copy(s) are currently issued to members.",
                    book.getTitle(), borrowedCopies));
        }

        try {
            bookDAO.deleteBook(bookId);
        } catch (DatabaseException e) {
            if (e.getErrorCode() == 1451 || e.getMessage().contains("borrowing") || e.getMessage().contains("related")) {
                throw new IllegalStateException(
                        "Cannot delete \"" + book.getTitle() + "\" because it has borrowing records in history.");
            }
            throw e;
        }
    }

    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return bookDAO.getAllBooks();
        }
        return bookDAO.searchBooks(keyword.trim());
    }

    // ── Validation ──────────────────────────────────────────
    private void validateBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book details cannot be empty.");
        }
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        if (book.getAuthor() == null || book.getAuthor().isBlank()) {
            throw new IllegalArgumentException("Author cannot be empty.");
        }
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            throw new IllegalArgumentException("ISBN cannot be empty.");
        }
        String normalizedIsbn = book.getIsbn().replaceAll("[\\s-]", "");
        if (!ISBN_PATTERN.matcher(normalizedIsbn).matches()) {
            throw new IllegalArgumentException(
                    "ISBN must be 10 or 13 digits (hyphens allowed), e.g. 978-0132350884.");
        }
        if (book.getTotalCopies() < 1) {
            throw new IllegalArgumentException("Total copies must be at least 1.");
        }
    }
}