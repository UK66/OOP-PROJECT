package com.library.dao;

import com.library.model.Book;
import java.util.List;

public interface IBookDAO {
    void addBook(Book book);

    Book getBookById(int bookId);

    List<Book> getAllBooks();

    void updateBook(Book book);

    void deleteBook(int bookId);

    List<Book> searchBooks(String keyword);
}