package com.scc.model;

public final class User {

    private User() {}

    public record UserRecord(int id, String username, String role, String status) {}

    public record Resident(int id, String name, String birthDate, String address, String contact) {}

    public record DocumentType(int id, String name, String description, double fee, String status) {}

    public record DocumentRequest(int id, String residentName, String documentName, double fee, String status) {}
}