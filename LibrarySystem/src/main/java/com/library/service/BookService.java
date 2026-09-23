package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.IBookDAO;
import com.library.model.Book;

import java.util.List;

public class BookService {

    private final IBookDAO bookDAO;

    public BookService() {
        this.bookDAO = new BookDAO();
    }

    public void addBook(Book book) {
        validateBook(book);

        // Business rule: ISBN must be unique — checked here, not just relying
        // on the DB's UNIQUE constraint, so we can give a clear message
        // instead of a raw SQL exception bubbling up.
        List<Book> existing = bookDAO.searchBooks(book.getIsbn());
        boolean duplicate = existing.stream().anyMatch(b -> b.getIsbn().equalsIgnoreCase(book.getIsbn()));
        if (duplicate) {
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
        bookDAO.updateBook(book);
    }

    public void deleteBook(int bookId) {
        bookDAO.deleteBook(bookId);
    }

    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return bookDAO.getAllBooks();
        }
        return bookDAO.searchBooks(keyword.trim());
    }

    // ── Validation ──────────────────────────────────────────
    private void validateBook(Book book) {
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        if (book.getAuthor() == null || book.getAuthor().isBlank()) {
            throw new IllegalArgumentException("Author cannot be empty.");
        }
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            throw new IllegalArgumentException("ISBN cannot be empty.");
        }
        if (book.getTotalCopies() < 1) {
            throw new IllegalArgumentException("Total copies must be at least 1.");
        }
    }
}