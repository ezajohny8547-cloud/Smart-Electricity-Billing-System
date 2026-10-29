
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class LoginPage extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    private static final Color NAVY = new Color(25, 55, 85);
    private static final Color GREEN = new Color(25, 155, 95);
    private static final Color LIGHT_BG = new Color(245, 248, 252);
    private static final Color RED = new Color(220, 38, 38);

    public LoginPage() {

        setTitle("SEUBS - Login");
        setSize(450, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(25, 35, 25, 35)
        );

        // Heading
        JLabel title = new JLabel(
            "SMART ELECTRICITY BILLING",
            SwingConstants.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Login to your account",
            SwingConstants.CENTER
        );
        subtitle.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel headingPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        headingPanel.setBackground(LIGHT_BG);
        headingPanel.add(title);
        headingPanel.add(subtitle);

        // Login form
        JPanel formPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        formPanel.setBackground(LIGHT_BG);

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        JCheckBox showPassword = new JCheckBox("Show password");
        showPassword.setBackground(LIGHT_BG);

        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('\u2022');
            }
        });

        formPanel.add(requiredLabel("Username / Consumer ID:"));
        formPanel.add(usernameField);

        formPanel.add(requiredLabel("Password:"));
        formPanel.add(passwordField);

        formPanel.add(showPassword);

        // Login button
        JButton loginButton = new JButton("LOGIN");
        loginButton.setBackground(GREEN);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 15));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(LIGHT_BG);
        bottomPanel.add(loginButton, BorderLayout.CENTER);

        mainPanel.add(headingPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Button actions
        loginButton.addActionListener(e -> login());

        passwordField.addActionListener(e -> login());

        usernameField.addActionListener(
            e -> passwordField.requestFocus()
        );
    }

    // Red asterisk for mandatory fields
    private JLabel requiredLabel(String text) {
        return new JLabel(
            "<html>" + text
            + " <font color='#DC2626'>*</font></html>"
        );
    }

    // Login validation and authentication
    private void login() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please fill in all mandatory fields marked with *.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );

            if (username.isEmpty()) {
                usernameField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
            return;
        }

        /*
         * Admin login:
         * Username: admin
         * Password: admin123
         */
        if (username.equals("admin") && password.equals("admin123")) {
            openDashboard(new AdminDashboard());
            return;
        }

        // Try Staff login
        if (verifyStaff(username, password)) {
            openDashboard(new Dashboard());
            return;
        }

        // Try Consumer login
        int customerId = verifyConsumer(username, password);

        if (customerId > 0) {
            openDashboard(new ConsumerDashboard(customerId));
            return;
        }

        showLoginError();
    }

    // Verify staff login
    private boolean verifyStaff(String username, String password) {

        String sql =
            "SELECT password_hash FROM staff_accounts WHERE username = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    return storedHash != null
                        && storedHash.equals(hashPassword(password));
                }
            }

        } catch (Exception ex) {
            showDatabaseError(ex);
        }

        return false;
    }

    // Verify consumer login
    private int verifyConsumer(String consumerId, String password) {

        String sql =
            "SELECT c.customer_id, "
            + "COALESCE(a.password_hash, c.password_hash) AS saved_hash "
            + "FROM customers c "
            + "LEFT JOIN consumer_accounts a "
            + "ON c.customer_id = a.customer_id "
            + "WHERE c.consumer_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, consumerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("saved_hash");

                    if (storedHash != null
                            && storedHash.equals(hashPassword(password))) {
                        return rs.getInt("customer_id");
                    }
                }
            }

        } catch (Exception ex) {
            showDatabaseError(ex);
        }

        return -1;
    }

    // SHA-256 password hashing
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

    // Open the correct dashboard
    private void openDashboard(JFrame dashboard) {
        dashboard.setVisible(true);
        dispose();
    }

    // Invalid login message
    private void showLoginError() {
        JOptionPane.showMessageDialog(
            this,
            "Invalid username / Consumer ID or password.",
            "Login Failed",
            JOptionPane.ERROR_MESSAGE
        );
    }

    // Database error message
    private void showDatabaseError(Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Login verification failed:\n" + ex.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );

        ex.printStackTrace();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginPage().setVisible(true);
        });
    }
}
