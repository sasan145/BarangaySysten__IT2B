package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportDao {

    private static final String FEE_JOIN =
            "SELECT COALESCE(SUM(dt.fee), 0) FROM document_requests dr "
                    + "JOIN document_types dt ON dt.id = dr.document_type_id ";

    public Map<String, Integer> usersByRole() {
        return grouped("SELECT role, COUNT(*) FROM users GROUP BY role");
    }

    public Map<String, Integer> usersByStatus() {
        return grouped("SELECT status, COUNT(*) FROM users GROUP BY status");
    }

    public Map<String, Integer> requestsByStatus() {
        return grouped("SELECT status, COUNT(*) FROM document_requests GROUP BY status");
    }

    public int totalUsers() {
        return scalarInt("SELECT COUNT(*) FROM users");
    }

    public int totalResidents() {
        return scalarInt("SELECT COUNT(*) FROM residents");
    }

    public int totalRequests() {
        return scalarInt("SELECT COUNT(*) FROM document_requests");
    }

    public int totalDocumentTypes() {
        return scalarInt("SELECT COUNT(*) FROM document_types");
    }

    public int activeDocumentTypes() {
        return scalarInt("SELECT COUNT(*) FROM document_types WHERE status = 'ACTIVE'");
    }

    /** Fees from requests already RELEASED. */
    public double feesCollected() {
        return scalarDouble(FEE_JOIN + "WHERE dr.status = 'RELEASED'");
    }

    /** Fees from requests still in progress (not released, not canceled). */
    public double feesPending() {
        return scalarDouble(FEE_JOIN + "WHERE dr.status IN ('PENDING', 'PROCESSING', 'APPROVED')");
    }

    private Map<String, Integer> grouped(String sql) {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return map;
    }

    private int scalarInt(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return 0;
    }

    private double scalarDouble(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.out.println("-> [ERROR] " + e.getMessage());
        }
        return 0;
    }
}