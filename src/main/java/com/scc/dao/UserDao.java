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

    /** Creates admin / admin123 if the users table is empty, so you can log in the first time. */
    private void seedDefaultAdmin() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                insert("admin", "admin123", "ADMIN", "APPROVED");
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