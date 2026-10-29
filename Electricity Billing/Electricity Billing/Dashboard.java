
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

public class Dashboard extends JFrame {

    private final Color NAVY = new Color(20, 43, 69);
    private final Color GREEN = new Color(32, 164, 100);
    private final Color LIGHT_BG = new Color(244, 247, 250);
    private final Color WHITE = Color.WHITE;
    private final Color GRAY = new Color(110, 120, 130);

    private JLabel customerValue;
    private JLabel unpaidValue;
    private JLabel revenueValue;
    private JLabel unitsValue;

    public Dashboard() {

        setTitle("SEUBS - Staff Dashboard");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(LIGHT_BG);

        // Sidebar
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(220, 650));
        sidebar.setBackground(NAVY);

        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(NAVY);
        logoPanel.setLayout(
                new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBorder(
                BorderFactory.createEmptyBorder(25, 10, 20, 10));

        JLabel logo = new JLabel("\u26A1");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 38));
        logo.setForeground(GREEN);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("SMART");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("STAFF PORTAL");
        subtitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        subtitle.setForeground(GREEN);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoPanel.add(logo);
        logoPanel.add(title);
        logoPanel.add(Box.createVerticalStrut(4));
        logoPanel.add(subtitle);

        sidebar.add(logoPanel, BorderLayout.NORTH);

        // Navigation menu
        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(NAVY);
        menuPanel.setLayout(
                new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton dashboardButton = createMenuButton("Dashboard");
        JButton customerButton = createMenuButton("Customers");
        JButton meterBillingButton =
                createMenuButton("Meter & Billing");
        JButton billCalculationButton =
                createMenuButton("Bill Calculation");
        JButton paymentButton = createMenuButton("Payments");
        JButton reportsButton = createMenuButton("Reports");
        JButton settingsButton = createMenuButton("Settings");

        menuPanel.add(dashboardButton);
        menuPanel.add(Box.createVerticalStrut(5));
        menuPanel.add(customerButton);
        menuPanel.add(Box.createVerticalStrut(5));
        menuPanel.add(meterBillingButton);
        menuPanel.add(Box.createVerticalStrut(5));
        menuPanel.add(billCalculationButton);
        menuPanel.add(Box.createVerticalStrut(5));
        menuPanel.add(paymentButton);
        menuPanel.add(Box.createVerticalStrut(5));
        menuPanel.add(reportsButton);
        menuPanel.add(Box.createVerticalStrut(5));
        menuPanel.add(settingsButton);

        sidebar.add(menuPanel, BorderLayout.CENTER);

        // Logout
        JPanel logoutPanel = new JPanel();
        logoutPanel.setBackground(NAVY);
        logoutPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 20, 10));

        JButton logoutButton = createMenuButton("Logout");
        logoutPanel.add(logoutButton);

        sidebar.add(logoutPanel, BorderLayout.SOUTH);

        // Main content
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(LIGHT_BG);

        JPanel header = new JPanel();
        header.setBackground(LIGHT_BG);
        header.setLayout(
                new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(
                BorderFactory.createEmptyBorder(20, 30, 10, 30));

        JLabel heading = new JLabel("Staff Dashboard");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 34));
        heading.setForeground(NAVY);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subheading = new JLabel(
                "SMART UTILITY - MANAGEMENT PORTAL");
        subheading.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subheading.setForeground(GRAY);
        subheading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton refreshButton = new JButton("Refresh Dashboard");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshButton.setForeground(WHITE);
        refreshButton.setBackground(GREEN);
        refreshButton.setFocusPainted(false);
        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        header.add(heading);
        header.add(Box.createVerticalStrut(5));
        header.add(subheading);
        header.add(Box.createVerticalStrut(12));
        header.add(refreshButton);

        content.add(header, BorderLayout.NORTH);

        // Summary cards
        JPanel cards = new JPanel(new GridLayout(2, 2, 20, 20));
        cards.setBackground(LIGHT_BG);
        cards.setBorder(
                BorderFactory.createEmptyBorder(15, 30, 25, 30));

        cards.add(createCard("CUSTOMERS"));
        cards.add(createCard("UNPAID BILLS"));
        cards.add(createCard("REVENUE"));
        cards.add(createCard("UNITS USED"));

        content.add(cards, BorderLayout.CENTER);

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(content, BorderLayout.CENTER);

        add(mainPanel);

        // Initial database values
        refreshDashboard();

        // Navigation actions
        dashboardButton.addActionListener(e -> refreshDashboard());

        customerButton.addActionListener(e -> {
            new Customers(this).setVisible(true);
            setVisible(false);
        });

        meterBillingButton.addActionListener(e -> {
            new Meter_Billing(this).setVisible(true);
            setVisible(false);
        });

        billCalculationButton.addActionListener(e -> {
            new Billcalculation(this).setVisible(true);
            setVisible(false);
        });

        paymentButton.addActionListener(e -> {
            new Payments(this).setVisible(true);
            setVisible(false);
        });

        reportsButton.addActionListener(e -> {
            new Reports(this).setVisible(true);
            setVisible(false);
        });

        settingsButton.addActionListener(e -> {
            new Settings(this).setVisible(true);
            setVisible(false);
        });

        logoutButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to logout?",
                    "Logout",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                dispose();
                new LoginPage().setVisible(true);
            }
        });

        refreshButton.addActionListener(e -> refreshDashboard());
    }

    // Create summary card
    private JPanel createCard(String heading) {

        JPanel card = new JPanel();
        card.setBackground(WHITE);
        card.setLayout(
                new BoxLayout(card, BoxLayout.Y_AXIS));

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(225, 230, 235)),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)));

        JLabel titleLabel = new JLabel(heading);
        titleLabel.setFont(
                new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(GRAY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel("Loading...");
        valueLabel.setFont(
                new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(NAVY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(valueLabel);

        switch (heading) {
            case "CUSTOMERS":
                customerValue = valueLabel;
                break;

            case "UNPAID BILLS":
                unpaidValue = valueLabel;
                break;

            case "REVENUE":
                revenueValue = valueLabel;
                break;

            case "UNITS USED":
                unitsValue = valueLabel;
                break;
        }

        return card;
    }

    // Refresh dashboard values
    private void refreshDashboard() {
        customerValue.setText(String.valueOf(getCustomerCount()));
        unpaidValue.setText(getUnpaidBillCount());
        revenueValue.setText(getTotalRevenue());
        unitsValue.setText(getTotalUnitsUsed());
    }

    private int getCustomerCount() {

        String sql = "SELECT COUNT(*) FROM customers";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            showDatabaseError("Customer count", e);
        }

        return 0;
    }

    private String getUnpaidBillCount() {

        String sql =
                "SELECT COUNT(*) FROM bills " +
                "WHERE payment_status = 'Unpaid'";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return String.valueOf(rs.getInt(1));
            }

        } catch (SQLException e) {
            showDatabaseError("Unpaid bill count", e);
        }

        return "0";
    }

    private String getTotalRevenue() {

        String sql =
                "SELECT COALESCE(SUM(amount_paid), 0) FROM payments";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return String.format(
                        java.util.Locale.US,
                        "\u20B9%.2f",
                        rs.getDouble(1));
            }

        } catch (SQLException e) {
            showDatabaseError("Revenue", e);
        }

        return "\u20B90.00";
    }

    private String getTotalUnitsUsed() {

        String sql =
                "SELECT COALESCE(SUM(units_consumed), 0) " +
                "FROM meter_readings";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return String.format(
                        java.util.Locale.US,
                        "%.0f",
                        rs.getDouble(1));
            }

        } catch (SQLException e) {
            showDatabaseError("Units used", e);
        }

        return "0";
    }

    private void showDatabaseError(
            String operation, SQLException e) {

        System.err.println(
                "Error loading " + operation + ": "
                + e.getMessage());
    }

    // Sidebar button design
    private JButton createMenuButton(String text) {

        JButton button = new JButton(text);

        button.setMaximumSize(new Dimension(200, 42));
        button.setPreferredSize(new Dimension(200, 42));
        button.setMinimumSize(new Dimension(200, 42));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setForeground(WHITE);
        button.setBackground(NAVY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(
                new java.awt.event.MouseAdapter() {

            @Override
            public void mouseEntered(
                    java.awt.event.MouseEvent e) {
                button.setBackground(GREEN);
            }

            @Override
            public void mouseExited(
                    java.awt.event.MouseEvent e) {
                button.setBackground(NAVY);
            }
        });

        return button;
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new Dashboard().setVisible(true);
        });
    }
}
