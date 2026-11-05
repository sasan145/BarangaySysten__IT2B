package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User.UserRecord;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private static final List<String> VALID_ROLES = List.of("ADMIN", "STAFF", "RESIDENT");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void display(UserRecord admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("============================");
            System.out.println("====== ADMIN DASHBOARD =====");
            System.out.println("============================");
            System.out.println("1. View All User");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add User");
            System.out.println("5. Update User role / Status");
            System.out.println("6. Delete User");
            System.out.println("7. Logout");

            int select = readIntInput("Select: ");

            switch (select) {
                case 1 -> viewAllUser();
                case 2 -> viewPendingUser();
                case 3 -> approveUser();
                case 4 -> addUser();
                case 5 -> updateUserRoleAndStatus(admin);
                case 7 -> loggedIn = false;
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    // ---------- Input validation wrappers ----------

    /** Keeps asking until the admin types a whole number. Never throws on letters/symbols. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid input. Please enter a number.");
            }
        }
    }

    /** Returns a valid role, or null if the admin enters 0 to cancel. */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter New Role " + VALID_ROLES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (VALID_ROLES.contains(input)) return input;
            System.out.println("-> [ERROR] Invalid role. Valid roles: " + VALID_ROLES);
        }
    }

    /** Returns a valid status, or null if the admin enters 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter New Status " + VALID_STATUSES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (VALID_STATUSES.contains(input)) return input;
            System.out.println("-> [ERROR] Invalid status. Valid statuses: " + VALID_STATUSES);
        }
    }

    // ---------- Features ----------

    private void updateUserRoleAndStatus(UserRecord currentAdmin) {
        List<UserRecord> users = userDao.getAllUsers();
        displayUserTable(users, "SELECT USER TO UPDATE");
        if (users.isEmpty()) return;

        int targetUserId = readIntInput("Enter User ID to update (0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("-> Update cancelled.");
            return;
        }

        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == targetUserId);
        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("-> Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("-> Update cancelled.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus)) {
            System.out.println("-> User #" + targetUserId + " updated successfully: role = "
                    + newRole + ", status = " + newStatus);
        } else {
            System.out.println("-> Failed to update user.");
        }
    }

    private void displayUserTable(List<UserRecord> users, String title) {
        System.out.println("===================================" + title + "===================================");
        if (users.isEmpty()) {
            System.out.println("-> No Records Found");
            return;
        }
        String border = "+-----+-----------------+---------------------------+------------+";
        System.out.println(border);
        System.out.printf("| %-3s | %-15s | %-25s | %-10s |\n", "ID", "Username", "Role", "Status");
        System.out.println(border);
        for (UserRecord u : users) {
            System.out.printf("| %-3s | %-15s | %-25s | %-10s |\n",
                    u.id(), u.username(), u.role(), u.status());
        }
        System.out.println(border);
    }

    private void viewAllUser() {
        displayUserTable(userDao.getAllUsers(), "ALL REGISTERED USER");
    }

    private void viewPendingUser() {
        displayUserTable(userDao.getPendingUser(), "PENDING APPROVALS");
    }

    private void addUser() {
        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();
        String role = readValidRole();
        if (role == null) {
            System.out.println("-> Add user cancelled.");
            return;
        }

        if (userDao.insert(username, password, role, "APPROVED")) {
            System.out.println("-> User added successfully.");
        } else {
            System.out.println("-> Failed to add user.");
        }
    }

    private void approveUser() {
        List<UserRecord> pendingUsers = userDao.getPendingUser();
        displayUserTable(pendingUsers, "PENDING APPROVALS");
        if (pendingUsers.isEmpty()) return;

        int userID = readIntInput("Enter User ID to Approve (or 0 to cancel): ");
        if (userID == 0) {
            System.out.println("-> Operation Cancelled by the User");
            return;
        }
        boolean exists = pendingUsers.stream().anyMatch(u -> u.id() == userID);
        if (!exists) {
            System.out.println("[ERROR] User ID not found.");
            return;
        }
        userDao.approveUser(userID);
        System.out.println("-> User approved successfully.");
    }
}