package db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInit {

    public static void init() {
        // Đảm bảo thư mục database/ tồn tại (SQLite không tự tạo thư mục cha)
        try { Files.createDirectories(Paths.get("database")); } catch (Exception ignored) {}

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("INIT DB...");

            String sql = loadSql();
            // SQLite không cho chạy nhiều statement một lúc → tách từng câu
            for (String statement : sql.split(";")) {
                String s = statement.trim();
                if (!s.isEmpty()) {
                    try { stmt.executeUpdate(s); } catch (Exception ignored) {}
                }
            }

            // ── safe migrations ───────────────────────────────────────────────
            runMigration(stmt, "ALTER TABLE inventory_entries ADD COLUMN supplier_name TEXT DEFAULT ''");
            runMigration(stmt, "ALTER TABLE inventory_entries ADD COLUMN supplier_phone TEXT DEFAULT ''");
            runMigration(stmt, "ALTER TABLE inventory_entries ADD COLUMN supplier_address TEXT DEFAULT ''");
            runMigration(stmt, "ALTER TABLE orders ADD COLUMN buyer_name TEXT DEFAULT ''");
            runMigration(stmt, "ALTER TABLE orders ADD COLUMN buyer_phone TEXT DEFAULT ''");
            runMigration(stmt, "ALTER TABLE orders ADD COLUMN buyer_address TEXT DEFAULT ''");
            runMigration(stmt, "ALTER TABLE order_items ADD COLUMN ck_percent REAL NOT NULL DEFAULT 0");

            System.out.println("✅ DB INIT DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Đọc init.sql: ưu tiên từ resource trong JAR,
     * fallback sang file ngoài (khi chạy thẳng trong IDE).
     */
    private static String loadSql() throws Exception {
        // 1. Thử đọc từ classpath (trong JAR sau khi đóng gói)
        try (InputStream is = DatabaseInit.class.getResourceAsStream("/database/init.sql")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        // 2. Fallback: đọc file ngoài (khi chạy trong IDE)
        return new String(Files.readAllBytes(Paths.get("database/init.sql")), StandardCharsets.UTF_8);
    }

    private static void runMigration(Statement stmt, String sql) {
        try { stmt.executeUpdate(sql); } catch (Exception ignored) {}
    }
}