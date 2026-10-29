import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Payment extends JFrame {

    private final JFrame Dashboard;
    private JComboBox<BillOption> billBox;
    private JComboBox<String> methodBox;
    private JTextField referenceField;
    private JLabel amountLabel;
    private DefaultTableModel model;
    private boolean loadingBills = false;

    private static class BillOption {
        private final int billId;
        private final String consumerId;
        private final double amount;

        public BillOption(int billId, String consumerId, double amount) {
            this.billId = billId;
            this.consumerId = consumerId;
            this.amount = amount;
        }

        public int getBillId() {
            return billId;
        }

        public String getConsumerId() {
            return consumerId;
        }

        public double getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return consumerId + " - ₹"
                    + String.format(java.util.Locale.US, "%.2f", amount);
        }
    }

    public Payment(JFrame Dashboard) {
        this.Dashboard = Dashboard;

        setTitle("Electricity Billing - Payments");
        setSize(1100, 620);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(244, 247, 250));

        JLabel title = new JLabel("PAYMENT MANAGEMENT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(20, 43, 69));
        title.setBounds(20, 15, 500, 40);
        panel.add(title);

        JLabel billLabel = requiredLabel("Unpaid Bill / Consumer ID");
        billLabel.setBounds(20, 85, 220, 30);
        panel.add(billLabel);

        billBox = new JComboBox<>();
        billBox.setBounds(250, 85, 800, 30);
        panel.add(billBox);

        JLabel amountTitle = new JLabel("Amount to Pay:");
        amountTitle.setBounds(20, 130, 220, 30);
        panel.add(amountTitle);

        amountLabel = new JLabel("₹0.00");
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        amountLabel.setForeground(new Color(32, 164, 100));
        amountLabel.setBounds(250, 130, 400, 30);
        panel.add(amountLabel);

        JLabel methodLabel = requiredLabel("Payment Method");
        methodLabel.setBounds(20, 175, 220, 30);
        panel.add(methodLabel);

        methodBox = new JComboBox<>(
                new String[]{"UPI", "Cash", "Card"});
        methodBox.setBounds(250, 175, 300, 30);
        panel.add(methodBox);

        JLabel referenceLabel = requiredLabel("Transaction Reference");
        referenceLabel.setBounds(20, 220, 220, 30);
        panel.add(referenceLabel);

        referenceField = new JTextField();
        referenceField.setBounds(250, 220, 500, 30);
        panel.add(referenceField);

        JButton payButton = new JButton("Save Payment");
        payButton.setBounds(270, 270, 145, 35);
        payButton.setBackground(new Color(32, 164, 100));
        payButton.setForeground(Color.WHITE);
        panel.add(payButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBounds(425, 270, 100, 35);
        panel.add(refreshButton);

        JButton clearButton = new JButton("Clear");
        clearButton.setBounds(535, 270, 90, 35);
        panel.add(clearButton);

        JButton backButton = new JButton("Back to Dashboard");
        backButton.setBounds(635, 270, 180, 35);
        panel.add(backButton);

        JLabel historyTitle = new JLabel("Payment History");
        historyTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        historyTitle.setBounds(20, 330, 300, 25);
        panel.add(historyTitle);

        model = new DefaultTableModel(
                new String[]{
                    "Payment ID", "Consumer ID", "Amount",
                    "Method", "Reference", "Payment Date"
                }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(20, 360, 1030, 190);
        panel.add(scrollPane);

        billBox.addActionListener(e -> {
            if (!loadingBills) {
                updateAmount();
            }
        });

        payButton.addActionListener(e -> recordPayment());

        refreshButton.addActionListener(e -> {
            loadUnpaidBills();
            loadPayments();
        });

        clearButton.addActionListener(e -> {
            billBox.setSelectedIndex(-1);
            methodBox.setSelectedIndex(0);
            referenceField.setText("");
            amountLabel.setText("₹0.00");
        });

        backButton.addActionListener(e -> {
            dispose();
            if (Dashboard != null) {
                Dashboard.setVisible(true);
            }
        });

        add(panel);

        loadUnpaidBills();
        loadPayments();

        setVisible(true);
    }

    // Adds a red asterisk to required fields.
    private JLabel requiredLabel(String text) {
        return new JLabel(
                "<html>" + text
                + " <font color='#DC2626'>*</font></html>");
    }

    private void loadUnpaidBills() {
        loadingBills = true;
        billBox.removeAllItems();

        String sql =
                "SELECT b.bill_id, c.consumer_id, b.total_amount "
                + "FROM bills b "
                + "INNER JOIN customers c "
                + "ON b.customer_id = c.customer_id "
                + "WHERE b.payment_status = 'Unpaid' "
                + "ORDER BY b.bill_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                billBox.addItem(new BillOption(
                        rs.getInt("bill_id"),
                        rs.getString("consumer_id"),
                        rs.getDouble("total_amount")
                ));
            }

        } catch (SQLException e) {
            showError("Could not load unpaid bills.", e);
        } finally {
            loadingBills = false;
        }

        updateAmount();
    }

    private BillOption getSelectedBill() {
        return (BillOption) billBox.getSelectedItem();
    }

    private void updateAmount() {
        BillOption selected = getSelectedBill();

        if (selected == null) {
            amountLabel.setText("₹0.00");
            return;
        }

        String sql =
                "SELECT total_amount FROM bills "
                + "WHERE bill_id = ? AND payment_status = 'Unpaid'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, selected.getBillId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double amount = rs.getDouble("total_amount");

                    amountLabel.setText(String.format(
                            java.util.Locale.US, "₹%.2f", amount));
                } else {
                    amountLabel.setText("₹0.00");
                }
            }

        } catch (SQLException e) {
            showError("Could not retrieve bill amount.", e);
        }
    }

    private boolean validatePayment() {
        if (getSelectedBill() == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select an unpaid bill.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            billBox.requestFocusInWindow();
            return false;
        }

        if (methodBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a payment method.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            methodBox.requestFocusInWindow();
            return false;
        }

        String reference = referenceField.getText().trim();

        if (reference.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the transaction reference.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            referenceField.requestFocusInWindow();
            return false;
        }

        if (reference.length() > 100) {
            JOptionPane.showMessageDialog(this,
                    "Transaction reference cannot exceed 100 characters.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            referenceField.requestFocusInWindow();
            return false;
        }

        return true;
    }

    private void recordPayment() {
        if (!validatePayment()) {
            return;
        }

        BillOption selected = getSelectedBill();
        int billId = selected.getBillId();
        String consumerId = selected.getConsumerId();
        String method = (String) methodBox.getSelectedItem();
        String reference = referenceField.getText().trim();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Record payment for Consumer ID: " + consumerId + "?\n"
                + "Amount: " + amountLabel.getText()
                + "\nMethod: " + method,
                "Confirm Payment",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String checkSql =
                "SELECT total_amount FROM bills "
                + "WHERE bill_id = ? AND payment_status = 'Unpaid'";

        String insertSql =
                "INSERT INTO payments "
                + "(bill_id, amount_paid, payment_method, "
                + "transaction_reference) VALUES (?, ?, ?, ?)";

        String updateSql =
                "UPDATE bills SET payment_status = 'Paid' "
                + "WHERE bill_id = ? AND payment_status = 'Unpaid'";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                double amount;

                try (PreparedStatement ps =
                             con.prepareStatement(checkSql)) {
                    ps.setInt(1, billId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Bill not found or already paid. "
                                    + "Please refresh the bill list.");
                        }

                        amount = rs.getDouble("total_amount");

                        if (rs.wasNull()
                                || !Double.isFinite(amount)
                                || amount <= 0) {
                            throw new SQLException(
                                    "The bill amount is invalid.");
                        }
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(insertSql)) {
                    ps.setInt(1, billId);
                    ps.setDouble(2, amount);
                    ps.setString(3, method);
                    ps.setString(4, reference);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             con.prepareStatement(updateSql)) {
                    ps.setInt(1, billId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Could not update bill payment status.");
                    }
                }

                con.commit();

                JOptionPane.showMessageDialog(
                        this,
                        String.format(
                                java.util.Locale.US,
                                "Payment recorded successfully!\n"
                                + "Consumer ID: %s\nAmount paid: ₹%.2f",
                                consumerId, amount));

                referenceField.setText("");
                loadUnpaidBills();
                loadPayments();

            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            showError("Could not record payment.", e);
        }
    }

    private void loadPayments() {
        model.setRowCount(0);

        String sql =
                "SELECT p.payment_id, c.consumer_id, p.amount_paid, "
                + "p.payment_method, p.transaction_reference, "
                + "p.payment_date "
                + "FROM payments p "
                + "INNER JOIN bills b ON p.bill_id = b.bill_id "
                + "INNER JOIN customers c "
                + "ON b.customer_id = c.customer_id "
                + "ORDER BY p.payment_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("payment_id"),
                    rs.getString("consumer_id"),
                    rs.getDouble("amount_paid"),
                    rs.getString("payment_method"),
                    rs.getString("transaction_reference"),
                    rs.getDate("payment_date")
                });
            }

        } catch (SQLException e) {
            showError("Could not load payment history.", e);
        }
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n" + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
