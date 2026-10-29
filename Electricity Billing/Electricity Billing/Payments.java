
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.Locale;

public class Payments extends JFrame {

    private JFrame dashboard;
    private JComboBox<BillItem> billCombo;
    private JLabel amountLabel;
    private JComboBox<String> methodCombo;
    private JTextField referenceField;
    private JTable paymentTable;
    private DefaultTableModel tableModel;
    private boolean loadingBills = false;

    private static final double LATE_PENALTY = 50.00;

    public Payments(JFrame dashboard) {
        this.dashboard = dashboard;

        setTitle("Electricity Billing - Payments");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel title = new JLabel("PAYMENT MANAGEMENT");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(20, 43, 69));
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(
                BorderFactory.createTitledBorder("Record Payment")
        );

        billCombo = new JComboBox<>();

        amountLabel = new JLabel("₹0.00");
        amountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        amountLabel.setForeground(new Color(32, 164, 100));

        methodCombo = new JComboBox<>(
                new String[]{"UPI", "Cash", "Card"}
        );

        referenceField = new JTextField();

        formPanel.add(requiredLabel("Unpaid Bill / Consumer ID"));
        formPanel.add(billCombo);

        formPanel.add(new JLabel("Amount to Pay:"));
        formPanel.add(amountLabel);

        formPanel.add(requiredLabel("Payment Method"));
        formPanel.add(methodCombo);

        formPanel.add(requiredLabel("Transaction Reference"));
        formPanel.add(referenceField);

        JButton payButton = new JButton("Save Payment");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear");
        JButton backButton = new JButton("Back to Dashboard");

        payButton.setBackground(new Color(32, 164, 100));
        payButton.setForeground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(payButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(backButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        tableModel = new DefaultTableModel(
                new String[]{
                    "Payment ID",
                    "Consumer ID",
                    "Amount",
                    "Method",
                    "Reference"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentTable = new JTable(tableModel);
        paymentTable.setRowHeight(24);

        JScrollPane scrollPane = new JScrollPane(paymentTable);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder("Payment History")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);

        billCombo.addActionListener(e -> {
            if (!loadingBills) {
                updateAmount();
            }
        });

        payButton.addActionListener(e -> savePayment());
        refreshButton.addActionListener(e -> refreshData());
        clearButton.addActionListener(e -> clearForm());

        backButton.addActionListener(e -> {
            dispose();

            if (this.dashboard != null) {
                this.dashboard.setVisible(true);
            }
        });

        refreshData();
        setVisible(true);
    }

    private JLabel requiredLabel(String text) {
        return new JLabel(
                "<html>" + text
                + " <font color='#DC2626'>*</font></html>"
        );
    }

    private static class BillItem {

        private final int billId;
        private final String consumerId;
        private final double amount;
        private final LocalDate dueDate;

        BillItem(int billId, String consumerId,
                 double amount, LocalDate dueDate) {
            this.billId = billId;
            this.consumerId = consumerId;
            this.amount = amount;
            this.dueDate = dueDate;
        }

        double getPenalty() {
            if (dueDate != null
                    && dueDate.isBefore(LocalDate.now())) {
                return LATE_PENALTY;
            }

            return 0.00;
        }

        double getTotalPayable() {
            return amount + getPenalty();
        }

        @Override
        public String toString() {
            return consumerId + " - ₹"
                    + String.format(
                            Locale.US, "%.2f", getTotalPayable()
                    );
        }
    }

    private void refreshData() {
        loadUnpaidBills();
        loadPaymentHistory();
        updateAmount();
    }

    private void loadUnpaidBills() {
        loadingBills = true;
        billCombo.removeAllItems();

        String sql =
                "SELECT b.bill_id, c.consumer_id, b.total_amount, "
                + "b.due_date "
                + "FROM bills b "
                + "INNER JOIN customers c "
                + "ON b.customer_id = c.customer_id "
                + "WHERE b.payment_status = 'Unpaid' "
                + "ORDER BY b.bill_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Date sqlDate = rs.getDate("due_date");

                LocalDate dueDate = sqlDate != null
                        ? sqlDate.toLocalDate()
                        : null;

                billCombo.addItem(
                        new BillItem(
                                rs.getInt("bill_id"),
                                rs.getString("consumer_id"),
                                rs.getDouble("total_amount"),
                                dueDate
                        )
                );
            }

            if (billCombo.getItemCount() == 0) {
                amountLabel.setText("No unpaid bills");
            }

        } catch (SQLException ex) {
            showError("Could not load unpaid bills", ex);
        } finally {
            loadingBills = false;
        }
    }

