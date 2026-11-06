package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private static final List<String> VALID_ROLES = List.of("ADMIN", "STAFF", "RESIDENT");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    private final UserDao userDao;
    private final Scanner scanner;
    private final ActivityLogDao logDao = new ActivityLogDao();

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void display(User.UserRecord admin) {
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
            System.out.println("6. View System Audit Logs");
            System.out.println("7. Logout");

            int select = readIntInput("Select: ");

            switch (select) {
                case 1 -> viewAllUser();
                case 2 -> viewPendingUser();
                case 3 -> approveUser(admin);
                case 4 -> addUser(admin);
                case 5 -> updateUserRoleAndStatus(admin);
                case 6 -> auditLogMenu();
                case 7 -> loggedIn = false;
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

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

    private String readValidRole() {
        while (true) {
            System.out.print("Enter Role " + VALID_ROLES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (VALID_ROLES.contains(input)) return input;
            System.out.println("-> [ERROR] Invalid role. Valid roles: " + VALID_ROLES);
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.print("Enter New Status " + VALID_STATUSES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (VALID_STATUSES.contains(input)) return input;
            System.out.println("-> [ERROR] Invalid status. Valid statuses: " + VALID_STATUSES);
        }
    }

    private void updateUserRoleAndStatus(User.UserRecord currentAdmin) {
        List<User.UserRecord> users = userDao.getAllUsers();
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
            logDao.logAction(currentAdmin.id(), "USER_UPDATED",
                    "Admin [" + currentAdmin.id() + "] changed User " + targetUserId
                            + " to role " + newRole + ", status " + newStatus);
            System.out.println("-> User updated successfully.");
        } else {
            System.out.println("-> Failed to update user.");
        }
    }

    private void displayUserTable(List<User.UserRecord> users, String title) {
        System.out.println("===================================" + title + "===================================");
        if (users.isEmpty()) {
            System.out.println("-> No Records Found");
            return;
        }
        String border = "+-----+-----------------+---------------------------+------------+";
        System.out.println(border);
        System.out.printf("| %-3s | %-15s | %-25s | %-10s |\n", "ID", "Username", "Role", "Status");
        System.out.println(border);
        for (User.UserRecord u : users) {
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

    private void addUser(User.UserRecord admin) {
        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        if (username.isBlank() || password.isBlank()) {
            System.out.println("-> [ERROR] Username and password are required.");
            return;
        }

        String role = readValidRole();
        if (role == null) {
            System.out.println("-> Add user cancelled.");
            return;
        }

        if (userDao.insert(username, password, role, "APPROVED")) {
            logDao.logAction(admin.id(), "USER_ADDED",
                    "Admin [" + admin.id() + "] created user '" + username + "' with role " + role);
            System.out.println("-> User added successfully.");
        } else {
            System.out.println("-> Failed to add user.");
        }
    }

    private void approveUser(User.UserRecord admin) {
        List<User.UserRecord> pendingUsers = userDao.getPendingUser();
        displayUserTable(pendingUsers, "PENDING APPROVALS");
        if (pendingUsers.isEmpty()) return;

        int userID = readIntInput("Enter User ID to Approve (or 0 to cancel): ");
        if (userID == 0) {
            System.out.println("-> Operation Cancelled by the User");
            return;
        }
        boolean exists = pendingUsers.stream().anyMatch(u -> u.id() == userID);
        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        if (userDao.approveUser(userID)) {
            logDao.logAction(admin.id(), "USER_APPROVED",
                    "Admin [" + admin.id() + "] approved account for User ID " + userID);
            System.out.println("-> User approved successfully.");
        } else {
            System.out.println("-> Failed to approve user.");
        }
    }

    private void auditLogMenu() {
        System.out.println("\n--- SYSTEM AUDIT LOGS ---");
        List<ActivityLog> logs = logDao.getAllLogs();
        if (logs.isEmpty()) {
            System.out.println("-> No audit logs found.");
            return;
        }
        for (ActivityLog log : logs) {
            System.out.printf("[%s] User ID: %d | %s: %s\n",
                    log.timestamp(), log.userId(), log.actionType(), log.description());
        }
    }
}