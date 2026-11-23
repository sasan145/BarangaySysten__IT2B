package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() {
        DatabaseConnection.initialize();
        createActivityLogsTable();
        createSettingsTable();
        new UserDao();
        new DocumentTypeDao();
        new SettingsDao();
        System.out.println(" -> Database tables verified.");
    }

    private static void createActivityLogsTable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {

            boolean hasNewSchema = false;
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(activity_logs)")) {
                while (rs.next()) {
                    if ("action_type".equals(rs.getString("name"))) hasNewSchema = true;
                }
            }
            if (!hasNewSchema) st.execute("DROP TABLE IF EXISTS activity_logs");

            st.execute("CREATE TABLE IF NOT EXISTS activity_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "action_type TEXT NOT NULL, " +
                    "description TEXT, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL)");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void createSettingsTable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS settings (" +
                    "setting_key TEXT PRIMARY KEY, " +
                    "setting_value TEXT NOT NULL)");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}