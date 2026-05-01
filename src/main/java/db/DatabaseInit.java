package db;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInit {

    public static void init() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("INIT DB...");

            String sql = new String(Files.readAllBytes(Paths.get("database/init.sql")));

            // 🔥 EXECUTE TOÀN BỘ FILE (QUAN TRỌNG)
            stmt.executeUpdate(sql);

            System.out.println("✅ DB INIT DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}