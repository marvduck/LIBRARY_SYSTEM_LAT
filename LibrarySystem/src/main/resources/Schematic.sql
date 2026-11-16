-- Safe to run more than once: every statement is IF NOT EXISTS / insert-if-missing.
-- The app also runs this file automatically on startup when the database is missing or empty.

CREATE DATABASE IF NOT EXISTS library_books
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE library_books;

CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(40) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(80) NOT NULL,
  role ENUM('student','librarian') NOT NULL DEFAULT 'student',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS books (
  book_id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255) NOT NULL,
  category VARCHAR(100) NOT NULL,
  type ENUM('Printed','E-book') NOT NULL DEFAULT 'Printed',
  isbn VARCHAR(20) NULL UNIQUE,
  total_copies INT NOT NULL DEFAULT 1,
  cover_path VARCHAR(255) NULL,   -- relative to the img folder, e.g. covers/1984.jpg
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS loans (
  loan_id INT AUTO_INCREMENT PRIMARY KEY,
  book_id INT NOT NULL,
  user_id INT NOT NULL,
  borrowed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  returned_at DATETIME NULL,
  FOREIGN KEY (book_id) REFERENCES books(book_id),
  FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- Was: (username, password_hash, display_name, is_admin) -- those columns don't exist in `users`.
INSERT INTO users (username, password_hash, full_name, role)
VALUES ('librarian', 'bf6b5bdb73240ac9f4fbfd4f35a83b2e6ec69a5f09603d', 'librarian', 'librarian')
ON DUPLICATE KEY UPDATE username = username;

-- Was: ten separate INSERTs guarded by "WHERE NOT EXISTS (SELECT 1 FROM books)".
-- After the first one ran, books was no longer empty, so the other five never inserted.
-- One statement, one check per title, and cover paths are relative to the img folder.
INSERT INTO books (title, author, category, type, total_copies, cover_path)
SELECT s.title, s.author, s.category, s.type, s.total_copies, s.cover_path
FROM (
  SELECT '1984' AS title, 'George Orwell' AS author, 'Fiction' AS category, 'Printed' AS type, 3 AS total_copies, 'covers/1984.jpg' AS cover_path
  UNION ALL SELECT 'Atomic Habits', 'James Clear', 'Self-development', 'Printed', 3, 'covers/Atomic_Habits.jpg'
  UNION ALL SELECT 'No Longer Human', 'Osamu Dazai', 'Fiction', 'Printed', 3, 'covers/No_Longer_Human.jpg'
  UNION ALL SELECT 'Noli Me Tangere', 'Jose Rizal', 'Literature', 'E-book', 3, 'covers/Noli_Me_Tangere.jpg'
  UNION ALL SELECT 'The Circle', 'Dave Eggers', 'Fiction', 'E-book', 3, 'covers/The_Circle.jpg'
  UNION ALL SELECT 'The Fall', 'Albert Camus', 'Philosophy', 'Printed', 3, 'covers/The_Fall.jpg'
  UNION ALL SELECT 'The Great Gatsby', 'F. Scott Fitzgerald', 'Fiction', 'Printed', 3, 'covers/The_Great_Gatsby.jpg'
  UNION ALL SELECT 'The Ones Who Walk Away From Omelas', 'Ursula K. Le Guin', 'Fiction', 'E-book', 3, 'covers/The_Ones_Who_Walk_Away_From_Omelas.jpg'
  UNION ALL SELECT 'The Stranger', 'Albert Camus', 'Literature', 'Printed', 3, 'covers/The_Stranger.jpg'
  UNION ALL SELECT 'The Unreality of Time', 'John McTaggart', 'Philosophy', 'E-book', 3, 'covers/The_Unreality_of_Time.jpg'
) AS s
WHERE NOT EXISTS (SELECT 1 FROM books b WHERE b.title = s.title);
