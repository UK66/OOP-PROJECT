package com.library.model;

import java.time.LocalDate;

/**
 * Represents a book in the library catalogue.
 * Demonstrates: Encapsulation (all fields private, accessed via getters/setters).
 */
public class Book {

    private int       bookId;
    private String    title;
    private String    author;
    private String    isbn;
    private String    category;
    private int       totalCopies;
    private int       availableCopies;
    private LocalDate addedDate;

    // ── Constructors ───────────────────────────────────────
    public Book() {
        this.addedDate = LocalDate.now();
    }

    public Book(String title, String author, String isbn, String category, int totalCopies) {
        this.title           = title;
        this.author          = author;
        this.isbn            = isbn;
        this.category        = category;
        this.totalCopies     = totalCopies;
        this.availableCopies = totalCopies;   // all copies available on creation
        this.addedDate       = LocalDate.now();
    }

    public Book(int bookId, String title, String author, String isbn,
                String category, int totalCopies, int availableCopies, LocalDate addedDate) {
        this.bookId          = bookId;
        this.title           = title;
        this.author          = author;
        this.isbn            = isbn;
        this.category        = category;
        this.totalCopies     = totalCopies;
        this.availableCopies = availableCopies;
        this.addedDate       = addedDate;
    }

    // ── Business helpers ───────────────────────────────────
    /** Returns true if at least one copy can be issued. */
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    /** Call when issuing — decrements available count. */
    public void decrementAvailableCopies() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No available copies to issue for: " + title);
        }
        availableCopies--;
    }

    /** Call when returning — increments available count. */
    public void incrementAvailableCopies() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("Available copies cannot exceed total copies for: " + title);
        }
        availableCopies++;
    }

    // ── Getters & Setters ──────────────────────────────────
    public int getBookId()                       { return bookId; }
    public void setBookId(int bookId)            { this.bookId = bookId; }

    public String getTitle()                     { return title; }
    public void setTitle(String title)           { this.title = title; }

    public String getAuthor()                    { return author; }
    public void setAuthor(String author)         { this.author = author; }

    public String getIsbn()                      { return isbn; }
    public void setIsbn(String isbn)             { this.isbn = isbn; }

    public String getCategory()                  { return category; }
    public void setCategory(String category)     { this.category = category; }

    public int getTotalCopies()                        { return totalCopies; }
    public void setTotalCopies(int totalCopies)        { this.totalCopies = totalCopies; }

    public int getAvailableCopies()                        { return availableCopies; }
    public void setAvailableCopies(int availableCopies)    { this.availableCopies = availableCopies; }

    public LocalDate getAddedDate()                    { return addedDate; }
    public void setAddedDate(LocalDate addedDate)      { this.addedDate = addedDate; }

    @Override
    public String toString() {
        return String.format("Book[%d] \"%s\" by %s (ISBN: %s) — %d/%d copies available",
                bookId, title, author, isbn, availableCopies, totalCopies);
    }
}
