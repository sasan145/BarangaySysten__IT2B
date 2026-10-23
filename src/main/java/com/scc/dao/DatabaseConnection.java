package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:barangay.db";

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL);
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to database", e);
        }
    }

    public static void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL, " +
                    "role TEXT NOT NULL, " +
                    "status TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS residents (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "name TEXT NOT NULL, " +
                    "birth_date TEXT, " +
                    "address TEXT, " +
                    "contact TEXT, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS document_types (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "description TEXT, " +
                    "fee REAL NOT NULL DEFAULT 0, " +
                    "status TEXT NOT NULL DEFAULT 'ACTIVE')");

            stmt.execute("CREATE TABLE IF NOT EXISTS document_requests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "resident_id INTEGER NOT NULL, " +
                    "document_type_id INTEGER NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'PENDING', " +
                    "FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (document_type_id) REFERENCES document_types(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS activity_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "action TEXT, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}
