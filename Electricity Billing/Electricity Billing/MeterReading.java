
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class MeterReading {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter Consumer ID (*): ");
        String consumerId = sc.nextLine().trim();

        if (consumerId.isEmpty()) {
            System.out.println("Error: Consumer ID is required.");
            sc.close();
            return;
        }

        int customerId;

        String customerSql =
                "SELECT customer_id FROM customers WHERE consumer_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(customerSql)) {

            ps.setString(1, consumerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    customerId = rs.getInt("customer_id");
                } else {
                    System.out.println("Error: Consumer ID not found.");
                    sc.close();
                    return;
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not find customer.");
            e.printStackTrace();
            sc.close();
            return;
        }

        System.out.println("Consumer ID: " + consumerId);

        double previous;
        double current;

        System.out.print("Enter previous meter reading (*): ");

        if (!sc.hasNextDouble()) {
            System.out.println("Error: Enter a valid numeric reading.");
            sc.close();
            return;
        }
        previous = sc.nextDouble();

        System.out.print("Enter current meter reading (*): ");

        if (!sc.hasNextDouble()) {
            System.out.println("Error: Enter a valid numeric reading.");
            sc.close();
            return;
        }
        current = sc.nextDouble();

        if (!Double.isFinite(previous) || !Double.isFinite(current)) {
            System.out.println("Error: Readings must be finite numbers.");
            sc.close();
            return;
        }

        if (previous < 0 || current < 0) {
            System.out.println("Error: Meter readings cannot be negative.");
            sc.close();
            return;
        }

        if (current < previous) {
            System.out.println(
                    "Error: Current reading cannot be less than previous reading.");
            sc.close();
            return;
        }

        double units = current - previous;

        System.out.println("Units consumed: " + units);

        System.out.print("Enter reading date (YYYY-MM-DD) (*): ");
        String date = sc.next();

        LocalDate readingDate;

        try {
            readingDate = LocalDate.parse(date);

            if (readingDate.isAfter(LocalDate.now())) {
                System.out.println("Error: Reading date cannot be in the future.");
                sc.close();
                return;
            }

        } catch (DateTimeParseException e) {
            System.out.println(
                    "Error: Enter a valid date in YYYY-MM-DD format.");
            sc.close();
            return;
        }

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
            ps.setDate(5, java.sql.Date.valueOf(readingDate));

            ps.executeUpdate();

            System.out.println("Meter reading saved successfully!");
            System.out.println("Consumer ID: " + consumerId);
            System.out.println("Units consumed: " + units);

        } catch (SQLException e) {
            System.out.println("Could not save meter reading.");
            e.printStackTrace();
        }

        sc.close();
    }
}
