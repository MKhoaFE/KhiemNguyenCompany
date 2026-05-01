package db;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInit {

    public static void init() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS card_types (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   name TEXT,
                   denomination INTEGER,
                   default_price REAL,
                   default_discount REAL,
                   created_at TEXT
                );
            """);

            System.out.println("DB Initialized");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}