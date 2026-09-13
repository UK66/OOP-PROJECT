package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents a borrow/return transaction.
 * Demonstrates: Encapsulation, business logic (fine calculation).
 */
public class Transaction {

    /** Fine rate: amount charged per overdue day. */
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("2.00");

    /** Standard loan period in days. */
    public static final int LOAN_PERIOD_DAYS = 14;

    public enum Status { ISSUED, RETURNED}

    private int        transactionId;
    private int        bookId;
    private int        memberId;
    private LocalDate  issueDate;
    private LocalDate  dueDate;
    private LocalDate  returnDate;      // null if not yet returned
    private BigDecimal fineAmount;
    private Status     status;

    // Convenience denormalized fields (populated by JOIN queries in DAO)
    private String     bookTitle;
    private String     memberName;

    // ── Constructors ───────────────────────────────────────
    public Transaction() {
        this.issueDate  = LocalDate.now();
        this.dueDate    = LocalDate.now().plusDays(LOAN_PERIOD_DAYS);
        this.fineAmount = BigDecimal.ZERO;
        this.status     = Status.ISSUED;
    }

    public Transaction(int bookId, int memberId) {
        this();
        this.bookId   = bookId;
        this.memberId = memberId;
    }

    // Full constructor for DB mapping
    public Transaction(int transactionId, int bookId, int memberId,
                       LocalDate issueDate, LocalDate dueDate, LocalDate returnDate,
                       BigDecimal fineAmount, Status status) {
        this.transactionId = transactionId;
        this.bookId        = bookId;
        this.memberId      = memberId;
        this.issueDate     = issueDate;
        this.dueDate       = dueDate;
        this.returnDate    = returnDate;
        this.fineAmount    = fineAmount;
        this.status        = status;
    }

    // ── Business logic ─────────────────────────────────────

    /**
     * Calculates the fine on return.
     * If returned after the due date, charges FINE_PER_DAY × overdue days.
     *
     * @param actualReturnDate The date the book is being returned.
     * @return Fine amount (BigDecimal). Zero if returned on time.
     */
    public BigDecimal calculateFine(LocalDate actualReturnDate) {
        if (actualReturnDate.isAfter(dueDate)) {
            long overdueDays = ChronoUnit.DAYS.between(dueDate, actualReturnDate);
            return FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Marks this transaction as returned, calculates and stores the fine.
     *
     * @param actualReturnDate The date the book is returned.
     */
    public void processReturn(LocalDate actualReturnDate) {
        this.returnDate  = actualReturnDate;
        this.fineAmount  = calculateFine(actualReturnDate);
        this.status      = Status.RETURNED;
    }

    /** Returns true if the due date has passed and book is not yet returned. */
    public boolean isOverdue() {
        return status == Status.ISSUED && LocalDate.now().isAfter(dueDate);
    }

    // ── Getters & Setters ──────────────────────────────────
    public int getTransactionId()                            { return transactionId; }
    public void setTransactionId(int transactionId)          { this.transactionId = transactionId; }

    public int getBookId()                                   { return bookId; }
    public void setBookId(int bookId)                        { this.bookId = bookId; }

    public int getMemberId()                                 { return memberId; }
    public void setMemberId(int memberId)                    { this.memberId = memberId; }

    public LocalDate getIssueDate()                          { return issueDate; }
    public void setIssueDate(LocalDate issueDate)            { this.issueDate = issueDate; }

    public LocalDate getDueDate()                            { return dueDate; }
    public void setDueDate(LocalDate dueDate)                { this.dueDate = dueDate; }

    public LocalDate getReturnDate()                         { return returnDate; }
    public void setReturnDate(LocalDate returnDate)          { this.returnDate = returnDate; }

    public BigDecimal getFineAmount()                        { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount)         { this.fineAmount = fineAmount; }

    public Status getStatus()                                { return status; }
    public void setStatus(Status status)                     { this.status = status; }

    public String getBookTitle()                             { return bookTitle; }
    public void setBookTitle(String bookTitle)               { this.bookTitle = bookTitle; }

    public String getMemberName()                            { return memberName; }
    public void setMemberName(String memberName)             { this.memberName = memberName; }

    @Override
    public String toString() {
        return String.format("Transaction[%d] Book:%d Member:%d | Issued:%s Due:%s | Status:%s Fine:₹%.2f",
                transactionId, bookId, memberId, issueDate, dueDate, status, fineAmount);
    }
}
