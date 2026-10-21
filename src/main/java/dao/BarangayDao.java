package barangay.dao;

import barangay.model.BarangayEntity.DocumentRequest;
import barangay.model.BarangayEntity.DocumentType;
import barangay.model.BarangayEntity.Resident;
import barangay.model.BarangayEntity.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BarangayDao {
    private BarangayDao() {}

    public static class UserDao {

        /** Returns false if the username is taken or the insert fails. */
        public boolean insert(String username, String password, String role, String status) {
            String sql = "INSERT INTO users (username, password, role, status) VALUES (?, ?, ?, ?)";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, role);
                ps.setString(4, status);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) return false;
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        public Optional<User> login(String username, String password) {
            String sql = "SELECT id, username, role, status FROM users WHERE username = ? AND password = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(map(rs));
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            return Optional.empty();
        }

        public List<User> findAll() {
            List<User> list = new ArrayList<>();
            String sql = "SELECT id, username, role, status FROM users ORDER BY id";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            return list;
        }

        public boolean updateStatus(int id, String status) {
            String sql = "UPDATE users SET status = ? WHERE id = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, id);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        /** Creates admin / admin123 on first run so someone can log in. */
        public void seedDefaultAdmin() {
            String sql = "INSERT OR IGNORE INTO users (username, password, role, status) VALUES ('admin', 'admin123', 'ADMIN', 'ACTIVE')";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }

        private User map(ResultSet rs) throws SQLException {
            return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"), rs.getString("status"));
        }
    }

    public static class ResidentDao {
        private static final String COLUMNS = "id, name, birth_date, address, contact";

        public boolean insert(Integer userId, String name, String birthDate, String address, String contact) {
            String sql = "INSERT INTO residents (user_id, name, birth_date, address, contact) VALUES (?, ?, ?, ?, ?)";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                if (userId != null) ps.setInt(1, userId);
                else ps.setNull(1, Types.INTEGER);
                ps.setString(2, name);
                ps.setString(3, birthDate);
                ps.setString(4, address);
                ps.setString(5, contact);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        public List<Resident> search(String query) {
            List<Resident> list = new ArrayList<>();
            String sql = "SELECT " + COLUMNS + " FROM residents WHERE name LIKE ? ORDER BY id";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, "%" + query + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            return list;
        }

        public Optional<Resident> findById(int id) {
            return findOne("SELECT " + COLUMNS + " FROM residents WHERE id = ?", id);
        }

        public Optional<Resident> findByUserId(int userId) {
            return findOne("SELECT " + COLUMNS + " FROM residents WHERE user_id = ?", userId);
        }

        public boolean update(int id, String name, String birthDate, String address, String contact) {
            String sql = "UPDATE residents SET name = ?, birth_date = ?, address = ?, contact = ? WHERE id = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, birthDate);
                ps.setString(3, address);
                ps.setString(4, contact);
                ps.setInt(5, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        public boolean delete(int id) {
            String sql = "DELETE FROM residents WHERE id = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        private Optional<Resident> findOne(String sql, int param) {
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, param);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(map(rs));
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            return Optional.empty();
        }

        private Resident map(ResultSet rs) throws SQLException {
            return new Resident(rs.getInt("id"), rs.getString("name"), rs.getString("birth_date"),
                    rs.getString("address"), rs.getString("contact"));
        }
    }

    public static class DocumentTypeDao {

        public List<DocumentType> findAll() {
            return query("SELECT id, name, description, fee, status FROM document_types ORDER BY id");
        }

        public List<DocumentType> findActive() {
            return query("SELECT id, name, description, fee, status FROM document_types WHERE status = 'ACTIVE' ORDER BY id");
        }

        public boolean insert(String name, String description, double fee) {
            String sql = "INSERT INTO document_types (name, description, fee, status) VALUES (?, ?, ?, 'ACTIVE')";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, description);
                ps.setDouble(3, fee);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        public boolean update(int id, String name, String description, double fee) {
            String sql = "UPDATE document_types SET name = ?, description = ?, fee = ? WHERE id = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, description);
                ps.setDouble(3, fee);
                ps.setInt(4, id);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        public boolean setStatus(int id, String status) {
            String sql = "UPDATE document_types SET status = ? WHERE id = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, id);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        /** Adds a few common documents on first run. */
        public void seedDefaults() {
            String sql = "INSERT OR IGNORE INTO document_types (name, description, fee, status) VALUES (?, ?, ?, 'ACTIVE')";
            Object[][] defaults = {
                    {"Barangay Clearance", "General purpose clearance", 50.0},
                    {"Certificate of Residency", "Proof of residency", 30.0},
                    {"Certificate of Indigency", "For financial assistance", 0.0}
            };
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (Object[] d : defaults) {
                    ps.setString(1, (String) d[0]);
                    ps.setString(2, (String) d[1]);
                    ps.setDouble(3, (Double) d[2]);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }

        private List<DocumentType> query(String sql) {
            List<DocumentType> list = new ArrayList<>();
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DocumentType(rs.getInt("id"), rs.getString("name"),
                            rs.getString("description"), rs.getDouble("fee"), rs.getString("status")));
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            return list;
        }
    }

    public static class DocumentRequestDao {
        private static final String SELECT =
                "SELECT r.id, res.name, dt.name AS doc_name, dt.fee, r.status " +
                        "FROM document_requests r " +
                        "JOIN residents res ON r.resident_id = res.id " +
                        "JOIN document_types dt ON r.document_type_id = dt.id ";

        public boolean insert(int residentId, int documentTypeId) {
            String sql = "INSERT INTO document_requests (resident_id, document_type_id, status) VALUES (?, ?, 'PENDING')";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, residentId);
                ps.setInt(2, documentTypeId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        public List<DocumentRequest> findAll() {
            return query(SELECT + "ORDER BY r.id", -1);
        }

        public List<DocumentRequest> findByResident(int residentId) {
            return query(SELECT + "WHERE r.resident_id = ? ORDER BY r.id", residentId);
        }

        public boolean updateStatus(int id, String status) {
            String sql = "UPDATE document_requests SET status = ? WHERE id = ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
                return false;
            }
        }

        private List<DocumentRequest> query(String sql, int param) {
            List<DocumentRequest> list = new ArrayList<>();
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                if (param >= 0) ps.setInt(1, param);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new DocumentRequest(rs.getInt("id"), rs.getString("name"),
                                rs.getString("doc_name"), rs.getDouble("fee"), rs.getString("status")));
                    }
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            return list;
        }
    }

    public static class ActivityLogDao {

        public void log(String action) {
            String sql = "INSERT INTO activity_logs (action) VALUES (?)";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, action);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }

        public List<String> findRecent(int limit) {
            List<String> list = new ArrayList<>();
            String sql = "SELECT timestamp, action FROM activity_logs ORDER BY id DESC LIMIT ?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add("[" + rs.getString("timestamp") + "] " + rs.getString("action"));
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            return list;
        }
    }
}