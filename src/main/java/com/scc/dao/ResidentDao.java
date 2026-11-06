package com.scc.dao;

import com.scc.model.User.Resident;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResidentDao {

    private static final String COLS = "id, name, birth_date, address, contact";

    public boolean insert(Integer userId, String name, String birthDate, String address, String contact) {
        String sql = "INSERT INTO residents (user_id, name, birth_date, address, contact) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, birthDate);
            ps.setString(4, address);
            ps.setString(5, contact);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public List<Resident> search(String keyword) {
        String sql = "SELECT " + COLS + " FROM residents "
                + "WHERE name LIKE ? OR address LIKE ? OR contact LIKE ? ORDER BY id";
        List<Resident> result = new ArrayList<>();
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return result;
    }

    public Optional<Resident> findById(int id) {
        return findOne("SELECT " + COLS + " FROM residents WHERE id = ?", id);
    }

    public Optional<Resident> findByUserId(int userId) {
        return findOne("SELECT " + COLS + " FROM residents WHERE user_id = ?", userId);
    }

    public boolean update(int id, String name, String birthDate, String address, String contact) {
        String sql = "UPDATE residents SET name = ?, birth_date = ?, address = ?, contact = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, birthDate);
            ps.setString(3, address);
            ps.setString(4, contact);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM residents WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    private Optional<Resident> findOne(String sql, int param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return Optional.empty();
    }

    private Resident map(ResultSet rs) throws SQLException {
        return new Resident(rs.getInt("id"), rs.getString("name"),
                rs.getString("birth_date"), rs.getString("address"), rs.getString("contact"));
    }
}