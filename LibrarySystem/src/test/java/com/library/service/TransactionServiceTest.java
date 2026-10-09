package com.library.service;

import com.library.dao.*;
import com.library.exception.BookNotAvailableException;
import com.library.exception.MemberNotFoundException;
import com.library.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransactionService Business Rules & Edge Cases Tests")
class TransactionServiceTest {

    private InMemoryBookDAO bookDAO;
    private InMemoryMemberDAO memberDAO;
    private InMemoryTransactionDAO transactionDAO;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        bookDAO = new InMemoryBookDAO();
        memberDAO = new InMemoryMemberDAO();
        transactionDAO = new InMemoryTransactionDAO(bookDAO);
        transactionService = new TransactionService(transactionDAO, bookDAO, memberDAO);

        // Seed basic book and member
        Book b = new Book("Clean Code", "Robert Martin", "978-0132350884", "Tech", 1);
        b.setBookId(1);
        bookDAO.addBook(b);

        Member m = new Member(1, "Alice", "alice@test.org", "9876543210", LocalDate.now(), true);
        memberDAO.addMember(m);
    }

    @Test
    @DisplayName("issueBook: Normal path successfully creates transaction and decrements copies")
    void testIssueBookNormalPath() {
        Transaction tx = transactionService.issueBook(1, 1);

        assertNotNull(tx);
        assertEquals(Transaction.Status.ISSUED, tx.getStatus());
        assertEquals(0, bookDAO.getBookById(1).getAvailableCopies());
    }

    @Test
    @DisplayName("issueBook: Zero-copy book throws BookNotAvailableException")
    void testIssueBookZeroCopiesAvailable() {
        // Issue the only copy
        transactionService.issueBook(1, 1);

        // Register member 2
        Member m2 = new Member(2, "Bob", "bob@test.org", "123", LocalDate.now(), true);
        memberDAO.addMember(m2);

        // Member 2 attempts to borrow the zero-copy book
        assertThrows(BookNotAvailableException.class, () -> transactionService.issueBook(1, 2));
    }

    @Test
    @DisplayName("issueBook: Inactive member cannot borrow books")
    void testIssueBookInactiveMember() {
        Member m = memberDAO.getMemberById(1);
        m.setActive(false);

        Exception ex = assertThrows(IllegalStateException.class, () -> transactionService.issueBook(1, 1));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    @DisplayName("issueBook: Member with outstanding fine cannot borrow books")
    void testIssueBookMemberWithOutstandingFine() {
        // Create an existing returned transaction with fine
        Transaction fineTx = new Transaction(1, 1);
        fineTx.setTransactionId(101);
        fineTx.setStatus(Transaction.Status.RETURNED);
        fineTx.setFineAmount(new BigDecimal("10.00"));
        transactionDAO.addTransaction(fineTx);

        Exception ex = assertThrows(IllegalStateException.class, () -> transactionService.issueBook(1, 1));
        assertTrue(ex.getMessage().contains("outstanding fine"));
    }

    @Test
    @DisplayName("payFine: After paying outstanding fine, member can borrow again")
    void testPayFineAllowsBorrowing() {
        Transaction fineTx = new Transaction(1, 1);
        fineTx.setTransactionId(101);
        fineTx.setStatus(Transaction.Status.RETURNED);
        fineTx.setFineAmount(new BigDecimal("10.00"));
        transactionDAO.addTransaction(fineTx);

        // Initially blocked
        assertThrows(IllegalStateException.class, () -> transactionService.issueBook(1, 1));

        // Member pays fine
        transactionService.payFine(101);

        // Now borrowing succeeds
        assertDoesNotThrow(() -> transactionService.issueBook(1, 1));
    }

    @Test
    @DisplayName("returnBook: On-time return incurs 0 fine and increments available copies")
    void testReturnBookOnTime() {
        Transaction tx = transactionService.issueBook(1, 1);
        assertEquals(0, bookDAO.getBookById(1).getAvailableCopies());

        // Return on time
        Transaction returned = transactionService.returnBook(tx.getTransactionId(), tx.getDueDate());
        assertEquals(Transaction.Status.RETURNED, returned.getStatus());
        assertEquals(BigDecimal.ZERO, returned.getFineAmount());
        assertEquals(1, bookDAO.getBookById(1).getAvailableCopies());
    }

    @Test
    @DisplayName("returnBook: Late return calculates ₹2.00/day fine correctly")
    void testReturnBookLateCalculatesFine() {
        Transaction tx = transactionService.issueBook(1, 1);

        // Return 4 days late
        LocalDate lateDate = tx.getDueDate().plusDays(4);
        Transaction returned = transactionService.returnBook(tx.getTransactionId(), lateDate);

        assertEquals(Transaction.Status.RETURNED, returned.getStatus());
        assertEquals(new BigDecimal("8.00"), returned.getFineAmount()); // 4 days * ₹2.00
    }

    @Test
    @DisplayName("returnBook: Returning already returned transaction throws IllegalStateException")
    void testReturnAlreadyReturnedBook() {
        Transaction tx = transactionService.issueBook(1, 1);
        transactionService.returnBook(tx.getTransactionId());

        assertThrows(IllegalStateException.class, () -> transactionService.returnBook(tx.getTransactionId()));
    }

    @Test
    @DisplayName("returnBook: Returning non-existent transaction throws IllegalArgumentException")
    void testReturnNonExistentTransaction() {
        assertThrows(IllegalArgumentException.class, () -> transactionService.returnBook(9999));
    }

    // ── In-Memory Stubs ────────────────────────────────────────
    static class InMemoryBookDAO implements IBookDAO {
        private final List<Book> books = new ArrayList<>();
        @Override public void addBook(Book book) { books.add(book); }
        @Override public Book getBookById(int bookId) {
            return books.stream().filter(b -> b.getBookId() == bookId).findFirst().orElse(null);
        }
        @Override public Book getBookByIsbn(String isbn) {
            return books.stream().filter(b -> b.getIsbn().equals(isbn)).findFirst().orElse(null);
        }
        @Override public List<Book> getAllBooks() { return books; }
        @Override public void updateBook(Book book) {}
        @Override public void deleteBook(int bookId) {}
        @Override public List<Book> searchBooks(String kw) { return books; }
    }

    static class InMemoryMemberDAO implements IMemberDAO {
        private final List<Member> members = new ArrayList<>();
        @Override public void addMember(Member member) { members.add(member); }
        @Override public Member getMemberById(int memberId) {
            return members.stream().filter(m -> m.getId() == memberId).findFirst().orElse(null);
        }
        @Override public List<Member> getAllMembers() { return members; }
        @Override public void updateMember(Member member) {}
        @Override public void deleteMember(int memberId) {}
    }

    static class InMemoryTransactionDAO implements ITransactionDAO {
        private final List<Transaction> list = new ArrayList<>();
        private final InMemoryBookDAO bookDAO;
        private int idCounter = 1;

        InMemoryTransactionDAO(InMemoryBookDAO bookDAO) {
            this.bookDAO = bookDAO;
        }

        public void addTransaction(Transaction tx) {
            list.add(tx);
        }

        @Override
        public void issueBook(Transaction transaction) {
            transaction.setTransactionId(idCounter++);
            list.add(transaction);
            Book b = bookDAO.getBookById(transaction.getBookId());
            if (b != null) b.decrementAvailableCopies();
        }

        @Override
        public void returnBook(Transaction transaction) {
            Book b = bookDAO.getBookById(transaction.getBookId());
            if (b != null) b.incrementAvailableCopies();
        }

        @Override
        public List<Transaction> getTransactionsByMember(int memberId) {
            return list.stream().filter(t -> t.getMemberId() == memberId).toList();
        }

        @Override
        public List<Transaction> getOverdueTransactions() {
            return list.stream().filter(Transaction::isOverdue).toList();
        }

        @Override
        public Transaction getTransactionById(int transactionId) {
            return list.stream().filter(t -> t.getTransactionId() == transactionId).findFirst().orElse(null);
        }

        @Override
        public List<Transaction> getActiveTransactions() {
            return list.stream().filter(t -> t.getStatus() == Transaction.Status.ISSUED).toList();
        }

        @Override
        public void clearFine(int transactionId) {
            Transaction t = getTransactionById(transactionId);
            if (t != null) t.setFineAmount(BigDecimal.ZERO);
        }

        @Override
        public void clearAllFinesForMember(int memberId) {
            list.stream().filter(t -> t.getMemberId() == memberId).forEach(t -> t.setFineAmount(BigDecimal.ZERO));
        }
    }
}
