-- ============================================================
-- Library Management System — Database Schema
-- Run this script in MySQL before launching the application.
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE library_db;

-- ------------------------------------------------------------
-- Table: books
-- Mapped to: com.library.model.Book
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    book_id         INT             PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(150)    NOT NULL,
    author          VARCHAR(100)    NOT NULL,
    isbn            VARCHAR(20)     UNIQUE,
    category        VARCHAR(50),
    total_copies    INT             NOT NULL DEFAULT 1,
    available_copies INT            NOT NULL DEFAULT 1,
    added_date      DATE            DEFAULT (CURRENT_DATE),
    CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

-- ------------------------------------------------------------
-- Table: members
-- Mapped to: com.library.model.Member
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS members (
    member_id       INT             PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(100)    UNIQUE NOT NULL,
    contact         VARCHAR(20),
    membership_date DATE            DEFAULT (CURRENT_DATE),
    is_active       TINYINT(1)      NOT NULL DEFAULT 1
);

-- ------------------------------------------------------------
-- Table: transactions
-- Mapped to: com.library.model.Transaction
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id  INT             PRIMARY KEY AUTO_INCREMENT,
    book_id         INT             NOT NULL,
    member_id       INT             NOT NULL,
    issue_date      DATE            NOT NULL DEFAULT (CURRENT_DATE),
    due_date        DATE            NOT NULL,
    return_date     DATE,
    fine_amount     DECIMAL(6,2)    NOT NULL DEFAULT 0.00,
    status          ENUM('ISSUED','RETURNED','OVERDUE') NOT NULL DEFAULT 'ISSUED',

    CONSTRAINT fk_trans_book   FOREIGN KEY (book_id)   REFERENCES books(book_id)   ON UPDATE CASCADE,
    CONSTRAINT fk_trans_member FOREIGN KEY (member_id) REFERENCES members(member_id) ON UPDATE CASCADE
);

-- ------------------------------------------------------------
-- Indexes for common query patterns
-- ------------------------------------------------------------
CREATE INDEX idx_books_title    ON books(title);
CREATE INDEX idx_books_author   ON books(author);
CREATE INDEX idx_books_isbn     ON books(isbn);
CREATE INDEX idx_members_email  ON members(email);
CREATE INDEX idx_trans_status   ON transactions(status);
CREATE INDEX idx_trans_member   ON transactions(member_id);
CREATE INDEX idx_trans_book     ON transactions(book_id);

-- ------------------------------------------------------------
-- Sample seed data (optional — remove before production demo)
-- ------------------------------------------------------------
INSERT INTO books (title, author, isbn, category, total_copies, available_copies) VALUES
    ('Clean Code', 'Robert C. Martin', '978-0132350884', 'Programming', 3, 3),
    ('The Pragmatic Programmer', 'David Thomas', '978-0135957059', 'Programming', 2, 2),
    ('Introduction to Algorithms', 'Cormen et al.', '978-0262046305', 'Computer Science', 2, 2),
    ('Design Patterns', 'Gang of Four', '978-0201633610', 'Software Engineering', 1, 1);

INSERT INTO members (name, email, contact, membership_date) VALUES
    ('Alice Johnson', 'alice@example.com', '9876543210', CURRENT_DATE),
    ('Bob Smith',     'bob@example.com',   '9123456789', CURRENT_DATE);
