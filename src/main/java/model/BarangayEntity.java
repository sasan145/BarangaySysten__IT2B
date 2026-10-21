package barangay.model;

public final class BarangayEntity {
    private BarangayEntity() {}

    public record User(int id, String username, String role, String status) {}

    // 5 attributes. The user link (user_id) lives only in the database and is used by ResidentDao.
    public record Resident(int id, String name, String birthDate, String address, String contact) {}

    public record DocumentType(int id, String name, String description, double fee, String status) {}

    // 5 attributes. Built from a JOIN of document_requests, residents and document_types.
    public record DocumentRequest(int id, String residentName, String documentName, double fee, String status) {}
}