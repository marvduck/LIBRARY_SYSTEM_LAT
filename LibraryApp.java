import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class LibraryApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LibraryFrame frame = new LibraryFrame();
            frame.setVisible(true);
        });
    }
}

// GUIFRAME
class LibraryFrame extends JFrame {
    private ArrayList<Book> books;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JTextArea statusArea;

    // FILEPATH
    private final String FILE_PATH = "C:\\Users\\User\\Documents\\LAT_ACTIVITIES\\TERM 2\\Book.txt";

    public LibraryFrame() {
        setTitle("Library Management System");
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        books = ReadAndCreateBook.readBooks(FILE_PATH);

        setLayout(new BorderLayout(10, 10));

        // HEADER
        JLabel titleLabel = new JLabel("Welcome to the Library Catalog", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titleLabel, BorderLayout.NORTH);

        // MAINTABLE
        String[] columnNames = { "ID", "Title", "Author", "Category", "Status" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // READONLY
            }
        };
        bookTable = new JTable(tableModel);
        refreshTable();

        JScrollPane tableScroll = new JScrollPane(bookTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Library Collection"));
        add(tableScroll, BorderLayout.CENTER);

        // RIGHTPANEL
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnSearch = new JButton("Search / View Details");
        JButton btnBorrow = new JButton("Borrow Book");
        JButton btnReturn = new JButton("Return Book");
        JButton btnAdd = new JButton("Add Book");
        JButton btnQuit = new JButton("Quit / Exit");

        buttonPanel.add(btnSearch);
        buttonPanel.add(btnBorrow);
        buttonPanel.add(btnReturn);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnQuit);

        add(buttonPanel, BorderLayout.EAST);

        // STATUSLOG
        statusArea = new JTextArea(5, 40);
        statusArea.setEditable(false);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        add(statusScroll, BorderLayout.SOUTH);

        // EVENTLISTENR
        btnSearch.addActionListener(e -> searchBookAction());
        btnBorrow.addActionListener(e -> borrowBookAction());
        btnReturn.addActionListener(e -> returnBookAction());
        btnAdd.addActionListener(e -> addBookAction());
        btnQuit.addActionListener(e -> {
            log("Thank you for using our library!");
            dispose();
            System.exit(0);
        });

