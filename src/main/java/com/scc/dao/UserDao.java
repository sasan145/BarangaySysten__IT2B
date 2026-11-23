package com.scc.dao;

import com.scc.model.User.UserRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public UserDao() {
        seedDefaultAdmin();
    }

    /**
     * Makes sure at least one APPROVED admin exists so you can never be locked out.
     * - If an approved admin already exists, nothing happens.
     * - If an account named "admin" exists but is not an approved admin, it is repaired.
     * - Otherwise admin / admin123 is created.
     */
    private void seedDefaultAdmin() {
        int approvedAdmins = 0;

        // Step 1: read only, and close everything before any write happens.
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND status = 'APPROVED'")) {
            if (rs.next()) approvedAdmins = rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return;
        }

        if (approvedAdmins > 0) return;

        // Step 2: repair an existing "admin" account, or create a new one.
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            int repaired = st.executeUpdate(
                    "UPDATE users SET role = 'ADMIN', status = 'APPROVED' WHERE username = 'admin'");
            if (repaired > 0) {
                System.out.println(" -> Existing 'admin' account was repaired (ADMIN, APPROVED).");
            } else {
                insert("admin", "admin123", "ADMIN", "APPROVED");
                System.out.println(" -> Default admin created: admin / admin123");
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
    }

    public boolean insert(String username, String password, String role, String status) {
        String sql = "INSERT INTO users (username, password, role, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public boolean registerUser(String username, String password, String role) {
        if (insert(username, password, role, "PENDING")) {
            System.out.println("-> Registration successful. Wait for admin approval.");
            return true;
        }
        System.out.println("-> Registration failed (username may already exist).");
        return false;
    }

    public UserRecord login(String username, String password) {
        String sql = "SELECT id, username, role, status FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserRecord u = map(rs);
                    if (!"APPROVED".equals(u.status())) {
                        System.out.println("-> Account is not approved yet.");
                        return null;
                    }
                    return u;
                }
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        System.out.println("-> Invalid username or password.");
        return null;
    }

    public List<UserRecord> getAllUsers() {
        return list("SELECT id, username, role, status FROM users ORDER BY id");
    }

    public List<UserRecord> getPendingUser() {
        return list("SELECT id, username, role, status FROM users WHERE status = 'PENDING' ORDER BY id");
    }

    public boolean approveUser(int userId) {
        return update("UPDATE users SET status = 'APPROVED' WHERE id = ?", userId);
    }

    public boolean updateUserRoleAndStatus(int userId, String role, String status) {
        String sql = "UPDATE users SET role = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, status);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        return update("DELETE FROM users WHERE id = ?", userId);
    }

    // ---------- helpers ----------

    private boolean update(String sql, int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    private List<UserRecord> list(String sql) {
        List<UserRecord> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(map(rs));
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return users;
    }

    private UserRecord map(ResultSet rs) throws SQLException {
        return new UserRecord(rs.getInt("id"), rs.getString("username"),
                rs.getString("role"), rs.getString("status"));
    }
}