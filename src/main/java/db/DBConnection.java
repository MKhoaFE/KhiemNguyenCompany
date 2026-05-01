package db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL = "jdbc:sqlite:database/data.db";

    public static Connection getConnection() {
        try {
            File dbFile = new File("database/data.db");

            // 🔥 In ra path thật
            System.out.println("DB PATH: " + dbFile.getAbsolutePath());

            return DriverManager.getConnection(URL);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}