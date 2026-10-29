
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ConsumerPayment extends JFrame {

    private final JFrame dashboard;
    private final int billId;
    private final int customerId;

    private JLabel consumerLabel;
    private JLabel amountLabel;
    private JLabel dueDateLabel;
    private JLabel statusLabel;

    private JComboBox<String> methodCombo;
    private JTextField referenceField;

    private JTable historyTable;
    private DefaultTableModel historyModel;

    private double billAmount;
    private String billStatus;
    private String consumerId;

    private static final Color NAVY = new Color(25, 55, 85);
    private static final Color GREEN = new Color(25, 155, 95);
    private static final Color RED = new Color(220, 38, 38);
    private static final Color LIGHT_BG = new Color(245, 248, 252);

    public ConsumerPayment(int billId, int customerId) {
        this(null, billId, customerId);
    }

    public ConsumerPayment(JFrame dashboard, int billId, int customerId) {
        this.dashboard = dashboard;
        this.billId = billId;
        this.customerId = customerId;

        setTitle("SEUBS - Consumer Payment");
        setSize(720, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadBillDetails();
        loadPaymentHistory();

        setVisible(true);
    }

    private void buildUI() {

        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBackground(LIGHT_BG);
        main.setBorder(BorderFactory.createEmptyBorder(
            20, 25, 20, 25
        ));

        JPanel heading = new JPanel(new GridLayout(2, 1, 5, 5));
        heading.setBackground(LIGHT_BG);

        JLabel title = new JLabel(
            "Electricity Bill Payment",
            SwingConstants.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Review your bill and enter payment details",
            SwingConstants.CENTER
        );

        heading.add(title);
        heading.add(subtitle);

        main.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setBackground(LIGHT_BG);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel details = new JPanel(new GridLayout(4, 2, 10, 12));
        details.setBorder(BorderFactory.createTitledBorder(
            "Bill Details"
        ));

        consumerLabel = new JLabel("Loading...");
        amountLabel = new JLabel("Loading...");
        dueDateLabel = new JLabel("Loading...");
        statusLabel = new JLabel("Loading...");

        details.add(new JLabel("Consumer ID:"));
        details.add(consumerLabel);

        details.add(new JLabel("Bill Amount:"));
        details.add(amountLabel);

        details.add(new JLabel("Due Date:"));
        details.add(dueDateLabel);

        details.add(new JLabel("Bill Status:"));
        details.add(statusLabel);

        JPanel paymentForm = new JPanel(new GridLayout(2, 2, 10, 12));
        paymentForm.setBorder(BorderFactory.createTitledBorder(
            "Payment Information"
        ));

        JLabel methodLabel = requiredLabel("Payment Method");
        JLabel referenceLabel =
            requiredLabel("Transaction / Receipt Reference");

        methodCombo = new JComboBox<>(new String[]{
            "-- Select Payment Method --",
            "UPI",
            "Bank Transfer",
            "Card",
            "Cash"
        });
        methodCombo.setSelectedIndex(0);

        referenceField = new JTextField();
        referenceField.setToolTipText(
            "Enter your transaction ID or receipt number"
        );

        paymentForm.add(methodLabel);
        paymentForm.add(methodCombo);

        paymentForm.add(referenceLabel);
        paymentForm.add(referenceField);

        JLabel requiredNote = new JLabel("* Required fields");
        requiredNote.setForeground(RED);

        center.add(details);
        center.add(Box.createVerticalStrut(15));
        center.add(paymentForm);
        center.add(Box.createVerticalStrut(5));
        center.add(requiredNote);
        center.add(Box.createVerticalStrut(12));

        historyModel = new DefaultTableModel(
            new String[]{
                "Payment ID",
                "Method",
                "Reference",
                "Amount",
                "Status",
                "Date"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(23);

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setPreferredSize(new Dimension(650, 150));
        historyScroll.setBorder(BorderFactory.createTitledBorder(
            "Payment History"
        ));

        center.add(historyScroll);
        main.add(center, BorderLayout.CENTER);

        JButton payButton = new JButton("Submit Payment");
        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");

        payButton.setBackground(GREEN);
        payButton.setForeground(Color.WHITE);
        payButton.setFocusPainted(false);

        refreshButton.addActionListener(e -> {
            loadBillDetails();
            loadPaymentHistory();
        });

        payButton.addActionListener(e -> submitPayment());
        closeButton.addActionListener(e -> closeWindow());

        JPanel buttons = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 12, 5)
        );
        buttons.setBackground(LIGHT_BG);

        buttons.add(payButton);
        buttons.add(refreshButton);
        buttons.add(closeButton);

        main.add(buttons, BorderLayout.SOUTH);
        setContentPane(main);
    }

    // Displays a red asterisk for mandatory fields.
    private JLabel requiredLabel(String text) {
        return new JLabel(
            "<html>" + text +
            " <font color='#DC2626'>*</font></html>"
        );
    }

    private void loadBillDetails() {

        String sql =
            "SELECT c.consumer_id, b.total_amount, " +
            "b.due_date, b.payment_status " +
            "FROM bills b " +
            "JOIN customers c ON b.customer_id = c.customer_id " +
            "WHERE b.bill_id = ? AND b.customer_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, billId);
            ps.setInt(2, customerId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    consumerId = rs.getString("consumer_id");
                    billAmount = rs.getDouble("total_amount");
                    billStatus = rs.getString("payment_status");

                    consumerLabel.setText(
                        consumerId == null ? "-" : consumerId
                    );

                    amountLabel.setText(
                        String.format("₹ %.2f", billAmount)
                    );

                    Date dueDate = rs.getDate("due_date");

                    dueDateLabel.setText(
                        dueDate == null
                            ? "Not specified"
                            : dueDate.toString()
                    );

                    statusLabel.setText(
                        billStatus == null ? "Unknown" : billStatus
                    );

                } else {
                    consumerId = null;
                    billStatus = null;

                    consumerLabel.setText("Bill not found");
                    amountLabel.setText("-");
                    dueDateLabel.setText("-");
                    statusLabel.setText("-");
                }
            }

        } catch (SQLException ex) {
            showError("Could not load bill details.", ex);
        }
    }

    private boolean validatePaymentForm() {

        if (consumerId == null) {
            showValidationError(
                "Bill details could not be loaded."
            );
            return false;
        }

        if (!Double.isFinite(billAmount) || billAmount <= 0) {
            showValidationError(
                "The bill amount is invalid. Please contact the administrator."
            );
            return false;
        }

        if ("Paid".equalsIgnoreCase(billStatus)
                || "SUCCESS".equalsIgnoreCase(billStatus)) {
            showValidationError("This bill is already paid.");
            return false;
        }

        String method = (String) methodCombo.getSelectedItem();

        if (method == null
                || method.equals("-- Select Payment Method --")) {
            showValidationError("Please select a payment method.");
            methodCombo.requestFocusInWindow();
            return false;
        }

        String reference = referenceField.getText().trim();

        if (reference.isEmpty()) {
            showValidationError(
                "Transaction / Receipt Reference is required."
            );
            referenceField.requestFocusInWindow();
            return false;
        }

        if (reference.length() > 100) {
            showValidationError(
                "The transaction reference cannot exceed 100 characters."
            );
            referenceField.requestFocusInWindow();
            return false;
        }

        return true;
    }

    private void submitPayment() {

        if (!validatePaymentForm()) {
            return;
        }

        String method = (String) methodCombo.getSelectedItem();
        String reference = referenceField.getText().trim();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Submit payment details?\n\n" +
            "Consumer ID: " + consumerId + "\n" +
            "Amount: " + String.format("₹ %.2f", billAmount) + "\n" +
            "Method: " + method + "\n" +
            "Reference: " + reference + "\n\n" +
            "The payment will remain pending until verified.",
            "Confirm Payment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String insertSql =
            "INSERT INTO payments " +
            "(bill_id, amount_paid, payment_method, " +
            "transaction_reference, payment_status) " +
            "SELECT b.bill_id, b.total_amount, ?, ?, 'PENDING' " +
            "FROM bills b " +
            "WHERE b.bill_id = ? " +
            "AND b.customer_id = ? " +
            "AND LOWER(b.payment_status) NOT IN ('paid', 'success') " +
            "AND NOT EXISTS (" +
            "SELECT 1 FROM payments p " +
            "WHERE p.bill_id = b.bill_id " +
            "AND UPPER(p.payment_status) IN ('PENDING', 'SUCCESS')" +
            ")";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(insertSql)) {

            ps.setString(1, method);
            ps.setString(2, reference);
            ps.setInt(3, billId);
            ps.setInt(4, customerId);

            int rows = ps.executeUpdate();

            if (rows == 1) {
                JOptionPane.showMessageDialog(
                    this,
                    "Payment details submitted successfully.\n\n" +
                    "Consumer ID: " + consumerId + "\n" +
                    "Amount: " + String.format("₹ %.2f", billAmount) +
                    "\nStatus: PENDING\n\n" +
                    "Your payment must be verified before " +
                    "the bill is marked as paid.",
                    "Payment Submitted",
                    JOptionPane.INFORMATION_MESSAGE
                );

                referenceField.setText("");
                methodCombo.setSelectedIndex(0);

                loadBillDetails();
                loadPaymentHistory();

            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Payment could not be submitted. The bill may " +
                    "already be paid or have a pending payment.",
                    "Payment Not Submitted",
                    JOptionPane.WARNING_MESSAGE
                );

                loadBillDetails();
                loadPaymentHistory();
            }

        } catch (SQLIntegrityConstraintViolationException ex) {
            showValidationError(
                "This transaction reference may already exist. " +
                "Please verify it and try again."
            );

        } catch (SQLException ex) {
            showError("Could not submit payment.", ex);
        }
    }

    private void loadPaymentHistory() {

        historyModel.setRowCount(0);

        String sql =
            "SELECT p.payment_id, p.payment_method, " +
            "p.transaction_reference, p.amount_paid, " +
            "p.payment_status, p.payment_date " +
            "FROM payments p " +
            "JOIN bills b ON p.bill_id = b.bill_id " +
            "WHERE b.bill_id = ? AND b.customer_id = ? " +
            "ORDER BY p.payment_date DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, billId);
            ps.setInt(2, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    historyModel.addRow(new Object[]{
                        rs.getInt("payment_id"),
                        rs.getString("payment_method"),
                        rs.getString("transaction_reference"),
                        String.format(
                            "₹ %.2f",
                            rs.getDouble("amount_paid")
                        ),
                        rs.getString("payment_status"),
                        rs.getTimestamp("payment_date")
                    });
                }
            }

        } catch (SQLException ex) {
            showError("Could not load payment history.", ex);
        }
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Validation Error",
            JOptionPane.WARNING_MESSAGE
        );
    }

    private void closeWindow() {
        dispose();

        if (dashboard != null) {
            dashboard.setVisible(true);
        }
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            message + "\n" + ex.getMessage(),
            "Payment Error",
            JOptionPane.ERROR_MESSAGE
        );

        ex.printStackTrace();
    }
}
