package com.library.service;

import com.library.dao.*;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.exception.BookNotAvailableException;
import com.library.exception.MemberNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransactionService {

    private final ITransactionDAO transactionDAO;
    private final IBookDAO bookDAO;
    private final IMemberDAO memberDAO;

    public TransactionService() {
        this(new TransactionDAO(), new BookDAO(), new MemberDAO());
    }

    public TransactionService(ITransactionDAO transactionDAO, IBookDAO bookDAO, IMemberDAO memberDAO) {
        this.transactionDAO = transactionDAO;
        this.bookDAO = bookDAO;
        this.memberDAO = memberDAO;
    }

    public List<Transaction> getAllActiveTransactions() {
        return transactionDAO.getActiveTransactions();
    }

    /**
     * Issues a book to a member, after validating:
     * - the book exists and has an available copy
     * - the member exists and is active
     * - the member has no overdue books
     * - the member has no outstanding fines
     */
    public Transaction issueBook(int bookId, int memberId) {
        Book book = bookDAO.getBookById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Selected book does not exist.");
        }
        if (!book.isAvailable()) {
            throw new BookNotAvailableException("\"" + book.getTitle() + "\" has no available copies right now.");
        }

        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
            throw new MemberNotFoundException("Selected member does not exist.");
        }
        if (!member.isActive()) {
            throw new IllegalStateException(member.getName() + "'s membership is currently inactive.");
        }

        List<Transaction> history = transactionDAO.getTransactionsByMember(memberId);

        // Edge case: Block borrowing if member has currently overdue books
        boolean hasOverdueBook = history.stream().anyMatch(Transaction::isOverdue);
        if (hasOverdueBook) {
            throw new IllegalStateException(
                    member.getName() + " has overdue book(s) that must be returned before borrowing new books.");
        }

        // Edge case: Block borrowing if member has any outstanding unpaid fine
        BigDecimal totalUnpaidFine = history.stream()
                .filter(t -> t.getStatus() == Transaction.Status.RETURNED)
                .map(Transaction::getFineAmount)
                .filter(fine -> fine != null && fine.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalUnpaidFine.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(String.format(
                    "%s has an outstanding fine of ₹%.2f and cannot borrow until it is paid.",
                    member.getName(), totalUnpaidFine));
        }

        Transaction transaction = new Transaction(bookId, memberId);
        transactionDAO.issueBook(transaction);
        return transaction;
    }

    /**
     * Returns a book, calculating any overdue fine automatically.
     */
    public Transaction returnBook(int transactionId) {
        return returnBook(transactionId, LocalDate.now());
    }

    /**
     * Returns a book on a specific date (allows testing past/future returns).
     */
    public Transaction returnBook(int transactionId, LocalDate returnDate) {
        Transaction transaction = findTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found (ID: " + transactionId + ").");
        }
        if (transaction.getStatus() == Transaction.Status.RETURNED) {
            throw new IllegalStateException("This book has already been returned.");
        }

        transaction.processReturn(returnDate); // calculates fine, sets status
        transactionDAO.returnBook(transaction);
        return transaction;
    }

    /**
     * Clears an unpaid fine on a returned transaction.
     */
    public void payFine(int transactionId) {
        Transaction t = findTransactionById(transactionId);
        if (t == null) {
            throw new IllegalArgumentException("Transaction not found.");
        }
        transactionDAO.clearFine(transactionId);
    }

    /**
     * Clears all unpaid fines for a member.
     */
    public void clearAllFinesForMember(int memberId) {
        Member m = memberDAO.getMemberById(memberId);
        if (m == null) {
            throw new IllegalArgumentException("Member not found.");
        }
        transactionDAO.clearAllFinesForMember(memberId);
    }

    public List<Transaction> getActiveIssuesForMember(int memberId) {
        return transactionDAO.getTransactionsByMember(memberId).stream()
                .filter(t -> t.getStatus() == Transaction.Status.ISSUED)
                .toList();
    }

    public List<Transaction> getOverdueTransactions() {
        return transactionDAO.getOverdueTransactions();
    }

    public Transaction findTransactionById(int transactionId) {
        return transactionDAO.getTransactionById(transactionId);
    }
}