
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ConsumerDashboard extends JFrame {

    private final int customerId;

    private JTable billTable;
    private DefaultTableModel tableModel;
    private JLabel nameLabel;
    private JLabel consumerIdLabel;

    private static final Color NAVY = new Color(25, 55, 85);
    private static final Color GREEN = new Color(25, 155, 95);
    private static final Color LIGHT_BG = new Color(245, 248, 252);

    public ConsumerDashboard(int customerId) {
        this.customerId = customerId;

        setTitle("SEUBS - Consumer Dashboard");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JLabel title = new JLabel("CONSUMER DASHBOARD");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(NAVY);

        nameLabel = new JLabel("Welcome!");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        consumerIdLabel = new JLabel("Consumer ID: Loading...");

        JPanel headingPanel = new JPanel(
            new GridLayout(3, 1, 5, 5)
        );
        headingPanel.setBackground(LIGHT_BG);
        headingPanel.add(title);
        headingPanel.add(nameLabel);
        headingPanel.add(consumerIdLabel);

        mainPanel.add(headingPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new String[]{
                "Bill ID",
                "Billing Month",
                "Units",
                "Rate / Unit",
                "Total Amount",
                "Bill Date",
                "Due Date",
                "Payment Status"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        billTable = new JTable(tableModel);
        billTable.setRowHeight(25);
        billTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JScrollPane scrollPane = new JScrollPane(billTable);
        scrollPane.setBorder(
            BorderFactory.createTitledBorder("My Electricity Bills")
        );

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Bills");
        JButton viewButton = new JButton("View Bill Details");
        JButton payButton = new JButton("Pay Selected Bill");
        JButton reportButton = new JButton("My Reports");
        JButton logoutButton = new JButton("Logout");

        payButton.setBackground(GREEN);
        payButton.setForeground(Color.WHITE);
        payButton.setFocusPainted(false);

        reportButton.setBackground(NAVY);
        reportButton.setForeground(Color.WHITE);
        reportButton.setFocusPainted(false);

        refreshButton.addActionListener(e -> {
            loadConsumerDetails();
            loadBills();
        });

        viewButton.addActionListener(e -> viewBillDetails());
        payButton.addActionListener(e -> paySelectedBill());

        reportButton.addActionListener(e -> {
            try {
                new ConsumerReport(customerId).setVisible(true);
            } catch (Exception ex) {
                showError(
                    "Could not open your reports.",
                    ex
                );
            }
        });

        logoutButton.addActionListener(e -> confirmLogout());

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 10, 5)
        );
        buttonPanel.setBackground(LIGHT_BG);

        buttonPanel.add(refreshButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(payButton);
        buttonPanel.add(reportButton);
        buttonPanel.add(logoutButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        loadConsumerDetails();
        loadBills();

        setVisible(true);
    }

    private void loadConsumerDetails() {

        String sql =
            "SELECT full_name, consumer_id FROM customers " +
            "WHERE customer_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nameLabel.setText(
                        "Welcome, " + rs.getString("full_name")
                    );

                    consumerIdLabel.setText(
                        "Consumer ID: " +
                        rs.getString("consumer_id")
                    );
                } else {
                    nameLabel.setText("Customer details unavailable");
                    consumerIdLabel.setText("Consumer ID: Not found");
                }
            }

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void loadBills() {

        tableModel.setRowCount(0);

        String sql =
            "SELECT bill_id, billing_month, units_consumed, " +
            "rate_per_unit, total_amount, bill_date, due_date, " +
            "payment_status FROM bills WHERE customer_id = ? " +
            "ORDER BY bill_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("bill_id"),
                        rs.getString("billing_month"),
                        rs.getBigDecimal("units_consumed"),
                        rs.getBigDecimal("rate_per_unit"),
                        rs.getBigDecimal("total_amount"),
                        rs.getDate("bill_date"),
                        rs.getDate("due_date"),
                        rs.getString("payment_status")
                    });
                }
            }

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private int getSelectedBillRow() {

        int viewRow = billTable.getSelectedRow();

        if (viewRow == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a bill first.",
                "No Bill Selected",
                JOptionPane.WARNING_MESSAGE
            );
            return -1;
        }

        // Correctly handles row selection if table sorting is enabled.
        return billTable.convertRowIndexToModel(viewRow);
    }

    private void viewBillDetails() {

        int row = getSelectedBillRow();

        if (row == -1) {
            return;
        }

        String details =
            "Bill ID: " + tableModel.getValueAt(row, 0) + "\n" +
            "Billing Month: " + tableModel.getValueAt(row, 1) + "\n" +
            "Units Consumed: " + tableModel.getValueAt(row, 2) + "\n" +
            "Rate per Unit: ₹" + tableModel.getValueAt(row, 3) + "\n" +
            "Total Amount: ₹" + tableModel.getValueAt(row, 4) + "\n" +
            "Bill Date: " + tableModel.getValueAt(row, 5) + "\n" +
            "Due Date: " + tableModel.getValueAt(row, 6) + "\n" +
            "Payment Status: " + tableModel.getValueAt(row, 7);

        JOptionPane.showMessageDialog(
            this,
            details,
            "Bill Details",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void paySelectedBill() {

        int row = getSelectedBillRow();

        if (row == -1) {
            return;
        }

        int billId = Integer.parseInt(
            tableModel.getValueAt(row, 0).toString()
        );

        String status = String.valueOf(
            tableModel.getValueAt(row, 7)
        );

        if ("Paid".equalsIgnoreCase(status)
                || "SUCCESS".equalsIgnoreCase(status)) {

            JOptionPane.showMessageDialog(
                this,
                "This bill is already paid.",
                "Payment Status",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Continue to payment for Bill ID " + billId + "?",
            "Confirm Payment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        setVisible(false);

        try {
            new ConsumerPayment(this, billId, customerId);
        } catch (Exception ex) {
            setVisible(true);

            showError(
                "Could not open payment screen.",
                ex
            );
        }
    }

    private void confirmLogout() {

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to log out?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new LoginPage().setVisible(true);
        }
    }

    private void showDatabaseError(SQLException ex) {
        showError("Database error.", ex);
    }

    private void showError(String message, Exception ex) {

        JOptionPane.showMessageDialog(
            this,
            message + "\n" + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );

        ex.printStackTrace();
    }
}
