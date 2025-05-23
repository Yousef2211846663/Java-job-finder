package com.mycompany.job_finder;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Job_Finder extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/job_finder";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // Main application variables
    private Connection connection;
    private JTable jobTable;
    private DefaultTableModel tableModel;
    private JTextField titleField, companyField, locationField, websiteField, emailField, tagsField;
    private JTextArea descriptionArea;
    private int currentUserId;
    private String currentUserName;

    // Constructor for main application
    public Job_Finder(int userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        initializeDatabase();
        initializeUI();
    }

    // Default constructor for initial launch
    public Job_Finder() {
        // Show login form instead of main UI
        new LoginForm();
        // Don't initialize the main UI yet
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connected successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage());
        }
    }

    private void initializeUI() {
        setTitle("Job Finder Application - Welcome " + currentUserName);
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top panel with navigation
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addJobButton = new JButton("Add Job");
        JButton searchJobsButton = new JButton("Search Jobs");
        navPanel.add(addJobButton);
        navPanel.add(searchJobsButton);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel userLabel = new JLabel("Welcome, " + currentUserName);
        JButton logoutButton = new JButton("Logout");
        userPanel.add(userLabel);
        userPanel.add(logoutButton);

        topPanel.add(navPanel, BorderLayout.WEST);
        topPanel.add(userPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Card layout for different pages
        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);

        // Add Job Panel
        JPanel addJobPanel = createAddJobPanel();
        cardPanel.add(addJobPanel, "ADD_JOB");

        // Search Jobs Panel
        JPanel searchJobsPanel = createSearchJobsPanel();
        cardPanel.add(searchJobsPanel, "SEARCH_JOBS");

        mainPanel.add(cardPanel, BorderLayout.CENTER);

        // Add button listeners
        addJobButton.addActionListener(e -> cardLayout.show(cardPanel, "ADD_JOB"));
        searchJobsButton.addActionListener(e -> {
            loadAllJobs(); // Refresh the jobs list
            cardLayout.show(cardPanel, "SEARCH_JOBS");
        });
        logoutButton.addActionListener(e -> logout());

        // Add main panel to frame
        add(mainPanel);

        // Pack and center the frame
        pack();//window to fit the preferred size
        setLocationRelativeTo(null);// This method centers the window
        
        // Start with add job panel
        cardLayout.show(cardPanel, "ADD_JOB");
    }

    private JPanel createAddJobPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Initialize form fields
        titleField = new JTextField(30);
        companyField = new JTextField(30);
        locationField = new JTextField(30);
        websiteField = new JTextField(30);
        emailField = new JTextField(30);
        tagsField = new JTextField(30);
        descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true); //enables line wrapping in the JTextArea.
        descriptionArea.setWrapStyleWord(true);// Controls how the lines are wrapped

        // Add components with labels
        addFormField(formPanel, "Job Title:", titleField, gbc, 0); //ترتيب
        addFormField(formPanel, "Company:", companyField, gbc, 1);
        addFormField(formPanel, "Location:", locationField, gbc, 2);
        addFormField(formPanel, "Website:", websiteField, gbc, 3);
        addFormField(formPanel, "Email:", emailField, gbc, 4);
        addFormField(formPanel, "Tags (comma-separated):", tagsField, gbc, 5);
        
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(descriptionArea), gbc); //scrollpane like scrollbar

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton submitButton = new JButton("Submit Job");
        JButton clearButton = new JButton("Clear Form");

        submitButton.addActionListener(e -> addJob());
        clearButton.addActionListener(e -> clearFields());

        buttonPanel.add(submitButton);
        buttonPanel.add(clearButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addFormField(JPanel panel, String label, JTextField field, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private JPanel createSearchJobsPanel() { //layout for panel
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Search panel at top
        JPanel searchPanel = createSearchPanel();
        panel.add(searchPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Title", "Company", "Location", "Email", "Website", "Tags"};
        tableModel = new DefaultTableModel(columns, 0) {//stores data in علي هيئة جدول
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        jobTable = new JTable(tableModel);
        jobTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set column widths
        jobTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        jobTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Title
        jobTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Company
        jobTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Location
        jobTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Email
        jobTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Website
        jobTable.getColumnModel().getColumn(6).setPreferredWidth(200); // Tags

        // Custom renderer to highlight user's own jobs
        jobTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Get the job ID and query for user_id
                int jobId = (Integer) table.getModel().getValueAt(row, 0);
                try {
                    String sql = "SELECT user_id FROM jobs WHERE id = ?";
                    PreparedStatement pstmt = connection.prepareStatement(sql);
                    pstmt.setInt(1, jobId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        int jobUserId = rs.getInt("user_id");
                        if (jobUserId == currentUserId) {
                            // Light green background for user's own jobs
                            c.setBackground(isSelected ? new Color(144, 238, 144) : new Color(220, 255, 220));
                            ((JComponent) c).setToolTipText("Your job posting");
                        } else {
                            c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                            ((JComponent) c).setToolTipText(null);
                        }
                    }
                    rs.close();
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return c;
            }
        });

        // Add double-click listener to show job details
        jobTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = jobTable.getSelectedRow();
                    if (row >= 0) {
                        int jobId = (Integer) tableModel.getValueAt(row, 0); // Get the job ID
                        String sql = "SELECT user_id FROM jobs WHERE id = ?";
                        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                            pstmt.setInt(1, jobId);
                            ResultSet rs = pstmt.executeQuery();
                            if (rs.next()) {
                                int jobUserId = rs.getInt("user_id");
                                showJobDetails(row, jobUserId == currentUserId); // Pass whether this is user's own job
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Error fetching job details: " + ex.getMessage());
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(jobTable);
        scrollPane.setPreferredSize(new Dimension(750, 400));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Add a legend panel at the bottom
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel colorBox = new JPanel();
        colorBox.setPreferredSize(new Dimension(20, 20));
        colorBox.setBackground(new Color(220, 255, 220));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        legendPanel.add(colorBox);
        legendPanel.add(new JLabel("Your job postings"));
        
        panel.add(legendPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        
        JTextField searchField = new JTextField(30);
        searchField.setToolTipText("Search in title, company, location, or tags");
        
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchJobs(searchField.getText())); //get text -> the searched for text
        
        JButton showAllButton = new JButton("Show All");
        showAllButton.addActionListener(e -> loadAllJobs());
        
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(showAllButton);
        
        // Add a help label to explain search functionality
        JLabel helpLabel = new JLabel("(Searches in title, company, location, and tags)");
        helpLabel.setFont(new Font(helpLabel.getFont().getName(), Font.ITALIC, 12));
        helpLabel.setForeground(Color.GRAY);
        searchPanel.add(helpLabel);
        
        return searchPanel;
    }

    private void searchJobs(String searchTerm) { //using lower so u can write caps still get same rsults
        String sql = "SELECT * FROM jobs WHERE " +
                    "LOWER(title) LIKE ? OR " +
                    "LOWER(company) LIKE ? OR " +
                    "LOWER(location) LIKE ? OR " +
                    "LOWER(tags) LIKE ? " +
                    "ORDER BY CASE WHEN user_id = ? THEN 0 ELSE 1 END, id DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%"; // use % so its anywhere in code % 
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            pstmt.setInt(5, currentUserId);

            ResultSet rs = pstmt.executeQuery();
            updateTableWithResults(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error searching jobs: " + e.getMessage());
        }
    }

    private void showJobDetails(int selectedRow, boolean isOwnJob) {    //
        if (selectedRow < 0) return;

        int jobId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String title = (String) tableModel.getValueAt(selectedRow, 1);
        String company = (String) tableModel.getValueAt(selectedRow, 2);
        String location = (String) tableModel.getValueAt(selectedRow, 3);
        String email = (String) tableModel.getValueAt(selectedRow, 4);
        String website = (String) tableModel.getValueAt(selectedRow, 5);
        String tags = (String) tableModel.getValueAt(selectedRow, 6);
        String description = getJobDescription(jobId);

        // Create and show details dialog
        JDialog dialog = new JDialog(this, "Job Details", true);// modal true to not interact with anything other than dialog
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel detailsPanel = new JPanel(new GridBagLayout()); // gridbaglayout rows and column
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Create text fields
        JTextField titleField = new JTextField(title); //takes value from above line 327
        JTextField companyField = new JTextField(company);
        JTextField locationField = new JTextField(location);
        JTextField emailField = new JTextField(email);
        JTextField websiteField = new JTextField(website);
        JTextField tagsField = new JTextField(tags);
        JTextArea descArea = new JTextArea(description, 5, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);

        // Set fields editable only if it's user's own job
        titleField.setEditable(isOwnJob); //if isOwnJob -> true (the one in parameter) then u can edit fields
        companyField.setEditable(isOwnJob);
        locationField.setEditable(isOwnJob);
        emailField.setEditable(isOwnJob);
        websiteField.setEditable(isOwnJob);
        tagsField.setEditable(isOwnJob);
        descArea.setEditable(isOwnJob);

        // Add components with labels
        addDetailField(detailsPanel, "Title:", titleField, gbc, 0); //gbc refrence GridBagConstraints
        addDetailField(detailsPanel, "Company:", companyField, gbc, 1);
        addDetailField(detailsPanel, "Location:", locationField, gbc, 2);
        addDetailField(detailsPanel, "Email:", emailField, gbc, 3);
        addDetailField(detailsPanel, "Website:", websiteField, gbc, 4);
        addDetailField(detailsPanel, "Tags:", tagsField, gbc, 5);

        gbc.gridx = 0; gbc.gridy = 6;
        detailsPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(new JScrollPane(descArea), gbc);

        dialog.add(detailsPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        // Only show edit and delete buttons for user's own jobs
        if (isOwnJob) {
            JButton saveButton = new JButton("Save Changes");// creates save button in case isOwnJob is true
            saveButton.addActionListener(e -> {
                updateJob(jobId, titleField.getText(), companyField.getText(), //update job (SEND NEW DATA) is explained in the function below
                         locationField.getText(), websiteField.getText(), 
                         emailField.getText(), descArea.getText(), tagsField.getText());
                dialog.dispose();//close it then below to load it all again
                loadAllJobs();// explained below
            });

            JButton deleteButton = new JButton("Delete Job");
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Are you sure you want to delete this job?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteJob(jobId);// explained below
                    dialog.dispose();
                    loadAllJobs();
                }
            });

            buttonPanel.add(saveButton); // added to the panel 
            buttonPanel.add(deleteButton);
        }

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void updateJob(int jobId, String title, String company, String location, 
                         String website, String email, String description, String tags) {
        String sql = "UPDATE jobs SET title=?, company=?, location=?, website=?, email=?, description=?, tags=? WHERE id=? AND user_id=?";// here we use ? so we stop SQLinjection then put the values of them in ptsmt stamtnet
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {//use preparedstatment to interact with database 
            pstmt.setString(1, title);
            pstmt.setString(2, company);
            pstmt.setString(3, location);
            pstmt.setString(4, website);
            pstmt.setString(5, email);
            pstmt.setString(6, description);
            pstmt.setString(7, tags);
            pstmt.setInt(8, jobId);
            pstmt.setInt(9, currentUserId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Job updated successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update job. You may only edit your own jobs.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating job: " + e.getMessage());
        }
    }

    private void deleteJob(int jobId) {
        String sql = "DELETE FROM jobs WHERE id=? AND user_id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, jobId);
            pstmt.setInt(2, currentUserId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Job deleted successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete job. You may only delete your own jobs.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting job: " + e.getMessage());
        }
    }

    private void addDetailField(JPanel panel, String label, JTextField field, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; //position for where put the label
        panel.add(new JLabel(label), gbc);// Add the label to the panel
        gbc.gridx = 1;// Position for the text field
        panel.add(field, gbc);// Add the text field to the panel
    }

    private void loadAllJobs() {
        tableModel.setRowCount(0);//remove any old data
        String sql = "SELECT * FROM jobs ORDER BY CASE WHEN user_id = ? THEN 0 ELSE 1 END, id DESC"; // puts ur own jobs at 0 then go decs
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            updateTableWithResults(rs); // this function(load) is only for fetching so we use updatetable to print data
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading jobs: " + e.getMessage());
        }
    }

    private String getJobDescription(int jobId) {
        String description = "";
        String sql = "SELECT description FROM jobs WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, jobId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                description = rs.getString("description");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return description;
    }

    private void updateTableWithResults(ResultSet rs) throws SQLException {//shows the table after  loading
        tableModel.setRowCount(0);
        while (rs.next()) {
            Object[] row = {
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("company"),
                rs.getString("location"),
                rs.getString("email"),
                rs.getString("website"),
                rs.getString("tags")
            };
            tableModel.addRow(row);// while loop prints all record 
        }
    }

    private void logout() {
        dispose();// closes current window but doesnt exit app entierly
        new LoginForm();
    }

    private void addJob() {
        String sql = "INSERT INTO jobs (user_id, title, company, location, website, email, description, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setString(2, titleField.getText()); // get it from user 
            pstmt.setString(3, companyField.getText());
            pstmt.setString(4, locationField.getText());
            pstmt.setString(5, websiteField.getText());
            pstmt.setString(6, emailField.getText());
            pstmt.setString(7, descriptionArea.getText());
            pstmt.setString(8, tagsField.getText().replace(',', ';')); //here when saved in database its seprated by ; instead of ,

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Job added successfully!");
            clearFields(); //clear all fields
            loadAllJobs();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding job: " + e.getMessage());
        }
    }

    private void clearFields() {
        titleField.setText("");
        companyField.setText("");
        locationField.setText("");
        websiteField.setText("");
        emailField.setText("");
        tagsField.setText("");
        descriptionArea.setText("");
    }

    // Login Form Class
    static class LoginForm extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;

        public LoginForm() {
            initializeLoginUI();
        }

        private void initializeLoginUI() {
            setTitle("Job Finder - Login");
            setSize(400, 300);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Title
            JLabel titleLabel = new JLabel("Job Finder Login", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            // Login Panel
            JPanel loginPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Username
            gbc.gridx = 0; gbc.gridy = 0;
            loginPanel.add(new JLabel("Username:"), gbc);
            gbc.gridx = 1;
            usernameField = new JTextField(20); //get usernane
            loginPanel.add(usernameField, gbc);

            // Password
            gbc.gridx = 0; gbc.gridy = 1;
            loginPanel.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            passwordField = new JPasswordField(20); //get passsword
            loginPanel.add(passwordField, gbc);

            mainPanel.add(loginPanel, BorderLayout.CENTER);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JButton loginButton = new JButton("Login");
            JButton registerButton = new JButton("Register");

            loginButton.addActionListener(e -> attemptLogin());
            registerButton.addActionListener(e -> showRegistrationForm());

            buttonPanel.add(loginButton);
            buttonPanel.add(registerButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(mainPanel);
            pack();//window to fit the preferred size
            setVisible(true);
        }

        private void attemptLogin() {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "SELECT id, name FROM users WHERE name = ? AND password = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, password);

                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        String name = rs.getString("name");
                        Job_Finder mainApp = new Job_Finder(userId, name);
                        mainApp.setVisible(true);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "Invalid username or password", 
                            "Login Failed", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Database error: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }

        private void showRegistrationForm() {
            dispose();
            new RegistrationForm();
        }
    }

    // Registration Form Class
    static class RegistrationForm extends JFrame {
        private JTextField nameField;
        private JPasswordField passwordField;
        private JPasswordField confirmPasswordField;
        private JTextField emailField;

        public RegistrationForm() {
            initializeRegistrationUI();
        }

        private void initializeRegistrationUI() {
            setTitle("Job Finder - Registration");
            setSize(400, 350);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Title
            JLabel titleLabel = new JLabel("Register New Account", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            // Registration Panel
            JPanel registrationPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Name
            gbc.gridx = 0; gbc.gridy = 0;
            registrationPanel.add(new JLabel("Name:"), gbc);
            gbc.gridx = 1;
            nameField = new JTextField(20);
            registrationPanel.add(nameField, gbc);

            // Email
            gbc.gridx = 0; gbc.gridy = 1;
            registrationPanel.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            emailField = new JTextField(20);
            registrationPanel.add(emailField, gbc);

            // Password
            gbc.gridx = 0; gbc.gridy = 2;
            registrationPanel.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            passwordField = new JPasswordField(20);
            registrationPanel.add(passwordField, gbc);

            // Confirm Password
            gbc.gridx = 0; gbc.gridy = 3;
            registrationPanel.add(new JLabel("Confirm Password:"), gbc);
            gbc.gridx = 1;
            confirmPasswordField = new JPasswordField(20);
            registrationPanel.add(confirmPasswordField, gbc);

            mainPanel.add(registrationPanel, BorderLayout.CENTER);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JButton registerButton = new JButton("Register");
            JButton backButton = new JButton("Back to Login");

            registerButton.addActionListener(e -> register());
            backButton.addActionListener(e -> backToLogin());

            buttonPanel.add(registerButton);
            buttonPanel.add(backButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(mainPanel);
            pack();
            setVisible(true);
        }

        private void register() {
            String name = nameField.getText();
            String email = emailField.getText();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            // Validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!");
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, email);
                    pstmt.setString(3, password);

                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Registration successful! Please login.");
                    backToLogin();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Registration failed: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }

        private void backToLogin() {
            dispose();
            new LoginForm();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());//sets it on operating system.
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {// ensures the UI is initialized on the correct thread (EDT). this method ensures that the code inside the Runnable is executed on the Event Dispatch Thread (EDT),
            new LoginForm();
        });
    }
}