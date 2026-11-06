package com.scc.ui;

import com.scc.dao.DocumentRequestDao;
import com.scc.dao.DocumentTypeDao;
import com.scc.dao.ResidentDao;
import com.scc.model.User.DocumentRequest;
import com.scc.model.User.DocumentType;
import com.scc.model.User.Resident;
import com.scc.model.User.UserRecord;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
            while (true) {
                System.out.print(prompt);
                String val = scanner.nextLine().trim();
                try {
                    if (!LocalDate.parse(val).isAfter(LocalDate.now())) return val;
                    System.out.println("Date cannot be in the future.");
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date. Use YYYY-MM-DD.");
                }
            }
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

        public static void summary() {
            System.out.println("\n--- SUMMARY REPORT ---");
            System.out.println("System operational and running smoothly.");
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
            if (name.isBlank()) {
                System.out.println("Name is required.");
                return;
            }
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
            if (name.isBlank()) {
                System.out.println("Name is required.");
                return;
            }
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
            System.out.println(recorded ? "Request recorded (PENDING)." : "Failed to record request (check the document type ID).");
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
            boolean canceled = requestDao.cancel(id);
            System.out.println(canceled ? "Request canceled." : "Request not found or can no longer be canceled.");
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
            if (name.isBlank()) {
                System.out.println("Name is required.");
                return;
            }

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
            System.out.println(submitted ? "Request submitted (PENDING)." : "Failed to submit request (check the document type ID).");
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
}