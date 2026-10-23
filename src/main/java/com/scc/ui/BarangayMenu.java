package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.dao.UserDao.ActivityLogDao;
import com.scc.dao.UserDao.DocumentRequestDao;
import com.scc.dao.UserDao.DocumentTypeDao;
import com.scc.dao.UserDao.ResidentDao;
import com.scc.model.User.DocumentRequest;
import com.scc.model.User.DocumentType;
import com.scc.model.User.Resident;
import com.scc.model.User.UserRecord;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public final class BarangayMenu {
    private BarangayMenu() {}

    public static class Input {
        private final Scanner scanner;

        public Input(Scanner scanner) {
            this.scanner = scanner;
        }

        public String text(String prompt) {
            System.out.print(prompt);
            return scanner.nextLine().trim();
        }

        public int integer(String prompt) {
            while (true) {
                try {
                    System.out.print(prompt);
                    String val = scanner.nextLine().trim();
                    return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid integer. Please try again.");
                }
            }
        }

        public double decimal(String prompt) {
            while (true) {
                try {
                    System.out.print(prompt);
                    String val = scanner.nextLine().trim();
                    return Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number. Please try again.");
                }
            }
        }

        public String date(String prompt) {
            System.out.print(prompt);
            return scanner.nextLine().trim();
        }
    }

    public static class Display {

        private static String cut(String s, int max) {
            if (s == null) return "";
            return s.length() <= max ? s : s.substring(0, max - 3) + "...";
        }

        private static void line(int width) {
            System.out.println("-".repeat(width));
        }

        public static void users(List<UserRecord> users) {
            System.out.println("\n--- USERS ---");
            String fmt = "%-5s %-20s %-10s %-10s%n";
            line(48);
            System.out.printf(fmt, "ID", "USERNAME", "ROLE", "STATUS");
            line(48);
            for (UserRecord u : users) {
                System.out.printf(fmt, u.id(), cut(u.username(), 20), u.role(), u.status());
            }
            line(48);
            System.out.println(users.size() + " record(s)");
        }

        public static void residents(List<Resident> residents) {
            System.out.println("\n--- RESIDENTS ---");
            String fmt = "%-5s %-25s %-12s %-30s %-15s%n";
            line(90);
            System.out.printf(fmt, "ID", "NAME", "BIRTH DATE", "ADDRESS", "CONTACT");
            line(90);
            for (Resident r : residents) {
                System.out.printf(fmt, r.id(), cut(r.name(), 25), r.birthDate(), cut(r.address(), 30), r.contact());
            }
            line(90);
            System.out.println(residents.size() + " record(s)");
        }

        public static void documentTypes(List<DocumentType> types) {
            System.out.println("\n--- DOCUMENT TYPES ---");
            String fmt = "%-5s %-28s %-32s %10s  %-10s%n";
            line(90);
            System.out.printf(fmt, "ID", "NAME", "DESCRIPTION", "FEE", "STATUS");
            line(90);
            for (DocumentType dt : types) {
                System.out.printf(fmt, dt.id(), cut(dt.name(), 28), cut(dt.description(), 32),
                        String.format("%.2f", dt.fee()), dt.status());
            }
            line(90);
            System.out.println(types.size() + " record(s)");
        }

        public static void requests(List<DocumentRequest> requests) {
            System.out.println("\n--- DOCUMENT REQUESTS ---");
            String fmt = "%-5s %-25s %-28s %10s  %-12s%n";
            line(86);
            System.out.printf(fmt, "ID", "RESIDENT", "DOCUMENT", "FEE", "STATUS");
            line(86);
            for (DocumentRequest dr : requests) {
                System.out.printf(fmt, dr.id(), cut(dr.residentName(), 25), cut(dr.documentName(), 28),
                        String.format("%.2f", dr.fee()), dr.status());
            }
            line(86);
            System.out.println(requests.size() + " record(s)");
        }

        public static void logs(List<String> logs) {
            System.out.println("\n--- ACTIVITY LOGS ---");
            for (String log : logs) {
                System.out.println(log);
            }
        }

        public static void summary() {
            System.out.println("\n--- SUMMARY REPORT ---");
            System.out.println("System operational and running smoothly.");
        }
    }

    public static class MainMenu {
        private final Input in;
        private final UserDao userDao = new UserDao();
        private final ActivityLogDao logDao = new ActivityLogDao();

        public MainMenu(Input in) {
            this.in = in;
        }

        public void show() {
            boolean running = true;
            while (running) {
                System.out.println("\n=== BARANGAY DOCUMENT REQUEST SYSTEM ===");
                System.out.println("1. Login");
                System.out.println("2. Register as resident");
                System.out.println("0. Exit");

                String choice = in.text("Choose: ");
                if ("1".equals(choice)) {
                    login();
                } else if ("2".equals(choice)) {
                    register();
                } else if ("0".equals(choice)) {
                    running = false;
                } else {
                    System.out.println("Invalid choice.");
                }
            }
            System.out.println("Goodbye!");
        }

        private void register() {
            String username = in.text("Choose a username: ");
            String password = in.text("Choose a password: ");
            if (username.isBlank() || password.isBlank()) {
                System.out.println("Username and password are required.");
                return;
            }
            boolean ok = userDao.insert(username, password, "RESIDENT", "PENDING");
            if (ok) {
                logDao.log("New resident registered: " + username + " (PENDING)");
                System.out.println("Registered! Wait for the admin to approve your account.");
            } else {
                System.out.println("Username already taken.");
            }
        }

        private void login() {
            String username = in.text("Username: ");
            String password = in.text("Password: ");

            UserRecord user = userDao.login(username, password);
            if (user == null) {
                System.out.println("Invalid credentials or account non-active.");
                return;
            }

            if (!"ACTIVE".equals(user.status())) {
                System.out.println("Your account is inactive or not yet approved.");
                return;
            }

            logDao.log(user.role() + " logged in: " + user.username());

            if ("ADMIN".equals(user.role())) {
                new AdminMenu(in, user).show();
            } else if ("STAFF".equals(user.role())) {
                new StaffMenu(in, user).show();
            } else {
                new ResidentMenu(in, user).show();
            }
        }
    }

    public static class AdminMenu {
        private final Input in;
        private final UserRecord admin;

        private final UserDao userDao = new UserDao();
        private final DocumentTypeDao typeDao = new DocumentTypeDao();
        private final ActivityLogDao logDao = new ActivityLogDao();

        public AdminMenu(Input in, UserRecord admin) {
            this.in = in;
            this.admin = admin;
        }

        public void show() {
            System.out.println("\nWelcome, " + admin.username() + "! (Administrator)");
            boolean running = true;

            while (running) {
                System.out.println("\n--- ADMINISTRATOR MENU ---");
                System.out.println("1. View all users");
                System.out.println("2. Approve user");
                System.out.println("3. Deactivate user");
                System.out.println("4. View document types");
                System.out.println("5. Add document type");
                System.out.println("6. Update document type");
                System.out.println("7. Set document type active/inactive");
                System.out.println("8. Summary report");
                System.out.println("9. Activity logs");
                System.out.println("10. Create staff account");
                System.out.println("0. Logout");

                switch (in.text("Choose: ")) {
                    case "1" -> Display.users(userDao.findAll());
                    case "2" -> setUserStatus("ACTIVE");
                    case "3" -> setUserStatus("INACTIVE");
                    case "4" -> Display.documentTypes(typeDao.findAll());
                    case "5" -> addType();
                    case "6" -> updateType();
                    case "7" -> setTypeStatus();
                    case "8" -> Display.summary();
                    case "9" -> Display.logs(logDao.findRecent(50));
                    case "10" -> createStaff();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            }
            System.out.println("Administrator logged out.");
        }

        private void createStaff() {
            String username = in.text("Staff username: ");
            String password = in.text("Staff password: ");
            if (username.isBlank() || password.isBlank()) {
                System.out.println("Username and password are required.");
                return;
            }
            boolean ok = userDao.insert(username, password, "STAFF", "ACTIVE");
            if (ok) logDao.log(admin.username() + " created staff account: " + username);
            System.out.println(ok ? "Staff account created." : "Username already taken.");
        }

        private void setUserStatus(String status) {
            Display.users(userDao.findAll());
            int id = in.integer("User ID: ");
            if (id == admin.id() && status.equals("INACTIVE")) {
                System.out.println("You cannot deactivate yourself.");
                return;
            }
            boolean updated = userDao.updateStatus(id, status);
            if (updated) logDao.log(admin.username() + " set user #" + id + " to " + status);
            System.out.println(updated ? "User is now " + status + "." : "User not found.");
        }

        private void addType() {
            String name = in.text("Document name: ");
            String description = in.text("Description: ");
            double fee = in.decimal("Processing fee: ");
            if (name.isBlank()) {
                System.out.println("Document name is required.");
                return;
            }
            boolean added = typeDao.insert(name, description, fee);
            if (added) logDao.log(admin.username() + " added document type: " + name);
            System.out.println(added ? "Document type added." : "Failed to add document type (name may already exist).");
        }

        private void updateType() {
            Display.documentTypes(typeDao.findAll());
            int id = in.integer("Document type ID: ");
            String name = in.text("New name: ");
            String description = in.text("New description: ");
            double fee = in.decimal("New fee: ");
            boolean updated = typeDao.update(id, name, description, fee);
            if (updated) logDao.log(admin.username() + " updated document type #" + id);
            System.out.println(updated ? "Document type updated." : "Document type not found.");
        }

        private void setTypeStatus() {
            Display.documentTypes(typeDao.findAll());
            int id = in.integer("Document type ID: ");
            String choice = in.text("1 = ACTIVE, 2 = INACTIVE: ");
            String status = choice.equals("1") ? "ACTIVE" : choice.equals("2") ? "INACTIVE" : null;
            if (status == null) {
                System.out.println("Invalid choice.");
                return;
            }
            boolean updated = typeDao.setStatus(id, status);
            if (updated) logDao.log(admin.username() + " set document type #" + id + " to " + status);
            System.out.println(updated ? "Status set to " + status + "." : "Document type not found.");
        }
    }

    public static class StaffMenu {
        private static final List<String> STATUSES = List.of("PENDING", "PROCESSING", "APPROVED", "RELEASED");

        private final Input in;
        private final UserRecord staff;

        private final ResidentDao residentDao = new ResidentDao();
        private final DocumentTypeDao typeDao = new DocumentTypeDao();
        private final DocumentRequestDao requestDao = new DocumentRequestDao();

        public StaffMenu(Input in, UserRecord staff) {
            this.in = in;
            this.staff = staff;
        }

        public void show() {
            System.out.println("\nWelcome, " + staff.username() + "! (Barangay Staff)");
            boolean running = true;

            while (running) {
                System.out.println("\n--- BARANGAY STAFF MENU ---");
                System.out.println("1. Register resident");
                System.out.println("2. Search / view residents");
                System.out.println("3. Update resident");
                System.out.println("4. Delete resident");
                System.out.println("5. Record document request");
                System.out.println("6. View all requests");
                System.out.println("7. Update request status");
                System.out.println("8. Cancel request");
                System.out.println("9. Summary report");
                System.out.println("0. Logout");

                switch (in.text("Choose: ")) {
                    case "1" -> registerResident();
                    case "2" -> Display.residents(residentDao.search(in.text("Search (blank = all): ")));
                    case "3" -> updateResident();
                    case "4" -> deleteResident();
                    case "5" -> recordRequest();
                    case "6" -> Display.requests(requestDao.findAll());
                    case "7" -> updateStatus();
                    case "8" -> cancelRequest();
                    case "9" -> Display.summary();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            }
            System.out.println("Staff logged out.");
        }

        private void registerResident() {
            String name = in.text("Full name: ");
            String birth = in.date("Birth date (YYYY-MM-DD): ");
            String address = in.text("Address: ");
            String contact = in.text("Contact number: ");
            boolean registered = residentDao.insert(null, name, birth, address, contact);
            System.out.println(registered ? "Resident registered." : "Failed to register resident.");
        }

        private void updateResident() {
            Display.residents(residentDao.search(""));
            int id = in.integer("Resident ID: ");
            String name = in.text("New full name: ");
            String birth = in.date("New birth date (YYYY-MM-DD): ");
            String address = in.text("New address: ");
            String contact = in.text("New contact number: ");
            boolean updated = residentDao.update(id, name, birth, address, contact);
            System.out.println(updated ? "Resident updated." : "Resident not found.");
        }

        private void deleteResident() {
            Display.residents(residentDao.search(""));
            int id = in.integer("Resident ID to delete: ");
            boolean deleted = residentDao.delete(id);
            System.out.println(deleted ? "Resident deleted." : "Resident not found.");
        }

        private void recordRequest() {
            Display.residents(residentDao.search(""));
            int residentId = in.integer("Resident ID: ");
            if (residentDao.findById(residentId).isEmpty()) {
                System.out.println("Resident not found.");
                return;
            }
            Display.documentTypes(typeDao.findActive());
            int typeId = in.integer("Document type ID: ");
            boolean recorded = requestDao.insert(residentId, typeId);
            System.out.println(recorded ? "Request recorded (PENDING)." : "Failed to record request.");
        }

        private void updateStatus() {
            Display.requests(requestDao.findAll());
            int id = in.integer("Request ID: ");
            System.out.println("Available statuses: " + STATUSES);
            String status = in.text("New status: ").toUpperCase();
            if (!STATUSES.contains(status)) {
                System.out.println("Invalid status.");
                return;
            }
            boolean updated = requestDao.updateStatus(id, status);
            System.out.println(updated ? "Status updated to " + status + "." : "Request not found.");
        }

        private void cancelRequest() {
            Display.requests(requestDao.findAll());
            int id = in.integer("Request ID to cancel: ");
            boolean canceled = requestDao.updateStatus(id, "CANCELED");
            System.out.println(canceled ? "Request canceled." : "Request not found.");
        }
    }

    public static class ResidentMenu {
        private final Input in;
        private final UserRecord user;

        private final ResidentDao residentDao = new ResidentDao();
        private final DocumentTypeDao typeDao = new DocumentTypeDao();
        private final DocumentRequestDao requestDao = new DocumentRequestDao();

        public ResidentMenu(Input in, UserRecord user) {
            this.in = in;
            this.user = user;
        }

        public void show() {
            System.out.println("\nWelcome, " + user.username() + "! (Barangay Resident)");
            boolean running = true;

            while (running) {
                System.out.println("\n--- BARANGAY RESIDENT MENU ---");
                System.out.println("1. My profile");
                System.out.println("2. View available documents");
                System.out.println("3. Submit a request");
                System.out.println("4. Track my requests / history");
                System.out.println("0. Logout");

                switch (in.text("Choose: ")) {
                    case "1" -> profile();
                    case "2" -> Display.documentTypes(typeDao.findActive());
                    case "3" -> submitRequest();
                    case "4" -> trackRequests();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            }
            System.out.println("Resident logged out.");
        }

        private void profile() {
            Optional<Resident> found = residentDao.findByUserId(user.id());
            if (found.isPresent()) {
                Display.residents(List.of(found.get()));
                return;
            }

            System.out.println("No profile found. Please enter your details.");
            String name = in.text("Full name: ");
            String birth = in.date("Birth date (YYYY-MM-DD): ");
            String address = in.text("Address: ");
            String contact = in.text("Contact number: ");

            boolean created = residentDao.insert(user.id(), name, birth, address, contact);
            System.out.println(created ? "Profile created successfully." : "Failed to create profile.");
        }

        private void submitRequest() {
            Optional<Resident> found = residentDao.findByUserId(user.id());
            if (found.isEmpty()) {
                System.out.println("Create your profile first using option 1.");
                return;
            }

            Display.documentTypes(typeDao.findActive());
            int typeId = in.integer("Document type ID: ");
            boolean submitted = requestDao.insert(found.get().id(), typeId);
            System.out.println(submitted ? "Request submitted (PENDING)." : "Failed to submit request.");
        }

        private void trackRequests() {
            Optional<Resident> found = residentDao.findByUserId(user.id());
            if (found.isEmpty()) {
                System.out.println("Create your profile first using option 1.");
                return;
            }
            Display.requests(requestDao.findByResident(found.get().id()));
        }
    }

    public static void run(Scanner scanner) {
        new MainMenu(new Input(scanner)).show();
    }
}