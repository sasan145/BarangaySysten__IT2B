package com.scc.ui;

import com.scc.dao.*;
import com.scc.model.ActivityLog;
import com.scc.model.User;
import com.scc.model.User.DocumentType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class AdminMenu {
    private static final List<String> VALID_ROLES = List.of("ADMIN", "STAFF", "RESIDENT");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");
    private static final List<String> REQUEST_STATUSES =
            List.of("PENDING", "PROCESSING", "APPROVED", "RELEASED", "CANCELED");

    private final UserDao userDao;
    private final Scanner scanner;
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final DocumentTypeDao typeDao = new DocumentTypeDao();
    private final SettingsDao settingsDao = new SettingsDao();
    private final ReportDao reportDao = new ReportDao();

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    // =====================================================================
    // MAIN MENU
    // =====================================================================

    public void display(User.UserRecord admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println();
            System.out.println("=====================================");
            System.out.println("========== ADMIN DASHBOARD ==========");
            System.out.println("=====================================");
            System.out.println("--- User Accounts & Access ---");
            System.out.println(" 1. View all users");
            System.out.println(" 2. View pending user approvals");
            System.out.println(" 3. Approve user registration");
            System.out.println(" 4. Add user");
            System.out.println(" 5. Update user role / status");
            System.out.println(" 6. Delete user");
            System.out.println("--- System ---");
            System.out.println(" 7. Manage document types");
            System.out.println(" 8. System settings");
            System.out.println(" 9. Summary report");
            System.out.println("10. System activity logs");
            System.out.println(" 0. Logout");

            int select = readIntInput("Select: ");

            switch (select) {
                case 1 -> viewAllUser();
                case 2 -> viewPendingUser();
                case 3 -> approveUser(admin);
                case 4 -> addUser(admin);
                case 5 -> updateUserRoleAndStatus(admin);
                case 6 -> deleteUser(admin);
                case 7 -> manageDocumentTypes(admin);
                case 8 -> systemSettings(admin);
                case 9 -> summaryReport();
                case 10 -> auditLogMenu();
                case 0 -> loggedIn = false;
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    // =====================================================================
    // INPUT HELPERS
    // =====================================================================

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readIntInput(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid input. Please enter a number.");
            }
        }
    }

    /** Reads a fee. If keep is not null, a blank answer keeps that value. */
    private double readFee(String prompt, Double keep) {
        while (true) {
            String input = readLine(prompt);
            if (input.isBlank() && keep != null) return keep;
            try {
                double fee = Double.parseDouble(input);
                if (Double.isFinite(fee) && fee >= 0) return fee;
                System.out.println("-> [ERROR] Fee must be zero or more.");
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid amount.");
            }
        }
    }

    private boolean confirm(String prompt) {
        return readLine(prompt + " (y/N): ").equalsIgnoreCase("y");
    }

    private String readValidRole() {
        while (true) {
            String input = readLine("Enter Role " + VALID_ROLES + " (0 to cancel): ").toUpperCase();
            if (input.equals("0")) return null;
            if (VALID_ROLES.contains(input)) return input;
            System.out.println("-> [ERROR] Invalid role. Valid roles: " + VALID_ROLES);
        }
    }

    private String readValidStatus() {
        while (true) {
            String input = readLine("Enter New Status " + VALID_STATUSES + " (0 to cancel): ").toUpperCase();
            if (input.equals("0")) return null;
            if (VALID_STATUSES.contains(input)) return input;
            System.out.println("-> [ERROR] Invalid status. Valid statuses: " + VALID_STATUSES);
        }
    }

    // =====================================================================
    // USER MANAGEMENT
    // =====================================================================

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
        String username = readLine("Enter Username: ");
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
            System.out.println("-> Failed to add user (username may already exist).");
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

    private void deleteUser(User.UserRecord currentAdmin) {
        List<User.UserRecord> users = userDao.getAllUsers();
        displayUserTable(users, "SELECT USER TO DELETE");
        if (users.isEmpty()) return;

        int targetUserId = readIntInput("Enter User ID to delete (0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("-> Delete cancelled.");
            return;
        }

        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: You cannot delete the account you are logged in with.");
            return;
        }

        Optional<User.UserRecord> target = users.stream().filter(u -> u.id() == targetUserId).findFirst();
        if (target.isEmpty()) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        User.UserRecord t = target.get();
        if (!confirm("Delete user '" + t.username() + "' (" + t.role() + ")? This cannot be undone")) {
            System.out.println("-> Delete cancelled.");
            return;
        }

        if (userDao.deleteUser(targetUserId)) {
            logDao.logAction(currentAdmin.id(), "USER_DELETED",
                    "Admin [" + currentAdmin.id() + "] deleted user '" + t.username()
                            + "' (ID " + targetUserId + ", role " + t.role() + ")");
            System.out.println("-> User deleted successfully.");
        } else {
            System.out.println("-> Failed to delete user.");
        }
    }

    // =====================================================================
    // DOCUMENT TYPES
    // =====================================================================

    private void manageDocumentTypes(User.UserRecord admin) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MANAGE DOCUMENT TYPES ---");
            System.out.println("1. View all document types");
            System.out.println("2. Add document type");
            System.out.println("3. Update document type");
            System.out.println("4. Activate / Deactivate document type");
            System.out.println("5. Delete document type");
            System.out.println("0. Back");

            switch (readIntInput("Select: ")) {
                case 1 -> BarangayMenu.Display.documentTypes(typeDao.findAll());
                case 2 -> addDocumentType(admin);
                case 3 -> updateDocumentType(admin);
                case 4 -> toggleDocumentType(admin);
                case 5 -> deleteDocumentType(admin);
                case 0 -> back = true;
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    private Optional<DocumentType> pickDocumentType(String action) {
        List<DocumentType> types = typeDao.findAll();
        BarangayMenu.Display.documentTypes(types);
        if (types.isEmpty()) return Optional.empty();

        int id = readIntInput("Enter Document Type ID to " + action + " (0 to cancel): ");
        if (id == 0) {
            System.out.println("-> Cancelled.");
            return Optional.empty();
        }
        Optional<DocumentType> found = types.stream().filter(t -> t.id() == id).findFirst();
        if (found.isEmpty()) System.out.println("-> [ERROR] Document type ID not found.");
        return found;
    }

    private void addDocumentType(User.UserRecord admin) {
        String name = readLine("Document name (blank to cancel): ");
        if (name.isBlank()) {
            System.out.println("-> Add cancelled.");
            return;
        }
        String description = readLine("Description: ");
        double fee = readFee("Fee: ", null);

        if (typeDao.insert(name, description, fee)) {
            logDao.logAction(admin.id(), "DOCTYPE_ADDED",
                    "Admin [" + admin.id() + "] added document type '" + name
                            + "' (fee " + String.format("%.2f", fee) + ")");
            System.out.println("-> Document type added.");
        } else {
            System.out.println("-> Failed to add document type (name may already exist).");
        }
    }

    private void updateDocumentType(User.UserRecord admin) {
        Optional<DocumentType> found = pickDocumentType("update");
        if (found.isEmpty()) return;
        DocumentType cur = found.get();

        String curDesc = cur.description() == null ? "" : cur.description();

        String name = readLine("New name [" + cur.name() + "] (blank = keep): ");
        if (name.isBlank()) name = cur.name();
        String description = readLine("New description [" + curDesc + "] (blank = keep): ");
        if (description.isBlank()) description = curDesc;
        double fee = readFee("New fee [" + String.format("%.2f", cur.fee()) + "] (blank = keep): ", cur.fee());

        if (typeDao.update(cur.id(), name, description, fee)) {
            logDao.logAction(admin.id(), "DOCTYPE_UPDATED",
                    "Admin [" + admin.id() + "] updated document type ID " + cur.id()
                            + ": '" + name + "', fee " + String.format("%.2f", fee));
            System.out.println("-> Document type updated.");
        } else {
            System.out.println("-> Failed to update document type (name may already exist).");
        }
    }

    private void toggleDocumentType(User.UserRecord admin) {
        Optional<DocumentType> found = pickDocumentType("activate/deactivate");
        if (found.isEmpty()) return;
        DocumentType cur = found.get();

        String newStatus = "ACTIVE".equals(cur.status()) ? "INACTIVE" : "ACTIVE";
        if (typeDao.setStatus(cur.id(), newStatus)) {
            logDao.logAction(admin.id(), "DOCTYPE_STATUS",
                    "Admin [" + admin.id() + "] set document type '" + cur.name() + "' to " + newStatus);
            System.out.println("-> '" + cur.name() + "' is now " + newStatus + ".");
        } else {
            System.out.println("-> Failed to change status.");
        }
    }

    private void deleteDocumentType(User.UserRecord admin) {
        Optional<DocumentType> found = pickDocumentType("delete");
        if (found.isEmpty()) return;
        DocumentType cur = found.get();

        if (typeDao.isUsed(cur.id())) {
            System.out.println("-> This document type already has requests, so it cannot be deleted.");
            System.out.println("   Use option 4 to deactivate it instead.");
            return;
        }
        if (!confirm("Delete document type '" + cur.name() + "'? This cannot be undone")) {
            System.out.println("-> Delete cancelled.");
            return;
        }

        if (typeDao.delete(cur.id())) {
            logDao.logAction(admin.id(), "DOCTYPE_DELETED",
                    "Admin [" + admin.id() + "] deleted document type '" + cur.name() + "'");
            System.out.println("-> Document type deleted.");
        } else {
            System.out.println("-> Failed to delete document type.");
        }
    }

    // =====================================================================
    // SYSTEM SETTINGS
    // =====================================================================

    private void systemSettings(User.UserRecord admin) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- SYSTEM SETTINGS ---");
            System.out.println("Barangay name       : " + settingsDao.get(SettingsDao.BARANGAY_NAME, "Barangay"));
            System.out.println("Public registration : " + (settingsDao.isRegistrationOpen() ? "ENABLED" : "DISABLED"));
            System.out.println("-----------------------");
            System.out.println("1. Change barangay name");
            System.out.println("2. Enable / Disable public registration");
            System.out.println("0. Back");

            switch (readIntInput("Select: ")) {
                case 1 -> changeBarangayName(admin);
                case 2 -> toggleRegistration(admin);
                case 0 -> back = true;
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    private void changeBarangayName(User.UserRecord admin) {
        String name = readLine("New barangay name (blank to cancel): ");
        if (name.isBlank()) {
            System.out.println("-> Cancelled.");
            return;
        }
        if (settingsDao.set(SettingsDao.BARANGAY_NAME, name)) {
            logDao.logAction(admin.id(), "SETTING_CHANGED",
                    "Admin [" + admin.id() + "] set barangay name to '" + name + "'");
            System.out.println("-> Barangay name updated.");
        } else {
            System.out.println("-> Failed to update setting.");
        }
    }

    private void toggleRegistration(User.UserRecord admin) {
        boolean newValue = !settingsDao.isRegistrationOpen();
        if (settingsDao.set(SettingsDao.ALLOW_REGISTRATION, String.valueOf(newValue))) {
            logDao.logAction(admin.id(), "SETTING_CHANGED",
                    "Admin [" + admin.id() + "] " + (newValue ? "enabled" : "disabled") + " public registration");
            System.out.println("-> Public registration is now " + (newValue ? "ENABLED." : "DISABLED."));
        } else {
            System.out.println("-> Failed to update setting.");
        }
    }

    // =====================================================================
    // SUMMARY REPORT
    // =====================================================================

    private String countLine(List<String> keys, Map<String, Integer> counts) {
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(k).append(": ").append(counts.getOrDefault(k, 0));
        }
        return sb.toString();
    }

    private void summaryReport() {
        System.out.println("\n=========== SUMMARY REPORT ===========");
        System.out.println(settingsDao.get(SettingsDao.BARANGAY_NAME, "Barangay"));

        System.out.println("\nUSERS (" + reportDao.totalUsers() + " total)");
        System.out.println("  By role   -> " + countLine(VALID_ROLES, reportDao.usersByRole()));
        System.out.println("  By status -> " + countLine(VALID_STATUSES, reportDao.usersByStatus()));

        System.out.println("\nRESIDENTS");
        System.out.println("  Registered residents: " + reportDao.totalResidents());

        System.out.println("\nDOCUMENT TYPES");
        System.out.println("  Active: " + reportDao.activeDocumentTypes()
                + " of " + reportDao.totalDocumentTypes());

        System.out.println("\nDOCUMENT REQUESTS (" + reportDao.totalRequests() + " total)");
        Map<String, Integer> byStatus = reportDao.requestsByStatus();
        for (String s : REQUEST_STATUSES) {
            System.out.printf("  %-12s %d%n", s, byStatus.getOrDefault(s, 0));
        }

        System.out.println("\nFEES");
        System.out.printf("  Collected (RELEASED)          : PHP %.2f%n", reportDao.feesCollected());
        System.out.printf("  In progress (not yet released): PHP %.2f%n", reportDao.feesPending());
        System.out.println("======================================");
    }

    // =====================================================================
    // ACTIVITY LOGS
    // =====================================================================

    private void auditLogMenu() {
        System.out.println("\n--- SYSTEM ACTIVITY LOGS ---");
        String filter = readLine("Filter by User ID (blank = all): ");

        List<ActivityLog> logs;
        if (filter.isBlank()) {
            logs = logDao.getAllLogs();
        } else {
            try {
                logs = logDao.getLogsByUserId(Integer.parseInt(filter));
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid User ID.");
                return;
            }
        }

        if (logs.isEmpty()) {
            System.out.println("-> No activity logs found.");
            return;
        }
        for (ActivityLog log : logs) {
            System.out.printf("[%s] User ID: %d | %s: %s\n",
                    log.timestamp(), log.userId(), log.actionType(), log.description());
        }
        System.out.println(logs.size() + " log(s)");
    }
}