
import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class Settings extends JFrame {

    private JFrame dashboard;

    private JTextField rateField;
    private JTextField fixedChargeField;
    private JTextField dueDaysField;
    private JComboBox<String> paymentMethodCombo;
    private JCheckBox confirmPaymentCheck;
    private JCheckBox confirmDeleteCheck;

    private final Preferences preferences =
        Preferences.userNodeForPackage(Settings.class);

    private final Color NAVY = new Color(20, 43, 69);
    private final Color GREEN = new Color(32, 164, 100);

    public Settings(JFrame dashboard) {
        this.dashboard = dashboard;

        setTitle("Electricity Billing - Settings");
        setSize(700, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        );

        JLabel title = new JLabel("APPLICATION SETTINGS");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(NAVY);
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel settingsPanel = new JPanel(new GridLayout(7, 2, 12, 15));
        settingsPanel.setBorder(
            BorderFactory.createTitledBorder("Billing Preferences")
        );

        rateField = new JTextField();
        fixedChargeField = new JTextField();
        dueDaysField = new JTextField();

        paymentMethodCombo = new JComboBox<>(
            new String[]{"UPI", "Cash", "Card"}
        );

        confirmPaymentCheck = new JCheckBox(
            "Confirm before recording a payment"
        );
        confirmDeleteCheck = new JCheckBox(
            "Confirm before deleting customer records"
        );

        settingsPanel.add(new JLabel("Default Rate Per Unit (₹):"));
        settingsPanel.add(rateField);

        settingsPanel.add(new JLabel("Default Fixed Charge (₹):"));
        settingsPanel.add(fixedChargeField);

        settingsPanel.add(new JLabel("Default Due Period (days):"));
        settingsPanel.add(dueDaysField);

        settingsPanel.add(new JLabel("Default Payment Method:"));
        settingsPanel.add(paymentMethodCombo);

        settingsPanel.add(new JLabel("Payment Confirmation:"));
        settingsPanel.add(confirmPaymentCheck);

        settingsPanel.add(new JLabel("Delete Confirmation:"));
        settingsPanel.add(confirmDeleteCheck);

        settingsPanel.add(new JLabel("Storage:"));
        settingsPanel.add(new JLabel("Saved on this computer"));

        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton saveButton = new JButton("Save Settings");
        JButton resetButton = new JButton("Reset Defaults");
        JButton backButton = new JButton("Back to Dashboard");

        saveButton.setBackground(GREEN);
        saveButton.setForeground(Color.WHITE);

        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(backButton);

        mainPanel.add(settingsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        saveButton.addActionListener(e -> saveSettings());
        resetButton.addActionListener(e -> resetSettings());

        backButton.addActionListener(e -> {
            dispose();
            if (this.dashboard != null) {
                this.dashboard.setVisible(true);
            }
        });

        loadSettings();
        setVisible(true);
    }

    private void loadSettings() {
        rateField.setText(
            preferences.get("rate", "6.00")
        );

        fixedChargeField.setText(
            preferences.get("fixedCharge", "50.00")
        );

        dueDaysField.setText(
            preferences.get("dueDays", "15")
        );

        paymentMethodCombo.setSelectedItem(
            preferences.get("paymentMethod", "UPI")
        );

        confirmPaymentCheck.setSelected(
            preferences.getBoolean("confirmPayment", true)
        );

        confirmDeleteCheck.setSelected(
            preferences.getBoolean("confirmDelete", true)
        );
    }

    private void saveSettings() {
        try {
            double rate = Double.parseDouble(
                rateField.getText().trim()
            );

            double fixedCharge = Double.parseDouble(
                fixedChargeField.getText().trim()
            );

            int dueDays = Integer.parseInt(
                dueDaysField.getText().trim()
            );

            if (rate < 0 || fixedCharge < 0 || dueDays < 1) {
                JOptionPane.showMessageDialog(
                    this,
                    "Rate and fixed charge must be non-negative. "
                    + "Due period must be at least 1 day."
                );
                return;
            }

            preferences.put("rate", String.valueOf(rate));
            preferences.put(
                "fixedCharge", String.valueOf(fixedCharge)
            );
            preferences.put("dueDays", String.valueOf(dueDays));
            preferences.put(
                "paymentMethod",
                (String) paymentMethodCombo.getSelectedItem()
            );

            preferences.putBoolean(
                "confirmPayment",
                confirmPaymentCheck.isSelected()
            );

            preferences.putBoolean(
                "confirmDelete",
                confirmDeleteCheck.isSelected()
            );

            JOptionPane.showMessageDialog(
                this,
                "Settings saved successfully!"
            );

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Enter valid numbers for rate, fixed charge, and due days.",
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void resetSettings() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Reset all settings to their default values?",
            "Reset Settings",
            JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            preferences.put("rate", "6.00");
            preferences.put("fixedCharge", "50.00");
            preferences.put("dueDays", "15");
            preferences.put("paymentMethod", "UPI");
            preferences.putBoolean("confirmPayment", true);
            preferences.putBoolean("confirmDelete", true);

            loadSettings();

            JOptionPane.showMessageDialog(
                this,
                "Default settings restored."
            );
        }
    }
}
