package com.library.service;

import com.library.dao.*;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.exception.BookNotAvailableException;
import com.library.exception.MemberNotFoundException;

import java.time.LocalDate;
import java.util.List;

public class TransactionService {

    private final ITransactionDAO transactionDAO;
    private final IBookDAO bookDAO;
    private final IMemberDAO memberDAO;

    public TransactionService() {
        this.transactionDAO = new TransactionDAO();
        this.bookDAO = new BookDAO();
        this.memberDAO = new MemberDAO();
    }

    /**
     * Issues a book to a member, after validating:
     * - the book exists and has an available copy
     * - the member exists and is active
     * - the member has no outstanding overdue fine
     */
    public Transaction issueBook(int bookId, int memberId) {
        Book book = bookDAO.getBookById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Selected book no longer exists.");
        }
        if (!book.isAvailable()) {
            throw new BookNotAvailableException("\"" + book.getTitle() + "\" has no available copies right now.");
        }

        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
        throw new MemberNotFoundException("Selected member no longer exists.");
    }
        if (!member.isActive()) {
            throw new IllegalStateException(member.getName() + "'s membership is not active.");
        }

        // Business rule: block borrowing if the member has any unpaid fine
        // from a previous overdue return.
        List<Transaction> history = transactionDAO.getTransactionsByMember(memberId);
        boolean hasUnpaidFine = history.stream()
                .anyMatch(t -> t.getFineAmount() != null && t.getFineAmount().signum() > 0
                        && t.getStatus() == Transaction.Status.RETURNED);
        // Note: this flags ANY past fine as unpaid, since there's no separate
        // "fine paid" tracking in the schema yet. Good enough for this project's
        // scope — a real system would have a payment record to check instead.
        if (hasUnpaidFine) {
            throw new IllegalStateException(
                    member.getName() + " has an outstanding fine and cannot borrow until it's cleared.");
        }

        Transaction transaction = new Transaction(bookId, memberId);
        transactionDAO.issueBook(transaction);
        return transaction;
    }

    /**
     * Returns a book, calculating any overdue fine automatically.
     */
    public Transaction returnBook(int transactionId) {
        Transaction transaction = findTransactionById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found.");
        }
        if (transaction.getStatus() == Transaction.Status.RETURNED) {
            throw new IllegalStateException("This book has already been returned.");
        }

        transaction.processReturn(LocalDate.now()); // calculates fine, sets status
        transactionDAO.returnBook(transaction);
        return transaction;
    }

    public List<Transaction> getActiveIssuesForMember(int memberId) {
        return transactionDAO.getTransactionsByMember(memberId).stream()
                .filter(t -> t.getStatus() == Transaction.Status.ISSUED)
                .toList();
    }

    public List<Transaction> getOverdueTransactions() {
        return transactionDAO.getOverdueTransactions();
    }

    private Transaction findTransactionById(int transactionId) {
    return transactionDAO.getTransactionById(transactionId); 
    }
}