    private void updateAmount() {
        BillItem selected = (BillItem) billCombo.getSelectedItem();

        if (selected == null) {
            amountLabel.setText("₹0.00");
            return;
        }

        amountLabel.setText(
                "₹" + String.format(
                        Locale.US, "%.2f",
                        selected.getTotalPayable()
                )
                + " (Penalty: ₹"
                + String.format(
                        Locale.US, "%.2f",
                        selected.getPenalty()
                ) + ")"
        );
    }

    private boolean validatePayment() {
        BillItem selected = (BillItem) billCombo.getSelectedItem();

        if (selected == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an unpaid bill.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            billCombo.requestFocusInWindow();
            return false;
        }

        if (!Double.isFinite(selected.amount) || selected.amount <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "The bill amount must be greater than zero.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        if (methodCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a payment method.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            methodCombo.requestFocusInWindow();
            return false;
        }

        String reference = referenceField.getText().trim();

        if (reference.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter the transaction reference.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            referenceField.requestFocusInWindow();
            return false;
        }

        if (reference.length() > 100) {
            JOptionPane.showMessageDialog(
                    this,
                    "Transaction reference cannot exceed 100 characters.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            referenceField.requestFocusInWindow();
            return false;
        }

        return true;
    }

    private void savePayment() {
        if (!validatePayment()) {
            return;
        }

        BillItem selected = (BillItem) billCombo.getSelectedItem();

        String method = (String) methodCombo.getSelectedItem();
        String reference = referenceField.getText().trim();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Record payment of ₹"
                        + String.format(
                                Locale.US, "%.2f",
                                selected.getTotalPayable()
                        )
                        + "\nConsumer ID: " + selected.consumerId
                        + "\nBill amount: ₹"
                        + String.format(
                                Locale.US, "%.2f", selected.amount
                        )
                        + "\nLate penalty: ₹"
                        + String.format(
                                Locale.US, "%.2f",
                                selected.getPenalty()
                        )
                        + "\nPayment method: " + method + "?",
                "Confirm Payment",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String checkSql =
                "SELECT total_amount, due_date FROM bills "
                + "WHERE bill_id = ? AND payment_status = 'Unpaid' "
                + "FOR UPDATE";

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
                double currentAmount;

                try (PreparedStatement check =
                             con.prepareStatement(checkSql)) {
                    check.setInt(1, selected.billId);

                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "This bill is no longer unpaid. "
                                    + "Refresh and try again."
                            );
                        }

                        double billAmount = rs.getDouble("total_amount");

                        if (rs.wasNull()
                                || !Double.isFinite(billAmount)
                                || billAmount <= 0) {
                            throw new SQLException(
                                    "The bill amount is invalid."
                            );
                        }

                        Date sqlDate = rs.getDate("due_date");

                        double penalty = 0.00;

                        if (sqlDate != null
                                && sqlDate.toLocalDate()
                                        .isBefore(LocalDate.now())) {
                            penalty = LATE_PENALTY;
                        }

                        currentAmount = billAmount + penalty;
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(insertSql)) {
                    ps.setInt(1, selected.billId);
                    ps.setDouble(2, currentAmount);
                    ps.setString(3, method);
                    ps.setString(4, reference);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             con.prepareStatement(updateSql)) {
                    ps.setInt(1, selected.billId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Could not update bill status."
                        );
                    }
                }

                con.commit();

                JOptionPane.showMessageDialog(
                        this,
                        "Payment recorded successfully!\n"
                        + "Consumer ID: " + selected.consumerId
                        + "\nAmount paid: ₹"
                        + String.format(
                                Locale.US, "%.2f", currentAmount
                        )
                        + "\nStatus: Paid",
                        "Payment Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );

                clearForm();
                refreshData();

            } catch (SQLException ex) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }

                throw ex;

            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Connection closes after this method.
                }
            }

        } catch (SQLException ex) {
            showError("Could not record payment", ex);
        }
    }

    private void loadPaymentHistory() {
        tableModel.setRowCount(0);

        String sql =
                "SELECT p.payment_id, c.consumer_id, p.amount_paid, "
                + "p.payment_method, p.transaction_reference "
                + "FROM payments p "
                + "INNER JOIN bills b ON p.bill_id = b.bill_id "
                + "INNER JOIN customers c "
                + "ON b.customer_id = c.customer_id "
                + "ORDER BY p.payment_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("payment_id"),
                    rs.getString("consumer_id"),
                    rs.getDouble("amount_paid"),
                    rs.getString("payment_method"),
                    rs.getString("transaction_reference")
                });
            }

        } catch (SQLException ex) {
            showError("Could not load payment history", ex);
        }
    }

    private void clearForm() {
        referenceField.setText("");
        methodCombo.setSelectedIndex(0);
        billCombo.setSelectedIndex(-1);
        updateAmount();
    }

    private void showError(String message, SQLException ex) {
        JOptionPane.showMessageDialog(
                this,
                message + ":\n" + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );

        ex.printStackTrace();
    }
}