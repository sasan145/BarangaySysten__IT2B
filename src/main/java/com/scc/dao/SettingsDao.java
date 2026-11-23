package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDao {

    public static final String BARANGAY_NAME = "barangay_name";
    public static final String ALLOW_REGISTRATION = "allow_registration";

    public SettingsDao() {
        seedDefaults();
    }

    private void seedDefaults() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put(BARANGAY_NAME, "Barangay");
        defaults.put(ALLOW_REGISTRATION, "true");

        String sql = "INSERT OR IGNORE INTO settings (setting_key, setting_value) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> e : defaults.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, e.getValue());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
    }

    public String get(String key, String fallback) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return fallback;
    }

    public boolean set(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings (setting_key, setting_value) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public boolean isRegistrationOpen() {
        return Boolean.parseBoolean(get(ALLOW_REGISTRATION, "true"));
    }
}