        log("System initialized. Loaded " + books.size() + " books from Book.txt.");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Book b : books) {
            String statusStr = b.getisBorrowed() ? "Borrowed" : "Available";
            Object[] row = { b.getId(), b.getTitle(), b.getAuthor(), b.getCategory(), statusStr };
            tableModel.addRow(row);
        }
    }

    private void log(String message) {
        statusArea.append(message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    private Book promptForBookByTitle(String actionName) {
        String query = JOptionPane.showInputDialog(this, "Enter Book Title to " + actionName + ":", "Book Search",
                JOptionPane.QUESTION_MESSAGE);
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        query = query.trim();
        for (Book b : books) {
            if (b.getTitle().equalsIgnoreCase(query)) {
                return b;
            }
        }
        return null;
    }

    private void searchBookAction() {
        Book found = promptForBookByTitle("search");
        if (found != null) {
            JOptionPane.showMessageDialog(this, found.getDetailsString(), "Book Details",
                    JOptionPane.INFORMATION_MESSAGE);
            log("Viewed details for: " + found.getTitle());
        } else {
            JOptionPane.showMessageDialog(this, "Book not found in the catalog.", "Not Found",
                    JOptionPane.WARNING_MESSAGE);
            log("Search failed: Book not found.");
        }
    }

    private void borrowBookAction() {
        Book found = promptForBookByTitle("borrow");
        if (found != null) {
            String msg = found.borrowBook();
            log(msg);
            JOptionPane.showMessageDialog(this, msg, "Borrow Status", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();

            // SAVE CHANGES TO FILE
            ReadAndCreateBook.writeBooks(FILE_PATH, books);
        }
    }

    private void returnBookAction() {
        Book found = promptForBookByTitle("return");
        if (found != null) {
            String msg = found.returnBook();
            log(msg);
            JOptionPane.showMessageDialog(this, msg, "Return Status", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();

            // SAVE CHANGES TO FILE
            ReadAndCreateBook.writeBooks(FILE_PATH, books);
        } else {
            JOptionPane.showMessageDialog(this, "Book not found!", "Error", JOptionPane.ERROR_MESSAGE);
            log("Return failed: Book not found.");
        }
    }

    private void addBookAction() {
        JTextField categoryField = new JTextField(10);
        JTextField titleField = new JTextField(10);
        JTextField authorField = new JTextField(10);
        JTextField idField = new JTextField(10);

        JPanel myPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        myPanel.add(new JLabel("Category:"));
        myPanel.add(categoryField);
        myPanel.add(new JLabel("Title:"));
        myPanel.add(titleField);
        myPanel.add(new JLabel("Author:"));
        myPanel.add(authorField);
        myPanel.add(new JLabel("ID:"));
        myPanel.add(idField);

        int result = JOptionPane.showConfirmDialog(this, myPanel, "Add a New Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String cat = categoryField.getText().trim();
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                int id = Integer.parseInt(idField.getText().trim());

                if (cat.isEmpty() || title.isEmpty() || author.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Fields cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Book newBook = new Book(cat, title, author, id, false);
                books.add(newBook);
                refreshTable();

                // SAVE CHANGES TO FILE
                ReadAndCreateBook.writeBooks(FILE_PATH, books);

                log("Successfully added new book: " + title);
                JOptionPane.showMessageDialog(this, "Book added successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid ID format! Please enter a number.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

// CLASSBOOK
class Book {
    private String category, title, author;
    private int id;
    private boolean isBorrowed;

    public Book(String category, String title, String author, int id, boolean isBorrowed) {
        this.category = category;
        this.title = title;
        this.author = author;
        this.id = id;
        this.isBorrowed = isBorrowed;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getId() {
        return id;
    }

    public boolean getisBorrowed() {
        return isBorrowed;
    }

    public void setIsBorrowed(boolean isBorrowed) {
        this.isBorrowed = isBorrowed;
    }

    public String getDetailsString() {
        return String.format("Category: %s\nTitle: %s\nAuthor: %s\nID: %d\nIs Borrowed: %b",
                this.category, this.title, this.author, this.id, this.isBorrowed);
    }

    public String returnBook() {
        if (this.isBorrowed) {
            this.isBorrowed = false;
            return String.format("You returned the book '%s'!", this.title);
        } else {
            return String.format("The book '%s' has already been returned!", this.title);
        }
    }

    public String borrowBook() {
        if (this.isBorrowed) {
            return String.format("The book '%s' has already been borrowed!", this.title);
        } else {
            this.isBorrowed = true;
            return String.format("You borrowed the book '%s'!", this.title);
        }
    }
}

// FILEREADERWRTIER
class ReadAndCreateBook extends Book {

    public ReadAndCreateBook(String category, String title, String author, int id, boolean isBorrowed) {
        super(category, title, author, id, isBorrowed);
    }

    public static void writeBooks(String filename, ArrayList<Book> books) {
        File file = new File(filename);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // CREATEFILEIFDOESNTEXISTS
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (Book b : books) {
                pw.println(b.getCategory() + "/" + b.getTitle() + "/" + b.getAuthor() + "/" + b.getId() + "/"
                        + b.getisBorrowed());
            }
        } catch (IOException e) {
            System.out.println("Error writing to Book.txt: " + e.getMessage());
        }
    }

    public static ArrayList<Book> readBooks(String filename) {
        ArrayList<Book> books = new ArrayList<>();
        File file = new File(filename);
        if (!file.exists())
            return books;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("/");
                if (parts.length >= 5) {
                    String category = parts[0].trim();
                    String title = parts[1].trim();
                    String author = parts[2].trim();
                    int id = Integer.parseInt(parts[3].trim());
                    boolean isBorrowed = Boolean.parseBoolean(parts[4].trim());
                    books.add(new Book(category, title, author, id, isBorrowed));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading Book.txt: " + e.getMessage());
        }
        return books;
    }
}