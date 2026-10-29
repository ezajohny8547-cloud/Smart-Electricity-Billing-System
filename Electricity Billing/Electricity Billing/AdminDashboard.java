import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AdminDashboard extends JFrame {

    private static final Color NAVY = new Color(25, 55, 85);
    private static final Color GREEN = new Color(25, 155, 95);
    private static final Color LIGHT_BG = new Color(245, 248, 252);

    public AdminDashboard() {
        setTitle("SEUBS - Admin Dashboard");
        setSize(900, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(LIGHT_BG);

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new GridLayout(0, 1, 8, 8));
        sidebar.setBackground(NAVY);
        sidebar.setBorder(new EmptyBorder(25, 15, 25, 15));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel logo = new JLabel("SEUBS ADMIN");
        logo.setFont(new Font("Arial", Font.BOLD, 20));
        logo.setForeground(Color.WHITE);
        logo.setHorizontalAlignment(SwingConstants.CENTER);

        JButton dashboardButton = new JButton("Dashboard");
        JButton staffButton = new JButton("Manage Staff");
        JButton reportsButton = new JButton("Reports");
        JButton logoutButton = new JButton("Logout");

        sidebar.add(logo);
        sidebar.add(dashboardButton);
        sidebar.add(staffButton);
        sidebar.add(reportsButton);
        sidebar.add(logoutButton);

        // Main content
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setBackground(LIGHT_BG);
        content.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Admin Dashboard");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(NAVY);

        JLabel description = new JLabel(
            "<html>Welcome, Admin!<br><br>" +
            "Manage staff accounts and view system reports.</html>"
        );
        description.setFont(new Font("Arial", Font.PLAIN, 17));

        JButton manageButton = new JButton("Manage Staff");
        manageButton.setBackground(GREEN);
        manageButton.setForeground(Color.WHITE);
        manageButton.setFocusPainted(false);

        content.add(title, BorderLayout.NORTH);
        content.add(description, BorderLayout.CENTER);
        content.add(manageButton, BorderLayout.SOUTH);

        // Manage Staff actions
        staffButton.addActionListener(e -> openStaffManagement());
        manageButton.addActionListener(e -> openStaffManagement());

        // Dashboard action
        dashboardButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                this,
                "You are on the Admin Dashboard.",
                "Dashboard",
                JOptionPane.INFORMATION_MESSAGE
            );
        });

        // Reports action
        reportsButton.addActionListener(e -> openReports());

        // Logout action with confirmation
        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                new LoginPage().setVisible(true);
                dispose();
            }
        });

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(content, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void openStaffManagement() {
        try {
            StaffManagement staffWindow = new StaffManagement(this);
            staffWindow.setVisible(true);
            setVisible(false);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not open Staff Management.\n" + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void openReports() {
        try {
            new Reports(this);
            setVisible(false);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not open Reports.\n" + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
