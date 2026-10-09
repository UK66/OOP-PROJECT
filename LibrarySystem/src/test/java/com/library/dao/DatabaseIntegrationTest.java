package com.library.dao;

import com.library.DBConnection;
import com.library.exception.DatabaseException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MySQL & JDBC DAO Layer Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIntegrationTest {

    private static BookDAO bookDAO;
    private static MemberDAO memberDAO;
    private static TransactionDAO transactionDAO;

    private static int testBookId;
    private static int testMemberId;
    private static int testTransactionId;

    @BeforeAll
    static void setUpAll() {
        Assumptions.assumeTrue(DBConnection.checkConnection(), "Skipping live DB tests: MySQL is not reachable");
        bookDAO = new BookDAO();
        memberDAO = new MemberDAO();
        transactionDAO = new TransactionDAO();
    }

    @Test
    @Order(1)
    @DisplayName("DB Connection: Successful live connection to library_db")
    void testLiveConnection() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    @Test
    @Order(2)
    @DisplayName("BookDAO: Add and retrieve test book in live MySQL")
    void testBookCrud() {
        String testIsbn = String.format("978%010d", Math.abs(System.currentTimeMillis() % 10000000000L));
        Book book = new Book("Integration Testing in Java", "Test Author", testIsbn, "Testing", 5);

        bookDAO.addBook(book);
        assertTrue(book.getBookId() > 0, "Generated ID should be positive");
        testBookId = book.getBookId();

        Book fetched = bookDAO.getBookById(testBookId);
        assertNotNull(fetched);
        assertEquals("Integration Testing in Java", fetched.getTitle());
        assertEquals(5, fetched.getAvailableCopies());

        // Update
        fetched.setTitle("Integration Testing in Java (2nd Edition)");
        bookDAO.updateBook(fetched);

        Book updated = bookDAO.getBookById(testBookId);
        assertEquals("Integration Testing in Java (2nd Edition)", updated.getTitle());
    }

    @Test
    @Order(3)
    @DisplayName("MemberDAO: Add and retrieve test member in live MySQL")
    void testMemberCrud() {
        String testEmail = "testuser" + (System.currentTimeMillis() % 1000000) + "@test.org";
        Member member = new Member(0, "Integration Tester", testEmail, "9998887776");

        memberDAO.addMember(member);
        assertTrue(member.getId() > 0, "Generated member ID should be positive");
        testMemberId = member.getId();

        Member fetched = memberDAO.getMemberById(testMemberId);
        assertNotNull(fetched);
        assertEquals("Integration Tester", fetched.getName());
        assertEquals(testEmail, fetched.getEmail());
    }

    @Test
    @Order(4)
    @DisplayName("TransactionDAO: Issue book decrements available copies in live DB")
    void testIssueBookLive() {
        Book beforeBook = bookDAO.getBookById(testBookId);
        assertNotNull(beforeBook, "Test book must exist before issuing");
        int initialAvailable = beforeBook.getAvailableCopies();

        Transaction tx = new Transaction(testBookId, testMemberId);
        transactionDAO.issueBook(tx);

        assertTrue(tx.getTransactionId() > 0, "Transaction should have generated ID");
        testTransactionId = tx.getTransactionId();

        Book afterBook = bookDAO.getBookById(testBookId);
        assertEquals(initialAvailable - 1, afterBook.getAvailableCopies(),
                "Available copies must decrement by 1 after issue");
    }

    @Test
    @Order(5)
    @DisplayName("TransactionDAO: Return book increments available copies in live DB")
    void testReturnBookLive() {
        Book beforeBook = bookDAO.getBookById(testBookId);
        assertNotNull(beforeBook, "Test book must exist before returning");
        int initialAvailable = beforeBook.getAvailableCopies();

        Transaction tx = transactionDAO.getTransactionById(testTransactionId);
        assertNotNull(tx);
        tx.processReturn(LocalDate.now());

        transactionDAO.returnBook(tx);

        Book afterBook = bookDAO.getBookById(testBookId);
        assertEquals(initialAvailable + 1, afterBook.getAvailableCopies(),
                "Available copies must increment by 1 after return");

        Transaction returnedTx = transactionDAO.getTransactionById(testTransactionId);
        assertEquals(Transaction.Status.RETURNED, returnedTx.getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("DatabaseException: Error translation handles SQL duplicate and connectivity errors cleanly")
    void testErrorTranslation() {
        SQLException mockDup = new SQLException("Duplicate entry 'dup' for key 'books.isbn'", "23000", 1062);
        DatabaseException ex = DatabaseException.fromSQLException("Add Book", mockDup);
        assertTrue(ex.getMessage().contains("already exists"));

        SQLException mockConn = new SQLException("Communications link failure", "08S01", 0);
        DatabaseException connEx = DatabaseException.fromSQLException("Connect", mockConn);
        assertTrue(connEx.getMessage().contains("unreachable"));
    }

    @AfterAll
    static void tearDownAll() {
        if (DBConnection.checkConnection()) {
            if (testTransactionId > 0) {
                try (Connection conn = DBConnection.getConnection();
                     var ps = conn.prepareStatement("DELETE FROM transactions WHERE transaction_id = ?")) {
                    ps.setInt(1, testTransactionId);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
            }
            if (testBookId > 0) {
                try {
                    bookDAO.deleteBook(testBookId);
                } catch (Exception ignored) {}
            }
            if (testMemberId > 0) {
                try {
                    memberDAO.deleteMember(testMemberId);
                } catch (Exception ignored) {}
            }
        }
    }
}
