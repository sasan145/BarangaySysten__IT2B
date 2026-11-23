package com.scc.dao;

import com.scc.model.User.DocumentType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DocumentTypeDao {

    private static final String SELECT = "SELECT id, name, description, fee, status FROM document_types ";

    public DocumentTypeDao() {
        seedDefaults();
    }

    private void seedDefaults() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM document_types")) {
            if (rs.next() && rs.getInt(1) == 0) {
                insert(conn, "Barangay Clearance", "Proof of good standing in the barangay", 50.0);
                insert(conn, "Certificate of Indigency", "For financial or medical assistance", 0.0);
                insert(conn, "Certificate of Residency", "Proof of residence in the barangay", 30.0);
                insert(conn, "Business Permit Clearance", "Barangay clearance for business", 100.0);
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
    }

    private void insert(Connection conn, String name, String description, double fee) throws SQLException {
        String sql = "INSERT INTO document_types (name, description, fee, status) VALUES (?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, fee);
            ps.executeUpdate();
        }
    }

    // ---------- read ----------

    public List<DocumentType> findActive() {
        return query(SELECT + "WHERE status = 'ACTIVE' ORDER BY id");
    }

    public List<DocumentType> findAll() {
        return query(SELECT + "ORDER BY id");
    }

    // ---------- admin management ----------

    public boolean insert(String name, String description, double fee) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            insert(conn, name, description, fee);
            return true;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public boolean update(int id, String name, String description, double fee) {
        String sql = "UPDATE document_types SET name = ?, description = ?, fee = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, fee);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public boolean setStatus(int id, String status) {
        String sql = "UPDATE document_types SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    /** True if at least one document request references this type. */
    public boolean isUsed(int id) {
        String sql = "SELECT COUNT(*) FROM document_requests WHERE document_type_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return true; // be safe: treat as used so it is not deleted
        }
    }

    public boolean delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM document_types WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    // ---------- helpers ----------

    private List<DocumentType> query(String sql) {
        List<DocumentType> types = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                types.add(new DocumentType(rs.getInt("id"), rs.getString("name"),
                        rs.getString("description"), rs.getDouble("fee"), rs.getString("status")));
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return types;
    }
}