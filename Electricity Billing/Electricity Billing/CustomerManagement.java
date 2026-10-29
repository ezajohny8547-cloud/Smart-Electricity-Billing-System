import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

public class CustomerManagement {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        // Get customer name
        String name;
        do {
            System.out.print("Enter customer name *: ");
            name = sc.nextLine().trim();

            if (name.isEmpty()) {
                System.out.println("Customer name is required!");
            }
        } while (name.isEmpty());

        // Get address
        String address;
        do {
            System.out.print("Enter address *: ");
            address = sc.nextLine().trim();

            if (address.isEmpty()) {
                System.out.println("Address is required!");
            }
        } while (address.isEmpty());

        // Get phone number
        String phone;
        do {
            System.out.print("Enter phone number *: ");
            phone = sc.nextLine().trim();

            if (phone.isEmpty()) {
                System.out.println("Phone number is required!");
            } else if (!phone.matches("[0-9]{10}")) {
                System.out.println(
                    "Enter a valid 10-digit phone number!"
                );
                phone = "";
            }
        } while (phone.isEmpty());

        // Get email
        String email;
        do {
            System.out.print("Enter email *: ");
            email = sc.nextLine().trim();

            if (email.isEmpty()) {
                System.out.println("Email is required!");
            } else if (!email.matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                System.out.println("Enter a valid email address!");
                email = "";
            }
        } while (email.isEmpty());

        String sql = "INSERT INTO customers "
                   + "(full_name, address, phone, email) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, phone);
            ps.setString(4, email);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println(
                    "Customer added successfully!"
                );
            }

        } catch (SQLException e) {
            System.out.println("Error adding customer.");
            e.printStackTrace();
        }

        sc.close();
    }
}