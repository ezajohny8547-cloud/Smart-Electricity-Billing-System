
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class StaffManagement extends JFrame {

    private final Color NAVY = new Color(20, 43, 69);
    private final Color GREEN = new Color(32, 164, 100);
    private final Color BG = new Color(244, 247, 250);

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTable staffTable;
    private DefaultTableModel model;

    private JFrame parent;

    public StaffManagement(JFrame parent) {
        this.parent = parent;

        setTitle("Staff Management");
        setSize(850, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        createTable();
        createUI();
        loadStaff();
    }

    // CREATE STAFF TABLE
    private void createTable() {

        String sql =
            "CREATE TABLE IF NOT EXISTS staff_accounts (" +
            "staff_id INT AUTO_INCREMENT PRIMARY KEY, " +
            "username VARCHAR(50) NOT NULL UNIQUE, " +
            "password_hash VARCHAR(64) NOT NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(sql);

        } catch (SQLException e) {
            showError("Could not create staff table", e);
        }
    }

    private void createUI() {

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(BG);
        main.setBorder(
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        );

        JLabel heading = new JLabel("Staff Management");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Add staff accounts and manage existing staff"
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel header = new JPanel();
        header.setBackground(BG);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(heading);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);

        main.add(header, BorderLayout.NORTH);

        // Add staff form
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Add New Staff"),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        form.add(requiredLabel("Username:"));
        form.add(usernameField);
        form.add(requiredLabel("Password:"));
        form.add(passwordField);

        JButton addButton = new JButton("Add Staff");
        styleButton(addButton, GREEN);

        JButton deleteButton = new JButton("Delete Selected");
        styleButton(deleteButton, NAVY);

        JButton refreshButton = new JButton("Refresh");
        styleButton(refreshButton, NAVY);

        JButton backButton = new JButton("Back to Admin");
        styleButton(backButton, NAVY);

        JPanel buttons = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 10, 8)
        );
        buttons.setBackground(BG);
        buttons.add(addButton);
        buttons.add(deleteButton);
        buttons.add(refreshButton);
        buttons.add(backButton);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(BG);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        // Staff table
        model = new DefaultTableModel(
            new String[]{"Staff ID", "Username", "Created At"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        staffTable = new JTable(model);
        staffTable.setRowHeight(25);
        staffTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        staffTable.getTableHeader().setFont(
            new Font("Segoe UI", Font.BOLD, 13)
        );

        JScrollPane scrollPane = new JScrollPane(staffTable);
        scrollPane.setBorder(
            BorderFactory.createTitledBorder("Existing Staff")
        );

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(BG);
        center.add(top, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);

        main.add(center, BorderLayout.CENTER);
        add(main);

        addButton.addActionListener(e -> addStaff());
        deleteButton.addActionListener(e -> deleteStaff());
        refreshButton.addActionListener(e -> loadStaff());

        // Enter key submits the staff form
        passwordField.addActionListener(e -> addStaff());
        usernameField.addActionListener(
            e -> passwordField.requestFocus()
        );

        backButton.addActionListener(e -> {
            dispose();

            if (parent != null) {
                parent.setVisible(true);
            }
        });
    }

    // RED ASTERISK FOR MANDATORY FIELDS
    private JLabel requiredLabel(String text) {
        return new JLabel(
            "<html>" + text
            + " <font color='#DC2626'>*</font></html>"
        );
    }

    // ADD STAFF
    private void addStaff() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Check empty fields
        if (username.isEmpty() || password.trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "Please fill in all mandatory fields marked with *.",
                "Missing Details",
                JOptionPane.WARNING_MESSAGE
            );

            if (username.isEmpty()) {
                usernameField.requestFocus();
            } else {
                passwordField.requestFocus();
            }

            return;
        }

        // Validate username length
        if (username.length() < 3 || username.length() > 50) {

            JOptionPane.showMessageDialog(
                this,
                "Username must contain between 3 and 50 characters.",
                "Invalid Username",
                JOptionPane.WARNING_MESSAGE
            );

            usernameField.requestFocus();
            return;
        }

        // Validate username characters
        if (!username.matches("[A-Za-z0-9_.-]+")) {

            JOptionPane.showMessageDialog(
                this,
                "Username can contain only letters, numbers, "
                + "underscores, dots, and hyphens.",
                "Invalid Username",
                JOptionPane.WARNING_MESSAGE
            );

            usernameField.requestFocus();
            return;
        }

        // Validate password length
        if (password.length() < 6) {

            JOptionPane.showMessageDialog(
                this,
                "Password must contain at least 6 characters.",
                "Invalid Password",
                JOptionPane.WARNING_MESSAGE
            );

            passwordField.requestFocus();
            return;
        }

        String sql =
            "INSERT INTO staff_accounts (username, password_hash) "
            + "VALUES (?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashPassword(password));

            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                this,
                "Staff account added successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );

            usernameField.setText("");
            passwordField.setText("");
            usernameField.requestFocus();

            loadStaff();

        } catch (SQLIntegrityConstraintViolationException e) {

            JOptionPane.showMessageDialog(
                this,
                "That username already exists. Please choose another.",
                "Duplicate Username",
                JOptionPane.WARNING_MESSAGE
            );

            usernameField.requestFocus();

        } catch (SQLException e) {
            showError("Could not add staff", e);
        }
    }

    // LOAD STAFF LIST
    private void loadStaff() {

        model.setRowCount(0);

        String sql =
            "SELECT staff_id, username, created_at "
            + "FROM staff_accounts ORDER BY staff_id";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("staff_id"),
                    rs.getString("username"),
                    rs.getTimestamp("created_at")
                });
            }

        } catch (SQLException e) {
            showError("Could not load staff list", e);
        }
    }

    // DELETE STAFF
    private void deleteStaff() {

        int selectedRow = staffTable.getSelectedRow();

        if (selectedRow == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Select a staff member to delete.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int row = staffTable.convertRowIndexToModel(selectedRow);

        int staffId = (int) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete staff account '" + username + "'?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql =
            "DELETE FROM staff_accounts WHERE staff_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, staffId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Staff account deleted successfully."
                );
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Staff account not found."
                );
            }

            loadStaff();

        } catch (SQLException e) {
            showError("Could not delete staff", e);
        }
    }

    // SHA-256 PASSWORD HASHING
    private String hashPassword(String password) {

        try {
            MessageDigest md =
                MessageDigest.getInstance("SHA-256");

            byte[] hash = md.digest(
                password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(
                    String.format("%02x", b & 0xff)
                );
            }

            return result.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                "Could not hash password", e
            );
        }
    }

    // BUTTON STYLING
    private void styleButton(JButton button, Color color) {

        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setCursor(
            new Cursor(Cursor.HAND_CURSOR)
        );
    }

    // DATABASE ERROR MESSAGE
    private void showError(String operation, SQLException e) {

        JOptionPane.showMessageDialog(
            this,
            operation + ":\n" + e.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );

        e.printStackTrace();
    }
}
