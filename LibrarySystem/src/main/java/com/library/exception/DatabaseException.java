package com.library.exception;

import java.sql.SQLException;

/**
 * Custom runtime exception for database operations.
 * Translates low-level SQLExceptions into user-friendly error messages
 * to prevent exposing raw SQL errors or stack traces to end users.
 */
public class DatabaseException extends RuntimeException {

    private final int errorCode;
    private final String sqlState;

    public DatabaseException(String message) {
        super(message);
        this.errorCode = 0;
        this.sqlState = null;
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        if (cause instanceof SQLException sqle) {
            this.errorCode = sqle.getErrorCode();
            this.sqlState = sqle.getSQLState();
        } else {
            this.errorCode = 0;
            this.sqlState = null;
        }
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getSqlState() {
        return sqlState;
    }

    /**
     * Factory method to convert an SQLException into a DatabaseException
     * with an end-user-friendly message.
     */
    public static DatabaseException fromSQLException(String actionDescription, SQLException ex) {
        String friendlyMessage = translateSQLException(actionDescription, ex);
        return new DatabaseException(friendlyMessage, ex);
    }

    /**
     * Translates vendor-specific error codes and SQL states into plain English.
     */
    public static String translateSQLException(String actionDescription, SQLException ex) {
        int code = ex.getErrorCode();
        String state = ex.getSQLState();
        String rawMsg = ex.getMessage() != null ? ex.getMessage() : "";

        // Communications link failure / server down / connection refused
        if ((state != null && state.startsWith("08"))
                || rawMsg.contains("Communications link failure")
                || rawMsg.contains("Connection refused")
                || rawMsg.contains("ConnectException")
                || rawMsg.contains("SocketException")) {
            return "Database server is unreachable. Please ensure MySQL is running on localhost:3306.";
        }

        // Access denied / bad credentials (MySQL error 1045)
        if (code == 1045 || rawMsg.contains("Access denied for user")) {
            return "Database authentication failed. Please check credentials in db.properties.";
        }

        // Duplicate entry (MySQL error 1062)
        if (code == 1062 || rawMsg.contains("Duplicate entry")) {
            if (rawMsg.contains("isbn") || rawMsg.contains("books.isbn")) {
                return "A book with this ISBN already exists.";
            }
            if (rawMsg.contains("email") || rawMsg.contains("members.email")) {
                return "A member with this email address already exists.";
            }
            return "A duplicate entry already exists in the database.";
        }

        // Foreign key constraint failure on delete/update (MySQL error 1451)
        if (code == 1451 || rawMsg.contains("foreign key constraint fails")) {
            return "Cannot delete or alter this record because related borrowing transactions exist.";
        }

        // Foreign key constraint failure on insert/update (MySQL error 1452)
        if (code == 1452) {
            return "The referenced book or member does not exist in the database.";
        }

        // Check constraint violated (MySQL error 3819)
        if (code == 3819 || rawMsg.contains("chk_copies")) {
            return "Available copies cannot be negative or exceed total copies.";
        }

        // Data too long / truncation (MySQL error 1406)
        if (code == 1406 || rawMsg.contains("Data truncation") || rawMsg.contains("Data too long")) {
            return "One of the entered values exceeds the maximum permitted field length in the database.";
        }

        // Fallback friendly message
        if (actionDescription != null && !actionDescription.isBlank()) {
            return actionDescription + ": " + ex.getMessage();
        }
        return "Database error: " + ex.getMessage();
    }
}
