package com.library.dao;

import com.library.DBConnection;
import com.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO implements IBookDAO {

    @Override
    public void addBook(Book book) {
        String sql = "INSERT INTO books (title, author, isbn, category, total_copies, available_copies) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getIsbn());
            ps.setString(4, book.getCategory());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());

            ps.executeUpdate();

            // Retrieve the auto-generated book_id and set it on the object
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    book.setBookId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add book: " + e.getMessage(), e);
        }
    }

    @Override
    public Book getBookById(int bookId) {
        String sql = "SELECT * FROM books WHERE book_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch book " + bookId + ": " + e.getMessage(), e);
        }

        return null; // no book found with this id
    }

    @Override
    public List<Book> getAllBooks() {
        String sql = "SELECT * FROM books ORDER BY title";
        List<Book> books = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                books.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch books: " + e.getMessage(), e);
        }

        return books;
    }

    @Override
    public void updateBook(Book book) {
        String sql = "UPDATE books SET title = ?, author = ?, isbn = ?, category = ?, " +
                "total_copies = ?, available_copies = ? WHERE book_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getIsbn());
            ps.setString(4, book.getCategory());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());
            ps.setInt(7, book.getBookId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("No book found with id " + book.getBookId() + " to update.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update book: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBook(int bookId) {
        String sql = "DELETE FROM books WHERE book_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookId);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("No book found with id " + bookId + " to delete.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete book " + bookId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Book> searchBooks(String keyword) {
        String sql = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ? ORDER BY title";
        List<Book> books = new ArrayList<>();
        String pattern = "%" + keyword + "%";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to search books: " + e.getMessage(), e);
        }

        return books;
    }

    // ── Row mapping helper ──────────────────────────────────
    // Converts one ResultSet row into a Book object. Kept private and
    // shared across all read methods so column-to-field mapping lives
    // in exactly one place.
    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(
                rs.getInt("book_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("isbn"),
                rs.getString("category"),
                rs.getInt("total_copies"),
                rs.getInt("available_copies"),
                rs.getDate("added_date").toLocalDate());
    }
}