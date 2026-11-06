package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public boolean logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] Could not write log: " + e.getMessage());
            return false;
        }
    }

    public List<ActivityLog> getAllLogs() {
        return query("SELECT * FROM activity_logs ORDER BY timestamp DESC, id DESC", null);
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        return query("SELECT * FROM activity_logs WHERE user_id = ? ORDER BY timestamp DESC, id DESC", userId);
    }

    public boolean clearLogs() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM activity_logs");
            return true;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] Could not clear logs: " + e.getMessage());
            return false;
        }
    }

    private List<ActivityLog> query(String sql, Integer userId) {
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("action_type"),
                            rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] Could not read logs: " + e.getMessage());
        }
        return logs;
    }
}