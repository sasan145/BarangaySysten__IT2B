package barangay;

import barangay.dao.BarangayDao.DocumentTypeDao;
import barangay.dao.BarangayDao.UserDao;
import barangay.dao.DatabaseConnection;
import barangay.ui.BarangayMenu;

import java.util.Scanner;

/** Application entry point. */
public class Main {
    public static void main(String[] args) {
        DatabaseConnection.initialize();
        new UserDao().seedDefaultAdmin();
        new DocumentTypeDao().seedDefaults();

        try (Scanner scanner = new Scanner(System.in)) {
            BarangayMenu.run(scanner);
        }
    }
}