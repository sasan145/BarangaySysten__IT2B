package com.scc.dao;

import com.scc.model.User.DocumentRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DocumentRequestDao {

    private static final String SELECT =
            "SELECT dr.id, r.name AS resident_name, dt.name AS document_name, dt.fee, dr.status "
                    + "FROM document_requests dr "
                    + "JOIN residents r ON r.id = dr.resident_id "
                    + "JOIN document_types dt ON dt.id = dr.document_type_id ";

    public boolean insert(int residentId, int documentTypeId) {
        String sql = "INSERT INTO document_requests (resident_id, document_type_id, status) "
                + "SELECT ?, id, 'PENDING' FROM document_types WHERE id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, residentId);
            ps.setInt(2, documentTypeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public List<DocumentRequest> findAll() {
        return query(SELECT + "ORDER BY dr.id DESC", null);
    }

    public List<DocumentRequest> findByResident(int residentId) {
        return query(SELECT + "WHERE dr.resident_id = ? ORDER BY dr.id DESC", residentId);
    }

    public boolean updateStatus(int requestId, String status) {
        String sql = "UPDATE document_requests SET status = ? WHERE id = ? AND status <> 'CANCELED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    public boolean cancel(int requestId) {
        String sql = "UPDATE document_requests SET status = 'CANCELED' "
                + "WHERE id = ? AND status IN ('PENDING', 'PROCESSING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
            return false;
        }
    }

    private List<DocumentRequest> query(String sql, Integer param) {
        List<DocumentRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DocumentRequest(rs.getInt("id"), rs.getString("resident_name"),
                            rs.getString("document_name"), rs.getDouble("fee"), rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return list;
    }
}