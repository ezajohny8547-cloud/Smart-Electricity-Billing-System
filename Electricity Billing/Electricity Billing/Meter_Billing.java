
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class Meter_Billing extends JFrame {

    private JFrame dashboard;
    private JComboBox<String> customerBox;
    private JTextField previousField;
    private JTextField currentField;
    private JTextField dateField;
    private JLabel unitsLabel;
    private DefaultTableModel model;

    public Meter_Billing(JFrame dashboard) {
        this.dashboard = dashboard;

        setTitle("Meter & Billing");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(244, 247, 250));

        JLabel title = new JLabel("Meter Reading Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(20, 43, 69));
        title.setBounds(30, 20, 500, 40);
        panel.add(title);

        JLabel customerLabel = requiredLabel("Customer");
        customerLabel.setBounds(40, 85, 130, 30);
        panel.add(customerLabel);

        customerBox = new JComboBox<>();
        customerBox.setBounds(180, 85, 300, 30);
        panel.add(customerBox);

        JLabel previousLabel = requiredLabel("Previous Reading");
        previousLabel.setBounds(40, 130, 130, 30);
        panel.add(previousLabel);

        previousField = new JTextField();
        previousField.setBounds(180, 130, 300, 30);
        panel.add(previousField);

        JLabel currentLabel = requiredLabel("Current Reading");
        currentLabel.setBounds(40, 175, 130, 30);
        panel.add(currentLabel);

        currentField = new JTextField();
        currentField.setBounds(180, 175, 300, 30);
        panel.add(currentField);

        JLabel dateLabel = requiredLabel("Reading Date (YYYY-MM-DD)");
        dateLabel.setBounds(40, 220, 180, 30);
        panel.add(dateLabel);

        dateField = new JTextField(LocalDate.now().toString());
        dateField.setBounds(220, 220, 260, 30);
        panel.add(dateField);

        JLabel unitsTitle = new JLabel("Units Consumed:");
        unitsTitle.setBounds(40, 265, 130, 30);
        panel.add(unitsTitle);

        unitsLabel = new JLabel("0");
        unitsLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        unitsLabel.setForeground(new Color(32, 164, 100));
        unitsLabel.setBounds(180, 265, 150, 30);
        panel.add(unitsLabel);

        JButton calculateButton = new JButton("CALCULATE");
        calculateButton.setBounds(40, 315, 140, 38);
        panel.add(calculateButton);

        JButton saveButton = new JButton("SAVE READING");
        saveButton.setBounds(190, 315, 160, 38);
        saveButton.setBackground(new Color(32, 164, 100));
        saveButton.setForeground(Color.WHITE);
        panel.add(saveButton);

        JButton backButton = new JButton("BACK");
        backButton.setBounds(360, 315, 120, 38);
        panel.add(backButton);

        model = new DefaultTableModel(
                new String[]{
                    "Reading ID", "Customer ID",
                    "Previous", "Current", "Units", "Date"
                }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(30, 380, 870, 190);
        panel.add(scrollPane);

        calculateButton.addActionListener(e -> calculateUnits());
        saveButton.addActionListener(e -> saveReading());

        backButton.addActionListener(e -> {
            dispose();
            if (dashboard != null) {
                dashboard.setVisible(true);
            }
        });

        add(panel);

        loadCustomers();
        loadReadings();
    }

    // Displays a red asterisk for required fields.
    private JLabel requiredLabel(String text) {
        JLabel label = new JLabel(
                "<html>" + text
                + " <font color='#DC2626'>*</font></html>");
        return label;
    }

    private boolean validateReadings() {
        String previousText = previousField.getText().trim();
        String currentText = currentField.getText().trim();
        String dateText = dateField.getText().trim();

        if (customerBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a customer.");
            customerBox.requestFocusInWindow();
            return false;
        }

        if (previousText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the previous reading.");
            previousField.requestFocusInWindow();
            return false;
        }

        if (currentText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the current reading.");
            currentField.requestFocusInWindow();
            return false;
        }

        if (dateText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the reading date.");
            dateField.requestFocusInWindow();
            return false;
        }

        double previous;
        double current;

        try {
            previous = Double.parseDouble(previousText);
            current = Double.parseDouble(currentText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Enter valid numbers for both meter readings.");
            previousField.requestFocusInWindow();
            return false;
        }

        if (!Double.isFinite(previous) || !Double.isFinite(current)) {
            JOptionPane.showMessageDialog(this,
                    "Meter readings must be valid finite numbers.");
            previousField.requestFocusInWindow();
            return false;
        }

        if (previous < 0 || current < 0) {
            JOptionPane.showMessageDialog(this,
                    "Meter readings cannot be negative.");
            previousField.requestFocusInWindow();
            return false;
        }

        if (current < previous) {
            JOptionPane.showMessageDialog(this,
                    "Current reading cannot be less than "
                    + "the previous reading.");
            currentField.requestFocusInWindow();
            return false;
        }

        try {
            LocalDate readingDate = LocalDate.parse(dateText);

            if (readingDate.isAfter(LocalDate.now())) {
                JOptionPane.showMessageDialog(this,
                        "Reading date cannot be in the future.");
                dateField.requestFocusInWindow();
                return false;
            }
        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Enter a valid date in YYYY-MM-DD format.");
            dateField.requestFocusInWindow();
            return false;
        }

        return true;
    }

    private void loadCustomers() {
        customerBox.removeAllItems();

        String sql =
                "SELECT customer_id, full_name FROM customers "
                + "ORDER BY customer_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                customerBox.addItem(
                        rs.getInt("customer_id") + " - "
                        + rs.getString("full_name"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load customers.\n" + e.getMessage());
        }
    }

    private void calculateUnits() {
        if (!validateReadings()) {
            return;
        }

        double previous =
                Double.parseDouble(previousField.getText().trim());
        double current =
                Double.parseDouble(currentField.getText().trim());

        double units = current - previous;
        unitsLabel.setText(String.valueOf(units));
    }

    private void saveReading() {
        if (!validateReadings()) {
            return;
        }

        String selected = (String) customerBox.getSelectedItem();

        try {
            int customerId =
                    Integer.parseInt(selected.split(" - ")[0]);

            double previous =
                    Double.parseDouble(previousField.getText().trim());
            double current =
                    Double.parseDouble(currentField.getText().trim());

            java.sql.Date readingDate =
                    java.sql.Date.valueOf(dateField.getText().trim());

            double units = current - previous;

            String sql =
                    "INSERT INTO meter_readings "
                    + "(customer_id, previous_reading, current_reading, "
                    + "units_consumed, reading_date) "
                    + "VALUES (?, ?, ?, ?, ?)";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, customerId);
                ps.setDouble(2, previous);
                ps.setDouble(3, current);
                ps.setDouble(4, units);
                ps.setDate(5, readingDate);

                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this,
                    "Meter reading saved successfully!\n"
                    + "Units consumed: " + units);

            previousField.setText("");
            currentField.setText("");
            unitsLabel.setText("0");
            dateField.setText(LocalDate.now().toString());

            loadReadings();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not save reading.\n" + e.getMessage());
        }
    }

    private void loadReadings() {
        model.setRowCount(0);

        String sql =
                "SELECT reading_id, customer_id, previous_reading, "
                + "current_reading, units_consumed, reading_date "
                + "FROM meter_readings ORDER BY reading_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("reading_id"),
                    rs.getInt("customer_id"),
                    rs.getDouble("previous_reading"),
                    rs.getDouble("current_reading"),
                    rs.getDouble("units_consumed"),
                    rs.getDate("reading_date")
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load readings.\n" + e.getMessage());
        }
    }
}