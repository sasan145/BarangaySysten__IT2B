package com.scc.dao;

public class DatabaseInitializer {
    public static void initialize() {
        DatabaseConnection.initialize();
        new UserDao();
        System.out.println(" -> Database tables verified.");
    }
}