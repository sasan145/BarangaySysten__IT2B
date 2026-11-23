package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.SettingsDao;
import com.scc.dao.UserDao;
import com.scc.model.User.UserRecord;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner sc;
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final SettingsDao settingsDao = new SettingsDao();

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.sc = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("=============================================");
            System.out.println("Barangay Document Request and Tracking System");
            System.out.println(settingsDao.get(SettingsDao.BARANGAY_NAME, "Barangay"));
            System.out.println("=============================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Select an Option: ");

            switch (sc.nextLine().trim()) {
                case "1" -> handleLogin();
                case "2" -> handleRegister();
                case "3" -> {
                    System.out.println("Exiting Program. THANK YOU FOR USING THE SYSTEM!");
                    running = false;
                }
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    private void handleRegister() {
        if (!settingsDao.isRegistrationOpen()) {
            System.out.println("-> Registration is currently closed. Please contact the administrator.");
            return;
        }

        System.out.print("Enter new Username: ");
        String newUser = sc.nextLine().trim();
        System.out.print("Enter Password: ");
        String newPass = sc.nextLine();

        if (newUser.isBlank() || newPass.isBlank()) {
            System.out.println("-> [ERROR] Username and password are required.");
            return;
        }

        // Admin accounts can only be created by an existing admin.
        System.out.print("Select Role: [1] Staff [2] Resident: ");
        String role = switch (sc.nextLine().trim()) {
            case "1" -> "STAFF";
            case "2" -> "RESIDENT";
            default -> null;
        };

        if (role == null) {
            System.out.println("-> [ERROR] Invalid Role Selection!");
            return;
        }
        userDao.registerUser(newUser, newPass, role);
    }

    private void handleLogin() {
        System.out.print("Enter Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        UserRecord user = userDao.login(username, password);
        if (user == null) return;

        System.out.println(" > Welcome " + user.username() + " | ROLE: " + user.role());
        logDao.logAction(user.id(), "LOGIN", user.role() + " logged in: " + user.username());

        BarangayMenu.Input in = new BarangayMenu.Input(sc);
        switch (user.role()) {
            case "ADMIN" -> new AdminMenu(userDao, sc).display(user);
            case "STAFF" -> new BarangayMenu.StaffMenu(in, user).show();
            case "RESIDENT" -> new BarangayMenu.ResidentMenu(in, user).show();
            default -> System.out.println("Account role not supported for this menu!");
        }

        logDao.logAction(user.id(), "LOGOUT", user.username() + " logged out");
    }
}