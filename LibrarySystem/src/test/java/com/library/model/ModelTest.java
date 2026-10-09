package com.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Model Layer Tests (OOP Pillars & Domain Logic)")
class ModelTest {

    @Test
    @DisplayName("Book: Availability and copy adjustments operate correctly")
    void testBookAvailability() {
        Book book = new Book("Effective Java", "Joshua Bloch", "978-0134685991", "Programming", 2);

        assertTrue(book.isAvailable());
        assertEquals(2, book.getTotalCopies());
        assertEquals(2, book.getAvailableCopies());

        book.decrementAvailableCopies();
        assertEquals(1, book.getAvailableCopies());
        assertTrue(book.isAvailable());

        book.decrementAvailableCopies();
        assertEquals(0, book.getAvailableCopies());
        assertFalse(book.isAvailable());

        assertThrows(IllegalStateException.class, book::decrementAvailableCopies,
                "Decrementing when availableCopies is 0 should throw IllegalStateException");

        book.incrementAvailableCopies();
        assertEquals(1, book.getAvailableCopies());
        assertTrue(book.isAvailable());

        book.incrementAvailableCopies();
        assertEquals(2, book.getAvailableCopies());

        assertThrows(IllegalStateException.class, book::incrementAvailableCopies,
                "Incrementing beyond totalCopies should throw IllegalStateException");
    }

    @Test
    @DisplayName("Member: Inheritance from Person and Polymorphism")
    void testMemberPolymorphismAndInheritance() {
        Person person = new Member(1, "John Doe", "john@example.com", "9876543210");

        assertEquals(1, person.getId());
        assertEquals("John Doe", person.getName());
        assertEquals("9876543210", person.getContact());

        // Polymorphic method call
        String display = person.getDisplayInfo();
        assertTrue(display.contains("John Doe"));
        assertTrue(display.contains("john@example.com"));

        Member member = (Member) person;
        assertTrue(member.isActive());
        member.setActive(false);
        assertFalse(member.isActive());
    }

    @Test
    @DisplayName("Transaction: Due date is 14 days after issue and return calculates zero fine on time")
    void testTransactionOnTimeReturn() {
        Transaction tx = new Transaction(10, 20);
        LocalDate today = LocalDate.now();

        assertEquals(today, tx.getIssueDate());
        assertEquals(today.plusDays(Transaction.LOAN_PERIOD_DAYS), tx.getDueDate());
        assertEquals(Transaction.Status.ISSUED, tx.getStatus());
        assertFalse(tx.isOverdue());

        // Return on or before due date
        tx.processReturn(today.plusDays(7));
        assertEquals(Transaction.Status.RETURNED, tx.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(tx.getFineAmount()));
    }

    @Test
    @DisplayName("Transaction: Overdue return correctly calculates fine (₹2 per day)")
    void testTransactionOverdueReturn() {
        Transaction tx = new Transaction(10, 20);
        LocalDate today = LocalDate.now();
        LocalDate dueDate = tx.getDueDate();

        // 5 days late
        LocalDate lateDate = dueDate.plusDays(5);
        BigDecimal expectedFine = Transaction.FINE_PER_DAY.multiply(BigDecimal.valueOf(5)); // ₹10.00

        tx.processReturn(lateDate);
        assertEquals(Transaction.Status.RETURNED, tx.getStatus());
        assertEquals(expectedFine, tx.getFineAmount());
    }
}
