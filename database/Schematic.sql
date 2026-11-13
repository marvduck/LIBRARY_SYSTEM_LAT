
CREATE DATABASE IF NOT EXISTS library_books
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE library_books;

CREATE TABLE users (
      user_id INT AUTO_INCREMENT PRIMARY KEY,
      username VARCHAR(40) NOT NULL UNIQUE,
      password_hash VARCHAR(255) NOT NULL,
      full_name VARCHAR(80) NOT NULL,
      role ENUM('student','librarian') NOT NULL DEFAULT 'student',
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

CREATE TABLE books (
  book_id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255) NOT NULL, 
  category VARCHAR(100) NOT NULL,
  type ENUM('Printed','E-book') NOT NULL DEFAULT 'Printed',
  isbn VARCHAR(20) NULL UNIQUE,  
  total_copies INT NOT NULL DEFAULT 1,  
  cover_path VARCHAR(255) NULL,  
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB;

CREATE TABLE loans (
  loan_id INT AUTO_INCREMENT PRIMARY KEY, 
  book_id INT NOT NULL,
  user_id INT NOT NULL,  
  borrowed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,  
  returned_at DATETIME NULL,  
  FOREIGN KEY (book_id) REFERENCES books(book_id),  
  FOREIGN KEY (user_id) REFERENCES users(user_id));

INSERT INTO users (username, password_hash, display_name, is_admin)
VALUES ('librarian', 'bf6b5bdb73240ac9f4fbfd4f35a83b2e6ec69a5f09603d', 'librarian', 1)
ON DUPLICATE KEY UPDATE username = username;


INSERT INTO books (title, author, category, type, cover_path)
SELECT * FROM (SELECT '1984' AS t, 'George Orwell' AS a, 'Fiction' AS c, 'Printed' AS ty, 'C:\Users\desiree\OneDrive\Desktop\LIBRARYSYSTEMJAVA\IMG_DIR\Book Cover\1984.jpg' AS ex) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM books LIMIT 1);

INSERT INTO books (title, author, category, type, cover_path)
SELECT * FROM (SELECT 'Atomic Habits' AS t, 'James Clear' AS a, 'Book Guide' AS c, 'Printed' AS ty, 'C:\Users\desiree\OneDrive\Desktop\LIBRARYSYSTEMJAVA\IMG_DIR\Book Cover\Atomic Habits.jpg' AS ex) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM books LIMIT 1);

INSERT INTO books (title, author, category, type, cover_path)
SELECT * FROM (SELECT 'No Longer Human' AS t, 'Osamu Dazai' AS a, 'Fiction' AS c, 'Printed' AS ty, 'C:\Users\desiree\OneDrive\Desktop\LIBRARYSYSTEMJAVA\IMG_DIR\Book Cover\No Longer Human.jpg' AS ex) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM books LIMIT 1);

INSERT INTO books (title, author, category, type, cover_path)
SELECT * FROM (SELECT 'Noli Me Tangere' AS t, 'Jose Rizal' AS a, 'Biography' AS c, 'E-Book' AS ty, 'C:\Users\desiree\OneDrive\Desktop\LIBRARYSYSTEMJAVA\IMG_DIR\Book Cover\Noli Me Tangere.jpg' AS ex) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM books LIMIT 1);

INSERT INTO books (title, author, category, type, cover_path)
SELECT * FROM (SELECT 'The Circle' AS t, 'Dave Eggers' AS a, 'Fiction' AS c, 'E-Book' AS ty, 'C:\Users\desiree\OneDrive\Desktop\LIBRARYSYSTEMJAVA\IMG_DIR\Book Cover\The Circle.jpg' AS ex) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM books LIMIT 1);

INSERT INTO books (title, author, category, type, cover_path)
SELECT * FROM (SELECT 'The Fall' AS t, 'Albert Camus' AS a, 'Philosophy' AS c, 'Printed' AS ty, 'C:\Users\desiree\OneDrive\Desktop\LIBRARYSYSTEMJAVA\IMG_DIR\Book Cover\The Fall.jpg' AS ex) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM books LIMIT 1);