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

    public List<DocumentType> findActive() {
        List<DocumentType> types = new ArrayList<>();
        String sql = "SELECT id, name, description, fee, status FROM document_types "
                + "WHERE status = 'ACTIVE' ORDER BY id";
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