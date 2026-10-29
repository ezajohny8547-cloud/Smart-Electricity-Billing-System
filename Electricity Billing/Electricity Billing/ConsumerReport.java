
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ConsumerReport extends JFrame {

    private final int customerId;

    private JLabel nameLabel;
    private JLabel totalBillsLabel;
    private JLabel paidBillsLabel;
    private JLabel unpaidBillsLabel;
    private JLabel totalAmountLabel;

    private DefaultTableModel billModel;
    private DefaultTableModel paymentModel;

    public ConsumerReport(int customerId) {
        this.customerId = customerId;

        setTitle("SEUBS - My Reports");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );
        mainPanel.setBackground(new Color(245, 248, 252));

        JLabel title = new JLabel("MY ELECTRICITY REPORT");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(25, 55, 85));

        nameLabel = new JLabel("Customer: Loading...");
        totalBillsLabel = new JLabel("Total Bills: 0");
        paidBillsLabel = new JLabel("Paid Bills: 0");
        unpaidBillsLabel = new JLabel("Unpaid Bills: 0");
        totalAmountLabel = new JLabel("Total Billed: ₹0.00");

        JPanel heading = new JPanel(new GridLayout(2, 1, 5, 5));
        heading.setBackground(new Color(245, 248, 252));
        heading.add(title);
        heading.add(nameLabel);

        JPanel summary = new JPanel(new GridLayout(1, 4, 10, 10));
        summary.setBackground(new Color(245, 248, 252));
        summary.add(totalBillsLabel);
        summary.add(paidBillsLabel);
        summary.add(unpaidBillsLabel);
        summary.add(totalAmountLabel);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(245, 248, 252));
        topPanel.add(heading, BorderLayout.NORTH);
        topPanel.add(summary, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        billModel = new DefaultTableModel(
            new String[] {
                "Month", "Units", "Total Amount",
                "Due Date", "Status"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentModel = new DefaultTableModel(
            new String[] {
                "Bill ID", "Amount Paid",
                "Payment Method", "Reference"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Bill History", new JScrollPane(new JTable(billModel)));
        tabs.addTab("Payment History",
                    new JScrollPane(new JTable(paymentModel)));

        mainPanel.add(tabs, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Report");
        refreshButton.addActionListener(e -> loadReport());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(245, 248, 252));
        bottomPanel.add(refreshButton);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        loadReport();
    }

    private void loadReport() {
        billModel.setRowCount(0);
        paymentModel.setRowCount(0);

        int totalBills = 0;
        int paidBills = 0;
        int unpaidBills = 0;
        double totalAmount = 0.0;

        String customerSql =
            "SELECT full_name FROM customers WHERE customer_id = ?";

        String billSql =
            "SELECT bill_id, billing_month, units_consumed, " +
            "total_amount, due_date, payment_status " +
            "FROM bills WHERE customer_id = ? ORDER BY bill_id DESC";

        String paymentSql =
            "SELECT b.bill_id, p.amount_paid, " +
            "p.payment_method, p.transaction_reference " +
            "FROM payments p JOIN bills b ON p.bill_id = b.bill_id " +
            "WHERE b.customer_id = ? ORDER BY b.bill_id DESC";

        try (Connection con = DBConnection.getConnection()) {

            try (PreparedStatement ps =
                     con.prepareStatement(customerSql)) {

                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nameLabel.setText(
                            "Customer: " + rs.getString("full_name")
                        );
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(billSql)) {

                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {

                        String status = rs.getString("payment_status");
                        double amount = rs.getDouble("total_amount");

                        totalBills++;
                        totalAmount += amount;

                        if ("Paid".equalsIgnoreCase(status)) {
                            paidBills++;
                        } else {
                            unpaidBills++;
                        }

                        billModel.addRow(new Object[] {
                            rs.getString("billing_month"),
                            rs.getBigDecimal("units_consumed"),
                            rs.getBigDecimal("total_amount"),
                            rs.getDate("due_date"),
                            status
                        });
                    }
                }
            }

            try (PreparedStatement ps =
                     con.prepareStatement(paymentSql)) {

                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        paymentModel.addRow(new Object[] {
                            rs.getInt("bill_id"),
                            rs.getBigDecimal("amount_paid"),
                            rs.getString("payment_method"),
                            rs.getString("transaction_reference")
                        });
                    }
                }
            }

            totalBillsLabel.setText("Total Bills: " + totalBills);
            paidBillsLabel.setText("Paid Bills: " + paidBills);
            unpaidBillsLabel.setText("Unpaid Bills: " + unpaidBills);
            totalAmountLabel.setText(
                String.format("Total Billed: ₹%.2f", totalAmount)
            );

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not load report:\n" + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
            ex.printStackTrace();
        }
    }
}
