
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class Customers extends JFrame {

    private JTextField nameField, addressField, phoneField, emailField;
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private JFrame dashboard;
    private int selectedCustomerId = -1;

    private static final Color NAVY = new Color(20, 43, 69);
    private static final Color GREEN = new Color(32, 164, 100);
    private static final Color LIGHT_BG = new Color(244, 247, 250);
    private static final Color RED = new Color(220, 38, 38);

    public Customers(JFrame dashboard) {
        this.dashboard = dashboard;

        setTitle("Customer Management");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel title = new JLabel("CUSTOMER MANAGEMENT");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(NAVY);
        mainPanel.add(title, BorderLayout.NORTH);

        // Customer form
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(
            BorderFactory.createTitledBorder("Customer Details")
        );
        formPanel.setBackground(LIGHT_BG);

        nameField = new JTextField();
        addressField = new JTextField();
        phoneField = new JTextField();
        emailField = new JTextField();

        formPanel.add(requiredLabel("Full Name:"));
        formPanel.add(nameField);
        formPanel.add(requiredLabel("Address:"));
        formPanel.add(addressField);
        formPanel.add(requiredLabel("Phone:"));
        formPanel.add(phoneField);
        formPanel.add(requiredLabel("Email:"));
        formPanel.add(emailField);

        // Buttons
        JButton addButton = new JButton("Add Customer");
        JButton updateButton = new JButton("Edit / Update");
        JButton deleteButton = new JButton("Delete Customer");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear");
        JButton backButton = new JButton("Back to Dashboard");

        addButton.setBackground(GREEN);
        addButton.setForeground(Color.WHITE);

        updateButton.setBackground(new Color(40, 110, 190));
        updateButton.setForeground(Color.WHITE);

        deleteButton.setBackground(new Color(190, 55, 55));
        deleteButton.setForeground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(LIGHT_BG);
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(backButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(LIGHT_BG);
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Customer table
        tableModel = new DefaultTableModel(
            new String[] {
                "Database ID", "Consumer ID", "Full Name",
                "Address", "Phone", "Email"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(tableModel);
        customerTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        customerTable.setRowHeight(23);

        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setBorder(
            BorderFactory.createTitledBorder("Saved Customers")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(LIGHT_BG);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Button actions
        addButton.addActionListener(e -> addCustomer());
        updateButton.addActionListener(e -> updateCustomer());
        deleteButton.addActionListener(e -> deleteCustomer());
        refreshButton.addActionListener(e -> loadCustomers());
        clearButton.addActionListener(e -> clearFields());

        backButton.addActionListener(e -> {
            dispose();

            if (dashboard != null) {
                dashboard.setVisible(true);
            }
        });

        // Fill form when a table row is selected
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()
                    && customerTable.getSelectedRow() != -1) {

                int viewRow = customerTable.getSelectedRow();
                int row = customerTable.convertRowIndexToModel(viewRow);

                selectedCustomerId = Integer.parseInt(
                    tableModel.getValueAt(row, 0).toString()
                );

                nameField.setText(
                    valueOrEmpty(tableModel.getValueAt(row, 2))
                );
                addressField.setText(
                    valueOrEmpty(tableModel.getValueAt(row, 3))
                );
                phoneField.setText(
                    valueOrEmpty(tableModel.getValueAt(row, 4))
                );
                emailField.setText(
                    valueOrEmpty(tableModel.getValueAt(row, 5))
                );
            }
        });

        loadCustomers();
        setVisible(true);
    }

    // RED ASTERISK FOR MANDATORY FIELDS
    private JLabel requiredLabel(String text) {
        JLabel label = new JLabel(
            "<html>" + text
            + " <font color='#DC2626'>*</font></html>"
        );
        return label;
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    // ADD CUSTOMER AND CREATE CONSUMER LOGIN
    private void addCustomer() {

        if (!validateFields()) {
            return;
        }

        String fullName = nameField.getText().trim();
        String firstName = fullName.split("\\s+")[0];
        String defaultPassword = firstName + "123";

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            String customerSql =
                "INSERT INTO customers "
                + "(full_name, address, phone, email) "
                + "VALUES (?, ?, ?, ?)";

            int customerId;

            try (PreparedStatement ps = con.prepareStatement(
                    customerSql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, fullName);
                ps.setString(2, addressField.getText().trim());
                ps.setString(3, phoneField.getText().trim());
                ps.setString(4, emailField.getText().trim());

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException(
                            "Could not obtain the new customer ID."
                        );
                    }
                    customerId = keys.getInt(1);
                }
            }

            // Generate Consumer ID, e.g. CON00001
            String consumerId = String.format("CON%05d", customerId);

            String updateIdSql =
                "UPDATE customers SET consumer_id = ? "
                + "WHERE customer_id = ?";

            try (PreparedStatement ps =
                    con.prepareStatement(updateIdSql)) {

                ps.setString(1, consumerId);
                ps.setInt(2, customerId);
                ps.executeUpdate();
            }

            // Create consumer login account
            String accountSql =
                "INSERT INTO consumer_accounts "
                + "(customer_id, password_hash) VALUES (?, ?)";

            try (PreparedStatement ps =
                    con.prepareStatement(accountSql)) {

                ps.setInt(1, customerId);
                ps.setString(2, hashPassword(defaultPassword));
                ps.executeUpdate();
            }

            con.commit();

            JOptionPane.showMessageDialog(
                this,
                "Customer added successfully!\n\n"
                + "Consumer ID: " + consumerId + "\n"
                + "Default Password: " + defaultPassword + "\n\n"
                + "Please provide these login details to the consumer.",
                "Consumer Login Created",
                JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadCustomers();

        } catch (Exception ex) {

            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
            }

            JOptionPane.showMessageDialog(
                this,
                "Could not add customer and create login account.\n"
                + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );

            ex.printStackTrace();

        } finally {

            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    // HASH PASSWORD USING SHA-256
    private String hashPassword(String password) throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] hash = md.digest(
            password.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {
            hex.append(String.format("%02x", b & 0xff));
        }

        return hex.toString();
    }

    // UPDATE CUSTOMER DETAILS
    private void updateCustomer() {

        if (selectedCustomerId == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a customer from the table first."
            );
            return;
        }

        if (!validateFields()) {
            return;
        }

        String sql =
            "UPDATE customers SET full_name = ?, address = ?, "
            + "phone = ?, email = ? WHERE customer_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nameField.getText().trim());
            ps.setString(2, addressField.getText().trim());
            ps.setString(3, phoneField.getText().trim());
            ps.setString(4, emailField.getText().trim());
            ps.setInt(5, selectedCustomerId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(
                    this, "Customer updated successfully!"
                );
            } else {
                JOptionPane.showMessageDialog(
                    this, "Customer not found."
                );
            }

            clearFields();
            loadCustomers();

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    // DELETE CUSTOMER
    private void deleteCustomer() {

        if (selectedCustomerId == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a customer from the table first."
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this customer?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            String checkSql =
                "SELECT "
                + "(SELECT COUNT(*) FROM meter_readings "
                + "WHERE customer_id = ?) + "
                + "(SELECT COUNT(*) FROM bills "
                + "WHERE customer_id = ?) AS record_count";

            try (PreparedStatement check =
                    con.prepareStatement(checkSql)) {

                check.setInt(1, selectedCustomerId);
                check.setInt(2, selectedCustomerId);

                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt("record_count") > 0) {
                        JOptionPane.showMessageDialog(
                            this,
                            "This customer has meter readings or bills. "
                            + "Deletion is blocked to protect billing history."
                        );
                        return;
                    }
                }
            }

            String accountSql =
                "DELETE FROM consumer_accounts WHERE customer_id = ?";

            try (PreparedStatement ps =
                    con.prepareStatement(accountSql)) {

                ps.setInt(1, selectedCustomerId);
                ps.executeUpdate();
            }

            String sql =
                "DELETE FROM customers WHERE customer_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, selectedCustomerId);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(
                        this, "Customer deleted successfully."
                    );
                } else {
                    JOptionPane.showMessageDialog(
                        this, "Customer not found."
                    );
                }
            }

            clearFields();
            loadCustomers();

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    // LOAD CUSTOMERS
    private void loadCustomers() {

        tableModel.setRowCount(0);

        String sql =
            "SELECT customer_id, consumer_id, full_name, "
            + "address, phone, email FROM customers "
            + "ORDER BY customer_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[] {
                    rs.getInt("customer_id"),
                    rs.getString("consumer_id"),
                    rs.getString("full_name"),
                    rs.getString("address"),
                    rs.getString("phone"),
                    rs.getString("email")
                });
            }

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    // VALIDATE FORM
    private boolean validateFields() {

        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || address.isEmpty()
                || phone.isEmpty() || email.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "Please fill in all mandatory fields marked with *.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        if (name.length() > 100) {
            JOptionPane.showMessageDialog(
                this, "Full name must not exceed 100 characters."
            );
            nameField.requestFocus();
            return false;
        }

        if (address.length() > 255) {
            JOptionPane.showMessageDialog(
                this, "Address must not exceed 255 characters."
            );
            addressField.requestFocus();
            return false;
        }

        if (!phone.matches("[0-9]{10}")) {
            JOptionPane.showMessageDialog(
                this,
                "Phone number must contain exactly 10 digits.",
                "Invalid Phone Number",
                JOptionPane.WARNING_MESSAGE
            );
            phoneField.requestFocus();
            return false;
        }

        if (email.length() > 100
                || !email.matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

            JOptionPane.showMessageDialog(
                this,
                "Please enter a valid email address (maximum 100 characters).",
                "Invalid Email",
                JOptionPane.WARNING_MESSAGE
            );
            emailField.requestFocus();
            return false;
        }

        return true;
    }

    // CLEAR FORM
    private void clearFields() {

        nameField.setText("");
        addressField.setText("");
        phoneField.setText("");
        emailField.setText("");

        selectedCustomerId = -1;
        customerTable.clearSelection();
        nameField.requestFocus();
    }

    // DISPLAY DATABASE ERRORS
    private void showDatabaseError(SQLException ex) {

        JOptionPane.showMessageDialog(
            this,
            "Database error:\n" + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );

        ex.printStackTrace();
    }
}
