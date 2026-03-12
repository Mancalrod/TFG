import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/tfgdb";
        String user = "tfg";
        String password = "tfg1234";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to the PostgreSQL server successfully.");
            
            String sql = "SELECT id, nombre, ruta FROM material WHERE id = 18";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Found Material 18!");
                    System.out.println("ID: " + rs.getLong("id"));
                    System.out.println("Nombre: " + rs.getString("nombre"));
                    System.out.println("Ruta: " + rs.getString("ruta"));
                } else {
                    System.out.println("Material 18 not found.");
                }
            }

            System.out.println("\nChecking all materials to see a few paths:");
            sql = "SELECT id, nombre, ruta FROM material LIMIT 5";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    System.out.println(rs.getLong("id") + " | " + rs.getString("nombre") + " | " + rs.getString("ruta"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
