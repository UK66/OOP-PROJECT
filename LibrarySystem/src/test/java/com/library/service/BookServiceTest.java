package com.library.service;

import com.library.dao.IBookDAO;
import com.library.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BookService Validation & Business Rule Tests")
class BookServiceTest {

    private InMemoryBookDAO bookDAO;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookDAO = new InMemoryBookDAO();
        bookService = new BookService(bookDAO);
    }

    @Test
    @DisplayName("addBook: Successfully adds valid book")
    void testAddBookSuccess() {
        Book book = new Book("Design Patterns", "Gang of Four", "978-0201633610", "Software", 3);
        bookService.addBook(book);

        assertEquals(1, bookService.getAllBooks().size());
        assertEquals("Design Patterns", bookService.getBookById(1).getTitle());
    }

    @Test
    @DisplayName("addBook: Empty title, author, or ISBN throws IllegalArgumentException")
    void testValidationEmptyFields() {
        assertThrows(IllegalArgumentException.class, () ->
                bookService.addBook(new Book("", "Author", "978-0201633610", "Cat", 1)));

        assertThrows(IllegalArgumentException.class, () ->
                bookService.addBook(new Book("Title", "   ", "978-0201633610", "Cat", 1)));

        assertThrows(IllegalArgumentException.class, () ->
                bookService.addBook(new Book("Title", "Author", "", "Cat", 1)));
    }

    @Test
    @DisplayName("addBook: Invalid ISBN format throws IllegalArgumentException")
    void testInvalidIsbnFormat() {
        assertThrows(IllegalArgumentException.class, () ->
                bookService.addBook(new Book("Title", "Author", "123", "Cat", 1)));

        assertThrows(IllegalArgumentException.class, () ->
                bookService.addBook(new Book("Title", "Author", "INVALID-ISBN", "Cat", 1)));
    }

    @Test
    @DisplayName("addBook: Duplicate ISBN throws IllegalArgumentException")
    void testDuplicateIsbnOnAdd() {
        Book book1 = new Book("Book 1", "Author 1", "978-0201633610", "Cat", 2);
        bookService.addBook(book1);

        Book book2 = new Book("Book 2", "Author 2", "978-0201633610", "Cat", 1);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> bookService.addBook(book2));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("updateBook: Changing to an ISBN already used by another book throws IllegalArgumentException")
    void testDuplicateIsbnOnUpdate() {
        Book book1 = new Book("Book 1", "Author 1", "978-0201633610", "Cat", 2);
        Book book2 = new Book("Book 2", "Author 2", "978-0132350884", "Cat", 1);
        bookService.addBook(book1);
        bookService.addBook(book2);

        // Try updating book 2 to book 1's ISBN
        book2.setIsbn("978-0201633610");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(book2));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("updateBook: Cannot reduce total copies below currently issued copies")
    void testReduceTotalCopiesBelowIssued() {
        Book book = new Book(1, "Clean Code", "Robert Martin", "978-0132350884", "Coding", 3, 1, LocalDate.now());
        bookDAO.addBookWithId(book); // 2 copies currently issued (3 total - 1 available)

        // Attempting to set total copies to 1 when 2 copies are issued
        Book updated = new Book(1, "Clean Code", "Robert Martin", "978-0132350884", "Coding", 1, 1, LocalDate.now());
        Exception ex = assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(updated));
        assertTrue(ex.getMessage().contains("currently issued"));
    }

    @Test
    @DisplayName("deleteBook: Cannot delete book while copies are currently issued")
    void testDeleteBookWhileIssued() {
        Book book = new Book(1, "Clean Code", "Robert Martin", "978-0132350884", "Coding", 3, 2, LocalDate.now());
        bookDAO.addBookWithId(book); // 1 copy issued

        Exception ex = assertThrows(IllegalStateException.class, () -> bookService.deleteBook(1));
        assertTrue(ex.getMessage().contains("currently issued"));
    }

    // ── In-Memory Stub for testing ────────────────────────────
    static class InMemoryBookDAO implements IBookDAO {
        private final List<Book> books = new ArrayList<>();
        private int idCounter = 1;

        @Override
        public void addBook(Book book) {
            book.setBookId(idCounter++);
            book.setAddedDate(LocalDate.now());
            books.add(book);
        }

        public void addBookWithId(Book book) {
            books.add(book);
        }

        @Override
        public Book getBookById(int bookId) {
            return books.stream().filter(b -> b.getBookId() == bookId).findFirst().orElse(null);
        }

        @Override
        public Book getBookByIsbn(String isbn) {
            String norm = isbn.replaceAll("[\\s-]", "");
            return books.stream()
                    .filter(b -> b.getIsbn().replaceAll("[\\s-]", "").equalsIgnoreCase(norm))
                    .findFirst().orElse(null);
        }

        @Override
        public List<Book> getAllBooks() {
            return new ArrayList<>(books);
        }

        @Override
        public void updateBook(Book book) {
            Book existing = getBookById(book.getBookId());
            if (existing != null) {
                books.remove(existing);
                books.add(book);
            }
        }

        @Override
        public void deleteBook(int bookId) {
            books.removeIf(b -> b.getBookId() == bookId);
        }

        @Override
        public List<Book> searchBooks(String keyword) {
            String kw = keyword.toLowerCase();
            return books.stream()
                    .filter(b -> b.getTitle().toLowerCase().contains(kw)
                            || b.getAuthor().toLowerCase().contains(kw)
                            || b.getIsbn().contains(kw))
                    .toList();
        }
    }
}
