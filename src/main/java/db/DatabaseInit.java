package db;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInit {

    public static void init() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = new String(Files.readAllBytes(Paths.get("C:/Users/Admin/IdeaProjects/KhiemNguyenCompany/database/init.sql")));

            // Tách từng câu lệnh bằng dấu ;
            String[] queries = sql.split(";");

            for (String query : queries) {
                query = query.trim();

                // Bỏ qua dòng rỗng hoặc comment
                if (query.isEmpty() || query.startsWith("--")) continue;

                stmt.execute(query);
            }

            System.out.println("DB INIT DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}