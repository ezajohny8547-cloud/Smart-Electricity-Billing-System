
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;

public class Billcalculation extends JFrame {

    private JFrame dashboard;
    private JComboBox<CustomerItem> customerCombo;
    private JComboBox<ReadingItem> readingCombo;
    private JTextField monthField;
    private JTextField rateField;
    private JTextField fixedField;
    private JTextField dueDateField;
    private JLabel unitsLabel;
    private JLabel totalLabel;
    private JTable billTable;
    private DefaultTableModel tableModel;

    private static final Color NAVY = new Color(20, 43, 69);
    private static final Color GREEN = new Color(32, 164, 100);
    private static final Color RED = new Color(220, 38, 38);

    public Billcalculation(JFrame dashboard) {
        this.dashboard = dashboard;

        setTitle("Electricity Billing - Bill Calculation");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel title = new JLabel("BILL CALCULATION");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(NAVY);
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 10, 10));
        formPanel.setBorder(
            BorderFactory.createTitledBorder("Create Electricity Bill")
        );

        customerCombo = new JComboBox<>();
        readingCombo = new JComboBox<>();

        monthField = new JTextField(YearMonth.now().toString());
        rateField = new JTextField("6.00");
        fixedField = new JTextField("50.00");
        dueDateField = new JTextField(
            LocalDate.now().plusDays(15).toString()
        );

        unitsLabel = new JLabel("0 units");

        totalLabel = new JLabel("₹0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 17));
        totalLabel.setForeground(GREEN);

        // Mandatory fields are marked with a red asterisk.
        formPanel.add(requiredLabel("Consumer ID"));
        formPanel.add(customerCombo);

        formPanel.add(requiredLabel("Meter Reading"));
        formPanel.add(readingCombo);

        formPanel.add(requiredLabel("Billing Month (YYYY-MM)"));
        formPanel.add(monthField);

        formPanel.add(new JLabel("Units Consumed:"));
        formPanel.add(unitsLabel);

        formPanel.add(requiredLabel("Rate Per Unit (₹)"));
        formPanel.add(rateField);

        formPanel.add(requiredLabel("Fixed Charge (₹)"));
        formPanel.add(fixedField);

        formPanel.add(requiredLabel("Due Date (YYYY-MM-DD)"));
        formPanel.add(dueDateField);

        formPanel.add(new JLabel("Total Bill:"));
        formPanel.add(totalLabel);

        JButton calculateButton = new JButton("Calculate");
        JButton saveButton = new JButton("Save Bill");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear");
        JButton backButton = new JButton("Back to Dashboard");

        saveButton.setBackground(GREEN);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(calculateButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(backButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        tableModel = new DefaultTableModel(
            new String[]{
                "Bill ID", "Consumer ID", "Reading ID",
                "Month", "Units", "Rate", "Total",
                "Due Date", "Status"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        billTable = new JTable(tableModel);
        billTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollPane = new JScrollPane(billTable);
        scrollPane.setBorder(
            BorderFactory.createTitledBorder("Saved Bills")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);

        customerCombo.addActionListener(e -> loadReadings());
        readingCombo.addActionListener(e -> updateUnits());

        calculateButton.addActionListener(e -> calculateBill());
        saveButton.addActionListener(e -> saveBill());

        refreshButton.addActionListener(e -> {
            loadCustomers();
            loadBills();
        });

        clearButton.addActionListener(e -> clearForm());

        backButton.addActionListener(e -> {
            dispose();

            if (dashboard != null) {
                dashboard.setVisible(true);
            }
        });

        loadCustomers();
        loadBills();

        setVisible(true);
    }

    // Creates a label with a red mandatory-field asterisk.
    private JLabel requiredLabel(String text) {
        JLabel label = new JLabel(
            "<html>" + text +
            " <font color='#DC2626'>*</font></html>"
        );
        return label;
    }

    private static class CustomerItem {
        int id;
        String consumerId;
        String name;

        CustomerItem(int id, String consumerId, String name) {
            this.id = id;
            this.consumerId = consumerId;
            this.name = name;
        }

        @Override
        public String toString() {
            return consumerId + " - " + name;
        }
    }

    private static class ReadingItem {
        int id;
        int customerId;
        double units;

        ReadingItem(int id, int customerId, double units) {
            this.id = id;
            this.customerId = customerId;
            this.units = units;
        }

        @Override
        public String toString() {
            return "Reading #" + id + " - " + units + " units";
        }
    }

    private void loadCustomers() {
        customerCombo.removeAllItems();

        String sql =
            "SELECT customer_id, consumer_id, full_name " +
            "FROM customers ORDER BY customer_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                customerCombo.addItem(
                    new CustomerItem(
                        rs.getInt("customer_id"),
                        rs.getString("consumer_id"),
                        rs.getString("full_name")
                    )
                );
            }

        } catch (SQLException ex) {
            showError("Could not load customers", ex);
        }

        loadReadings();
    }

    private void loadReadings() {
        readingCombo.removeAllItems();

        CustomerItem customer =
            (CustomerItem) customerCombo.getSelectedItem();

        if (customer == null) {
            updateUnits();
            return;
        }

        String sql =
            "SELECT reading_id, customer_id, units_consumed " +
            "FROM meter_readings WHERE customer_id = ? " +
            "ORDER BY reading_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, customer.id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    readingCombo.addItem(
                        new ReadingItem(
                            rs.getInt("reading_id"),
                            rs.getInt("customer_id"),
                            rs.getDouble("units_consumed")
                        )
                    );
                }
            }

            updateUnits();

        } catch (SQLException ex) {
            showError("Could not load meter readings", ex);
        }
    }

    private void updateUnits() {
        ReadingItem reading =
            (ReadingItem) readingCombo.getSelectedItem();

        if (reading == null) {
            unitsLabel.setText("0 units");
        } else {
            unitsLabel.setText(
                String.format("%.2f units", reading.units)
            );
        }

        calculateBill();
    }

    private double getTotal() {
        ReadingItem reading =
            (ReadingItem) readingCombo.getSelectedItem();

        if (reading == null) {
            return 0;
        }

        double rate = Double.parseDouble(rateField.getText().trim());
        double fixed = Double.parseDouble(fixedField.getText().trim());

        if (!Double.isFinite(rate) || !Double.isFinite(fixed)) {
            throw new IllegalArgumentException(
                "Enter valid rate and fixed charge values."
            );
        }

        if (rate < 0 || fixed < 0) {
            throw new IllegalArgumentException(
                "Rate and fixed charge cannot be negative."
            );
        }

        return reading.units * rate + fixed;
    }

    private void calculateBill() {
        try {
            totalLabel.setText(
                String.format("₹%.2f", getTotal())
            );
        } catch (NumberFormatException ex) {
            totalLabel.setText("Check rate/charge");
        } catch (IllegalArgumentException ex) {
            totalLabel.setText("Invalid amount");
        }
    }

    // Checks all required inputs before saving.
    private boolean validateForm() {
        if (customerCombo.getSelectedItem() == null) {
            showValidationError("Please select a Consumer ID.");
            customerCombo.requestFocusInWindow();
            return false;
        }

        if (readingCombo.getSelectedItem() == null) {
            showValidationError(
                "Please select a meter reading for this customer."
            );
            readingCombo.requestFocusInWindow();
            return false;
        }

        if (monthField.getText().trim().isEmpty()) {
            showValidationError("Billing Month is mandatory.");
            monthField.requestFocusInWindow();
            return false;
        }

        if (rateField.getText().trim().isEmpty()) {
            showValidationError("Rate Per Unit is mandatory.");
            rateField.requestFocusInWindow();
            return false;
        }

        if (fixedField.getText().trim().isEmpty()) {
            showValidationError("Fixed Charge is mandatory.");
            fixedField.requestFocusInWindow();
            return false;
        }

        if (dueDateField.getText().trim().isEmpty()) {
            showValidationError("Due Date is mandatory.");
            dueDateField.requestFocusInWindow();
            return false;
        }

        try {
            YearMonth.parse(monthField.getText().trim());
        } catch (Exception ex) {
            showValidationError(
                "Enter a valid Billing Month in YYYY-MM format."
            );
            monthField.requestFocusInWindow();
            return false;
        }

        try {
            LocalDate dueDate =
                LocalDate.parse(dueDateField.getText().trim());

            if (dueDate.isBefore(LocalDate.now())) {
                showValidationError(
                    "Due Date cannot be in the past."
                );
                dueDateField.requestFocusInWindow();
                return false;
            }
        } catch (Exception ex) {
            showValidationError(
                "Enter a valid Due Date in YYYY-MM-DD format."
            );
            dueDateField.requestFocusInWindow();
            return false;
        }

        try {
            double rate =
                Double.parseDouble(rateField.getText().trim());

            double fixed =
                Double.parseDouble(fixedField.getText().trim());

            if (!Double.isFinite(rate) || rate < 0) {
                showValidationError(
                    "Rate Per Unit must be a valid non-negative number."
                );
                rateField.requestFocusInWindow();
                return false;
            }

            if (!Double.isFinite(fixed) || fixed < 0) {
                showValidationError(
                    "Fixed Charge must be a valid non-negative number."
                );
                fixedField.requestFocusInWindow();
                return false;
            }

        } catch (NumberFormatException ex) {
            showValidationError(
                "Rate Per Unit and Fixed Charge must be numeric."
            );
            return false;
        }

        return true;
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Required Field / Validation Error",
            JOptionPane.WARNING_MESSAGE
        );
    }

    private void saveBill() {
        if (!validateForm()) {
            return;
        }

        CustomerItem customer =
            (CustomerItem) customerCombo.getSelectedItem();

        ReadingItem reading =
            (ReadingItem) readingCombo.getSelectedItem();

        if (reading.customerId != customer.id) {
            showValidationError(
                "The selected reading does not belong to this customer."
            );
            return;
        }

        String month = monthField.getText().trim();
        LocalDate parsedDueDate =
            LocalDate.parse(dueDateField.getText().trim());

        try {
            double rate =
                Double.parseDouble(rateField.getText().trim());

            double fixed =
                Double.parseDouble(fixedField.getText().trim());

            double total = getTotal();

            if (!Double.isFinite(total)) {
                showValidationError("The total bill is invalid.");
                return;
            }

            String duplicateSql =
                "SELECT COUNT(*) FROM bills " +
                "WHERE reading_id = ? AND billing_month = ?";

            String insertSql =
                "INSERT INTO bills " +
                "(customer_id, reading_id, billing_month, " +
                "units_consumed, rate_per_unit, total_amount, " +
                "bill_date, due_date, payment_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Unpaid')";

            try (Connection con = DBConnection.getConnection()) {

                try (PreparedStatement check =
                         con.prepareStatement(duplicateSql)) {

                    check.setInt(1, reading.id);
                    check.setString(2, month);

                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            showValidationError(
                                "A bill already exists for this reading " +
                                "and billing month."
                            );
                            return;
                        }
                    }
                }

                try (PreparedStatement ps =
                         con.prepareStatement(insertSql)) {

                    ps.setInt(1, customer.id);
                    ps.setInt(2, reading.id);
                    ps.setString(3, month);
                    ps.setDouble(4, reading.units);
                    ps.setDouble(5, rate);
                    ps.setDouble(6, total);
                    ps.setDate(7, Date.valueOf(LocalDate.now()));
                    ps.setDate(8, Date.valueOf(parsedDueDate));

                    ps.executeUpdate();
                }
            }

            JOptionPane.showMessageDialog(
                this,
                "Bill saved successfully!\n" +
                "Consumer ID: " + customer.consumerId +
                "\nCustomer: " + customer.name +
                "\nUnits: " + reading.units +
                "\nTotal: ₹" + String.format("%.2f", total) +
                "\nStatus: Unpaid",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );

            clearForm();
            loadBills();

        } catch (NumberFormatException ex) {
            showValidationError(
                "Enter valid numeric values for rate and fixed charge."
            );
        } catch (SQLException ex) {
            showError("Could not save bill", ex);
        } catch (IllegalArgumentException ex) {
            showValidationError(
                "Invalid billing information: " + ex.getMessage()
            );
        }
    }

    private void loadBills() {
        tableModel.setRowCount(0);

        String sql =
            "SELECT b.bill_id, c.consumer_id, b.reading_id, " +
            "b.billing_month, b.units_consumed, b.rate_per_unit, " +
            "b.total_amount, b.due_date, b.payment_status " +
            "FROM bills b " +
            "INNER JOIN customers c " +
            "ON b.customer_id = c.customer_id " +
            "ORDER BY b.bill_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("bill_id"),
                    rs.getString("consumer_id"),
                    rs.getInt("reading_id"),
                    rs.getString("billing_month"),
                    rs.getDouble("units_consumed"),
                    rs.getDouble("rate_per_unit"),
                    rs.getDouble("total_amount"),
                    rs.getDate("due_date"),
                    rs.getString("payment_status")
                });
            }

        } catch (SQLException ex) {
            showError("Could not load bills", ex);
        }
    }

    private void clearForm() {
        monthField.setText(YearMonth.now().toString());
        rateField.setText("6.00");
        fixedField.setText("50.00");

        dueDateField.setText(
            LocalDate.now().plusDays(15).toString()
        );

        if (customerCombo.getItemCount() > 0) {
            customerCombo.setSelectedIndex(0);
        }

        calculateBill();
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
