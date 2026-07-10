import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://bikpnmlohulrhlndvl13-mysql.services.clever-cloud.com:3306/bikpnmlohulrhlndvl13?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = "usskzwcrj7osfxxj";
        String password = "1tu7wmrVJ2r7QslostLF";

        System.out.println("Attempting to connect to database...");
        try {
            // Load driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("SUCCESS: Connected to Clever Cloud MySQL successfully!");
            conn.close();
        } catch (ClassNotFoundException e) {
            System.err.println("Driver class not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("SQL Connection Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
