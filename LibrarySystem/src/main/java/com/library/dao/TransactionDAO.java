package com.library.dao;

import com.library.DBConnection;
import com.library.model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO implements ITransactionDAO {

    @Override
    public void issueBook(Transaction transaction) {
        String insertSql = "INSERT INTO transactions (book_id, member_id, issue_date, due_date, fine_amount, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        String decrementSql = "UPDATE books SET available_copies = available_copies - 1 " +
                "WHERE book_id = ? AND available_copies > 0";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // start transaction

            // 1. Insert the transaction row
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, transaction.getBookId());
                ps.setInt(2, transaction.getMemberId());
                ps.setDate(3, Date.valueOf(transaction.getIssueDate()));
                ps.setDate(4, Date.valueOf(transaction.getDueDate()));
                ps.setBigDecimal(5, transaction.getFineAmount());
                ps.setString(6, transaction.getStatus().name());

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        transaction.setTransactionId(keys.getInt(1));
                    }
                }
            }

            // 2. Decrement available_copies — the WHERE clause guards against
            // a copy that became unavailable between the app's check and this write
            try (PreparedStatement ps = conn.prepareStatement(decrementSql)) {
                ps.setInt(1, transaction.getBookId());
                int rows = ps.executeUpdate();

                if (rows == 0) {
                    // no copies were available — abort the whole operation
                    conn.rollback();
                    throw new RuntimeException("Book is no longer available (copy count is zero).");
                }
            }

            conn.commit(); // both writes succeeded — make them permanent together

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Failed to issue book: " + e.getMessage(), e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void returnBook(Transaction transaction) {
        String updateSql = "UPDATE transactions SET return_date = ?, fine_amount = ?, status = ? " +
                "WHERE transaction_id = ?";
        String incrementSql = "UPDATE books SET available_copies = available_copies + 1 " +
                "WHERE book_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Mark the transaction as returned
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setDate(1, Date.valueOf(transaction.getReturnDate()));
                ps.setBigDecimal(2, transaction.getFineAmount());
                ps.setString(3, transaction.getStatus().name());
                ps.setInt(4, transaction.getTransactionId());

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    throw new RuntimeException("No transaction found with id " + transaction.getTransactionId());
                }
            }

            // 2. Give the copy back to the pool
            try (PreparedStatement ps = conn.prepareStatement(incrementSql)) {
                ps.setInt(1, transaction.getBookId());
                ps.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Failed to return book: " + e.getMessage(), e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public List<Transaction> getTransactionsByMember(int memberId) {
        String sql = "SELECT t.*, b.title AS book_title, m.name AS member_name " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "JOIN members m ON t.member_id = m.member_id " +
                "WHERE t.member_id = ? " +
                "ORDER BY t.issue_date DESC";

        List<Transaction> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch transactions for member " + memberId + ": " + e.getMessage(),
                    e);
        }

        return results;
    }

    @Override
    public List<Transaction> getOverdueTransactions() {
        // Overdue = still ISSUED and due_date has passed — derived from facts,
        // matching the decision to drop a stored OVERDUE status.
        String sql = "SELECT t.*, b.title AS book_title, m.name AS member_name " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "JOIN members m ON t.member_id = m.member_id " +
                "WHERE t.status = 'ISSUED' AND t.due_date < CURDATE() " +
                "ORDER BY t.due_date ASC";

        List<Transaction> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch overdue transactions: " + e.getMessage(), e);
        }

        return results;
    }

    // ── Row mapping helper ──────────────────────────────────
    // Requires a query that JOINs books and members and aliases
    // book.title AS book_title, member.name AS member_name
    private Transaction mapRow(ResultSet rs) throws SQLException {
        Date returnDate = rs.getDate("return_date");

        Transaction t = new Transaction(
                rs.getInt("transaction_id"),
                rs.getInt("book_id"),
                rs.getInt("member_id"),
                rs.getDate("issue_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                returnDate != null ? returnDate.toLocalDate() : null,
                rs.getBigDecimal("fine_amount"),
                Transaction.Status.valueOf(rs.getString("status")));
        t.setBookTitle(rs.getString("book_title"));
        t.setMemberName(rs.getString("member_name"));
        return t;
    }

    // ── Transaction safety helpers ───────────────────────────
    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                // rollback failure is logged but shouldn't mask the original error
                ex.printStackTrace();
            }
        }
    }

    private void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}