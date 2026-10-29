import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.Locale;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Reports extends JFrame {

    private JFrame dashboard;
    private JTable reportTable;
    private DefaultTableModel tableModel;

    private JLabel totalBillsLabel;
    private JLabel paidBillsLabel;
    private JLabel unpaidBillsLabel;
    private JLabel revenueLabel;

    private JComboBox<String> reportTypeCombo;
    private JComboBox<String> monthCombo;
    private JComboBox<Integer> yearCombo;

    private final Color NAVY = new Color(20, 43, 69);
    private final Color GREEN = new Color(32, 164, 100);
    private final Color LIGHT_BG = new Color(244, 247, 250);

    public Reports(JFrame dashboard) {
        this.dashboard = dashboard;

        setTitle("SEUBS - Reports");
        setSize(1250, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Return to Admin Dashboard when the Reports window closes.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (Reports.this.dashboard != null) {
                    Reports.this.dashboard.setVisible(true);
                }
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel title = new JLabel("REPORTS & ANALYTICS");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(NAVY);

        mainPanel.add(title, BorderLayout.NORTH);

        // Summary cards
        JPanel summaryPanel = new JPanel(
            new GridLayout(1, 4, 12, 12)
        );
        summaryPanel.setBackground(LIGHT_BG);

        totalBillsLabel = createSummaryCard(
            summaryPanel, "Total Bills", "0"
        );

        paidBillsLabel = createSummaryCard(
            summaryPanel, "Paid Bills", "0"
        );

        unpaidBillsLabel = createSummaryCard(
            summaryPanel, "Unpaid Bills", "0"
        );

        revenueLabel = createSummaryCard(
            summaryPanel, "Revenue Received", "₹0.00"
        );

        // Report filters
        JPanel filterPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 10)
        );
        filterPanel.setBackground(LIGHT_BG);
        filterPanel.setBorder(
            BorderFactory.createTitledBorder("Choose Report")
        );

        reportTypeCombo = new JComboBox<>(new String[]{
            "All Bills",
            "Paid Bills",
            "Unpaid Bills",
            "Payment History",
            "Customer Billing Details",
            "Month-wise Bills"
        });

        String[] months = new String[12];

        for (int i = 1; i <= 12; i++) {
            months[i - 1] = Month.of(i).getDisplayName(
                TextStyle.FULL, Locale.ENGLISH
            );
        }

        monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(
            LocalDate.now().getMonthValue() - 1
        );

        yearCombo = new JComboBox<>();
        int currentYear = Year.now().getValue();

        for (int year = currentYear - 5;
             year <= currentYear + 5; year++) {
            yearCombo.addItem(year);
        }

        yearCombo.setSelectedItem(currentYear);

        JButton viewButton = new JButton("View Report");
        JButton refreshButton = new JButton("Refresh");
        JButton backButton = new JButton("Back to Dashboard");

        viewButton.setBackground(GREEN);
        viewButton.setForeground(Color.WHITE);
        viewButton.setFocusPainted(false);

        filterPanel.add(new JLabel("Report Type:"));
        filterPanel.add(reportTypeCombo);
        filterPanel.add(new JLabel("Month:"));
        filterPanel.add(monthCombo);
        filterPanel.add(new JLabel("Year:"));
        filterPanel.add(yearCombo);
        filterPanel.add(viewButton);
        filterPanel.add(refreshButton);
        filterPanel.add(backButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(LIGHT_BG);
        topPanel.add(summaryPanel, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        // Table model
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reportTable = new JTable(tableModel);
        reportTable.setRowHeight(25);
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        reportTable.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(reportTable);
        scrollPane.setBorder(
            BorderFactory.createTitledBorder("Report Results")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(LIGHT_BG);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Button events
        viewButton.addActionListener(e -> refreshReports());
        refreshButton.addActionListener(e -> refreshReports());

        reportTypeCombo.addActionListener(e -> {
            boolean monthWise = isMonthWise();
            monthCombo.setEnabled(monthWise);
            yearCombo.setEnabled(monthWise);
        });

        backButton.addActionListener(e -> dispose());

        monthCombo.setEnabled(true);
        yearCombo.setEnabled(true);

        refreshReports();
        setVisible(true);
    }

    // Creates one summary card and returns its value label.
    private JLabel createSummaryCard(
            JPanel parent, String heading, String value) {

        JPanel card = new JPanel(new BorderLayout(5, 8));
        card.setBackground(Color.WHITE);

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(210, 220, 230)
                ),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
            )
        );

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setForeground(NAVY);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        valueLabel.setForeground(GREEN);

        card.add(headingLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        parent.add(card);

        return valueLabel;
    }

    private boolean isMonthWise() {
        return "Month-wise Bills".equals(
            reportTypeCombo.getSelectedItem()
        );
    }

    // Expected billing_month format: YYYY-MM, e.g. 2026-10.
    private String getSelectedBillingMonth() {
        int month = monthCombo.getSelectedIndex() + 1;
        int year = (Integer) yearCombo.getSelectedItem();

        return String.format("%04d-%02d", year, month);
    }

    private void refreshReports() {
        loadSelectedReport();
        loadSummary();
    }

    // Loads the four summary card values from the database.
    private void loadSummary() {
        String month = getSelectedBillingMonth();

        String billsSql =
            "SELECT COUNT(*) AS total_bills, " +
            "COALESCE(SUM(CASE WHEN payment_status = 'Paid' " +
            "THEN 1 ELSE 0 END), 0) AS paid_bills, " +
            "COALESCE(SUM(CASE WHEN payment_status = 'Unpaid' " +
            "THEN 1 ELSE 0 END), 0) AS unpaid_bills " +
            "FROM bills";

        if (isMonthWise()) {
            billsSql += " WHERE billing_month = ?";
        }

        String revenueSql =
            "SELECT COALESCE(SUM(p.amount_paid), 0) AS revenue " +
            "FROM payments p " +
            "JOIN bills b ON p.bill_id = b.bill_id";

        if (isMonthWise()) {
            revenueSql += " WHERE b.billing_month = ?";
        }

        try (Connection con = DBConnection.getConnection()) {

            try (PreparedStatement ps =
                     con.prepareStatement(billsSql)) {

                if (isMonthWise()) {
                    ps.setString(1, month);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalBillsLabel.setText(
                            String.valueOf(rs.getInt("total_bills"))
                        );

                        paidBillsLabel.setText(
                            String.valueOf(rs.getInt("paid_bills"))
                        );

                        unpaidBillsLabel.setText(
                            String.valueOf(rs.getInt("unpaid_bills"))
                        );
                    }
                }
            }

            try (PreparedStatement ps =
                     con.prepareStatement(revenueSql)) {

                if (isMonthWise()) {
                    ps.setString(1, month);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        revenueLabel.setText(
                            "₹" + String.format(
                                Locale.US,
                                "%.2f",
                                rs.getDouble("revenue")
                            )
                        );
                    }
                }
            }

        } catch (SQLException ex) {
            showError("Could not load report summary", ex);
        }
    }

    private void loadSelectedReport() {
        String type = (String) reportTypeCombo.getSelectedItem();

        if (type == null) {
            return;
        }

        switch (type) {

            case "All Bills":
                loadQuery(new String[]{
                    "Bill ID", "Customer ID", "Customer Name",
                    "Reading ID", "Month", "Units", "Rate",
                    "Total", "Bill Date", "Due Date", "Status"
                },
                "SELECT b.bill_id, b.customer_id, c.full_name, " +
                "b.reading_id, b.billing_month, b.units_consumed, " +
                "b.rate_per_unit, b.total_amount, b.bill_date, " +
                "b.due_date, b.payment_status " +
                "FROM bills b JOIN customers c " +
                "ON b.customer_id = c.customer_id " +
                "ORDER BY b.bill_id DESC");
                break;

            case "Paid Bills":
                loadQuery(new String[]{
                    "Bill ID", "Customer ID", "Customer Name",
                    "Month", "Total", "Status"
                },
                "SELECT b.bill_id, b.customer_id, c.full_name, " +
                "b.billing_month, b.total_amount, b.payment_status " +
                "FROM bills b JOIN customers c " +
                "ON b.customer_id = c.customer_id " +
                "WHERE b.payment_status = 'Paid' " +
                "ORDER BY b.bill_id DESC");
                break;

            case "Unpaid Bills":
                loadQuery(new String[]{
                    "Bill ID", "Customer ID", "Customer Name",
                    "Month", "Total", "Due Date", "Status"
                },
                "SELECT b.bill_id, b.customer_id, c.full_name, " +
                "b.billing_month, b.total_amount, b.due_date, " +
                "b.payment_status " +
                "FROM bills b JOIN customers c " +
                "ON b.customer_id = c.customer_id " +
                "WHERE b.payment_status = 'Unpaid' " +
                "ORDER BY b.due_date");
                break;

            case "Payment History":
                loadQuery(new String[]{
                    "Payment ID", "Bill ID", "Customer Name",
                    "Month", "Amount Paid", "Method", "Reference"
                },
                "SELECT p.payment_id, p.bill_id, c.full_name, " +
                "b.billing_month, p.amount_paid, " +
                "p.payment_method, p.transaction_reference " +
                "FROM payments p " +
                "JOIN bills b ON p.bill_id = b.bill_id " +
                "JOIN customers c ON b.customer_id = c.customer_id " +
                "ORDER BY p.payment_id DESC");
                break;

            case "Customer Billing Details":
                loadQuery(new String[]{
                    "Customer ID", "Customer Name", "Phone",
                    "Email", "Total Bills", "Total Billed"
                },
                "SELECT c.customer_id, c.full_name, c.phone, " +
                "c.email, COUNT(b.bill_id) AS total_bills, " +
                "COALESCE(SUM(b.total_amount), 0) AS total_billed " +
                "FROM customers c LEFT JOIN bills b " +
                "ON c.customer_id = b.customer_id " +
                "GROUP BY c.customer_id, c.full_name, c.phone, c.email " +
                "ORDER BY c.customer_id");
                break;

            case "Month-wise Bills":
                loadMonthWiseReport();
                break;

            default:
                break;
        }
    }

    private void loadMonthWiseReport() {
        String month = getSelectedBillingMonth();

        String sql =
            "SELECT b.bill_id, b.customer_id, c.full_name, " +
            "b.reading_id, b.billing_month, b.units_consumed, " +
            "b.rate_per_unit, b.total_amount, b.bill_date, " +
            "b.due_date, b.payment_status " +
            "FROM bills b JOIN customers c " +
            "ON b.customer_id = c.customer_id " +
            "WHERE b.billing_month = ? " +
            "ORDER BY b.bill_id DESC";

        String[] columns = {
            "Bill ID", "Customer ID", "Customer Name",
            "Reading ID", "Month", "Units", "Rate",
            "Total", "Bill Date", "Due Date", "Status"
        };

        tableModel.setRowCount(0);
        tableModel.setColumnIdentifiers(columns);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, month);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getObject("bill_id"),
                        rs.getObject("customer_id"),
                        rs.getString("full_name"),
                        rs.getObject("reading_id"),
                        rs.getString("billing_month"),
                        rs.getObject("units_consumed"),
                        rs.getObject("rate_per_unit"),
                        rs.getObject("total_amount"),
                        rs.getObject("bill_date"),
                        rs.getObject("due_date"),
                        rs.getString("payment_status")
                    });
                }
            }

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "No bills found for " + month + ".",
                    "Monthly Report",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (SQLException ex) {
            showError("Could not load monthly report", ex);
        }
    }

    private void loadQuery(String[] columns, String sql) {
        tableModel.setRowCount(0);
        tableModel.setColumnIdentifiers(columns);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Object[] row = new Object[columns.length];

                for (int i = 0; i < columns.length; i++) {
                    row[i] = rs.getObject(i + 1);
                }

                tableModel.addRow(row);
            }

        } catch (SQLException ex) {
            showError("Could not load report", ex);
        }
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
