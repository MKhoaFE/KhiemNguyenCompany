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
            stmt.executeUpdate(sql);

            // ── safe migrations: add new columns if not yet present ──────────
            runMigration(stmt,
                    "ALTER TABLE inventory_entries ADD COLUMN supplier_name TEXT DEFAULT ''");
            runMigration(stmt,
                    "ALTER TABLE inventory_entries ADD COLUMN supplier_phone TEXT DEFAULT ''");
            runMigration(stmt,
                    "ALTER TABLE inventory_entries ADD COLUMN supplier_address TEXT DEFAULT ''");

            runMigration(stmt,
                    "ALTER TABLE orders ADD COLUMN buyer_name TEXT DEFAULT ''");
            runMigration(stmt,
                    "ALTER TABLE orders ADD COLUMN buyer_phone TEXT DEFAULT ''");
            runMigration(stmt,
                    "ALTER TABLE orders ADD COLUMN buyer_address TEXT DEFAULT ''");

            runMigration(stmt,
                    "ALTER TABLE order_items ADD COLUMN ck_percent REAL NOT NULL DEFAULT 0");

            System.out.println("✅ DB INIT DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Run a DDL statement, silently ignoring "duplicate column" errors. */
    private static void runMigration(Statement stmt, String sql) {
        try {
            stmt.executeUpdate(sql);
        } catch (Exception ignored) {
            // Column already exists — safe to ignore in SQLite
        }
    }
}