package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User.UserRecord;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
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
            System.out.print("Select: ");

            int select = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (select) {
                case 1 -> viewAllUser();
                case 2 -> viewPendingUser();
                case 3 -> approveUser();
                case 4 -> addUser();
                case 5 -> updateUserStatus();
                case 7 -> loggedIn = false;
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
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
        System.out.print("Enter Role [ADMIN/STAFF/RESIDENT]: ");
        String role = scanner.nextLine().trim().toUpperCase();

        boolean success = userDao.insert(username, password, role, "ACTIVE");
        if (success) {
            System.out.println("-> User added successfully.");
        } else {
            System.out.println("-> Failed to add user.");
        }
    }

    private void approveUser() {
        List<UserRecord> pendingUsers = userDao.getPendingUser();
        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()) {
            return;
        }
        System.out.print("Enter User ID to Approve (or 0 to cancel): ");
        int userID = scanner.nextInt();
        scanner.nextLine();
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

    private void updateUserStatus() {
        viewAllUser();
        System.out.print("Enter User ID to Update Status: ");
        int userID = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Enter New Status [ACTIVE/INACTIVE/PENDING]: ");
        String status = scanner.nextLine().trim().toUpperCase();

        if (userDao.updateStatus(userID, status)) {
            System.out.println("-> Status updated successfully.");
        } else {
            System.out.println("-> Failed to update status.");
        }
    }
}