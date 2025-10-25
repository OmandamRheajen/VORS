package main;

import java.util.Scanner;
import config.config; 
import java.util.List;
import java.util.Map;

public class Main {
    
    private static final Scanner sc = new Scanner(System.in);
    private static final config conf = new config();
    
    public static void main(String[] args) {
        
        Main app = new Main(); 
        String resp = "yes";
        
        do{
            System.out.println("\n===== VEHICLE AND OWNERS REGISTRATION SYSTEM =====");
            System.out.println("1. LOGIN");
            System.out.println("2. REGISTER");
            System.out.println("3. EXIT");

            System.out.print("Enter Action: ");
            
            if (!sc.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
                continue;
            }
            
            int action = sc.nextInt();
            sc.nextLine();
            
            switch(action){
                case 1:
                    app.loginUser();
                    break;
                case 2:
                    app.registerUser();
                    break;
                case 3:
                    System.out.println("Exiting application.");
                    resp = "no"; 
                    continue; 
                default:
                    System.out.println("Invalid option.");
                    break;
            }
            
            if (action != 3) {
                System.out.print("Continue? (yes/no): ");
                resp = sc.nextLine();
            } else {
                break;
            }

        }while(resp.equalsIgnoreCase("yes"));
        
        System.out.println("Thank You!");
        sc.close();
    }
    
    private long countAdmins() {
        String qry = "SELECT COUNT(U_id) FROM tbl_users WHERE U_role = 'Admin'";
        return (long) conf.getSingleValue(qry);
    }
    
    public void registerUser() {
        System.out.println("\n--- USER REGISTRATION ---");
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();
        
        String role;
        String approvalStatus;
        
        if (countAdmins() == 0) {
            role = "Admin";
            approvalStatus = "Approved"; 
            System.out.println("\nSTATUS: First registration detected. User set as Admin.");
        } else {
            role = "User";
            approvalStatus = "Pending"; 
            System.out.println("\nSTATUS: Admin already exists. User set as User (Active).");
            System.out.println("NOTE: You must be promoted by an Admin to gain Admin privileges.");
        }

        String hashedPassword = config.hashPassword(password);
        if (hashedPassword == null) {
            System.out.println("Registration failed due to hashing error.");
            return;
        }

        String sql = "INSERT INTO tbl_users (U_username, U_password, U_role, U_approval_status) VALUES (?, ?, ?, ?)";
        conf.addRecord(sql, username, hashedPassword, role, approvalStatus);
    }

    public void loginUser() {
        System.out.println("\n--- USER LOGIN ---");
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();

        String hashedPassword = config.hashPassword(password);
        if (hashedPassword == null) {
            System.out.println("Login failed due to hashing error.");
            return;
        }
        
        String qry = "SELECT U_role FROM tbl_users WHERE U_username = ? AND U_password = ?";
        List<Map<String, Object>> records = conf.fetchRecords(qry, username, hashedPassword);
        
        if (records.isEmpty()) {
            System.out.println("Invalid Credentials. Please try again.");
            return;
        }
        
        String role = records.get(0).get("U_role").toString();

        System.out.println("Login Successful as " + role + ".");

        if (role.equalsIgnoreCase("Admin")) {
            adminDashboard();
        } else {
            userDashboard();
        }
    }

    private void adminDashboard() {
        String resp = "yes";
        
        do{
            System.out.println("\n===== ADMIN DASHBOARD =====");
            System.out.println("1. ADD Owner & Vehicle");
            System.out.println("2. VIEW All Records");
            System.out.println("3. UPDATE Records");
            System.out.println("4. DELETE Records");
            System.out.println("5. PROMOTE User to Admin");
            System.out.println("6. LOGOUT");

            System.out.print("Enter Action: ");
            
            if (!sc.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
                continue;
            }
            
            int action = sc.nextInt();
            sc.nextLine();
            
            switch(action){
                case 1:
                    addOwnerAndVehicle();
                    break;
                case 2:
                    viewRecordsMenu(false); 
                    break;
                case 3:
                    updateRecordsMenu();
                    break;
                case 4:
                    deleteRecordsMenu();
                    break;
                case 5:
                    promoteUserToAdmin();
                    break;
                case 6:
                    System.out.println("Logging out...");
                    resp = "no"; 
                    continue; 
                default:
                    System.out.println("Invalid option.");
                    break;
            }
            
            if (action != 6) {
                System.out.print("Continue in Admin Dashboard? (yes/no): ");
                resp = sc.nextLine();
            } else {
                break;
            }

        }while(resp.equalsIgnoreCase("yes"));
    }

    private void promoteUserToAdmin() {
        System.out.println("\n--- PROMOTE USER TO ADMIN ---");
        
        String qry = "SELECT U_id, U_username, U_role, U_approval_status FROM tbl_users WHERE U_role = 'User' AND U_approval_status = 'Pending'";
        String[] hdrs = {"ID", "Username", "Role", "Status"}; // Display Headers
        String[] keys = {"U_id", "U_username", "U_role", "U_approval_status"}; // Map Keys
        List<Map<String, Object>> pendingRecords = conf.fetchRecords(qry);
        
        if (pendingRecords.isEmpty()) {
            System.out.println("No Users are currently pending promotion to Admin.");
            return;
        }

        System.out.println("Users Pending Admin Promotion:");
        config.displayRecords(pendingRecords, hdrs, keys); // Call the static method
        
        System.out.print("Enter User ID to PROMOTE (or 0 to cancel): ");
        if (!sc.hasNextInt()) { System.out.println("Invalid input. Operation cancelled."); sc.nextLine(); return; }
        int idToPromote = sc.nextInt(); sc.nextLine();
        
        if (idToPromote == 0) {
            System.out.println("Promotion cancelled.");
            return;
        }
        
        String updateQry = "UPDATE tbl_users SET U_role = 'Admin', U_approval_status = 'Approved' WHERE U_id = ? AND U_role = 'User' AND U_approval_status = 'Pending'";
        conf.updateRecord(updateQry, idToPromote);
        System.out.println("\nUser ID " + idToPromote + " has been promoted to Admin.");
    }

    private void userDashboard() {
        String resp = "yes";
        
        do{
            System.out.println("\n===== USER DASHBOARD =====");
            System.out.println("1. ADD Owner & Vehicle");
            System.out.println("2. VIEW Records");
            System.out.println("3. UPDATE Records");
            System.out.println("4. DELETE Records");
            System.out.println("5. LOGOUT");

            System.out.print("Enter Action: ");
            
            if (!sc.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
                continue;
            }
            
            int action = sc.nextInt();
            sc.nextLine();
            
            switch(action){
                case 1:
                    addOwnerAndVehicle();
                    break;
                case 2:
                    viewRecordsMenu(false); 
                    break;
                case 3:
                    updateRecordsMenu();
                    break;
                case 4:
                    deleteRecordsMenu();
                    break;
                case 5:
                    System.out.println("Logging out...");
                    resp = "no"; 
                    continue; 
                default:
                    System.out.println("Invalid option.");
                    break;
            }
            
            if (action != 5) {
                System.out.print("Continue in User Dashboard? (yes/no): ");
                resp = sc.nextLine();
            } else {
                break;
            }

        }while(resp.equalsIgnoreCase("yes"));
    }
    
    public void addOwnerAndVehicle(){
        System.out.println("\n--- ADD OWNER ---");
        System.out.print("Owner Name: ");
        String oname = sc.nextLine(); 
        System.out.print("License: ");
        String license = sc.nextLine();
        System.out.print("Contact: ");
        String contact = sc.nextLine();

        String ownerSql = "INSERT INTO tbl_owners (O_name, O_license, O_contact, O_status) VALUES (?, ?, ?, 'Active')";
        int ownerId = conf.addRecordAndReturnId(ownerSql, oname, license, contact);
        
        if (ownerId > 0) {
            System.out.println("Owner added successfully. ID: " + ownerId);
            System.out.println("\n--- ADD VEHICLE ---");

            System.out.print("Plate Number: ");
            String plate = sc.nextLine(); 
            System.out.print("Brand: ");
            String brand = sc.nextLine();
            System.out.print("Model: ");
            String model = sc.nextLine();
            
            String vehicleSql = "INSERT INTO tbl_vehicles (V_plate, V_brand, V_model, O_id) VALUES (?, ?, ?, ?)";
            conf.addRecord(vehicleSql, plate, brand, model, ownerId);
        } else {
            System.out.println("Owner record failed to insert. Cannot add vehicle.");
        }
    }
    
    private void viewRecordsMenu(boolean unused) {
        String title = "VIEW RECORDS";
        
        System.out.println("\n--- " + title + " ---");
        System.out.println("1. Owners");
        System.out.println("2. Vehicles (Full Details)");
        System.out.print("Enter choice (1 or 2): ");
        
        if (!sc.hasNextInt()) { System.out.println("Invalid choice."); sc.nextLine(); return; }
        int choice = sc.nextInt(); sc.nextLine();
        
        if (choice == 1) {
            String qry = "SELECT O_id, O_name, O_license, O_contact, O_status FROM tbl_owners";
            String[] hdrs = {"ID", "Name", "License", "Contact", "Status"};
            String[] keys = {"O_id", "O_name", "O_license", "O_contact", "O_status"};
            List<Map<String, Object>> records = conf.fetchRecords(qry);
            config.displayRecords(records, hdrs, keys); // Call the static method
        } else if (choice == 2) {
            String qry = "SELECT t1.V_id, t1.V_plate, t1.V_brand, t1.V_model, t2.O_name, t2.O_license FROM tbl_vehicles t1 JOIN tbl_owners t2 ON t1.O_id = t2.O_id";
            String[] hdrs = {"ID", "Plate", "Brand", "Model", "Owner Name", "Owner License"};
            String[] keys = {"V_id", "V_plate", "V_brand", "V_model", "O_name", "O_license"};
            List<Map<String, Object>> records = conf.fetchRecords(qry);
            config.displayRecords(records, hdrs, keys); // Call the static method
        } else {
            System.out.println("Invalid view option.");
        }
    }

    // REMOVED: The local displayRecords method has been moved to config.java

    private void updateRecordsMenu(){
        System.out.println("\n--- UPDATE RECORDS ---");
        System.out.println("1. Update Owner Status (Active/Inactive)");
        System.out.println("2. Update Vehicle Model");
        System.out.print("Enter choice (1 or 2): ");
        
        if (!sc.hasNextInt()) { System.out.println("Invalid choice."); sc.nextLine(); return; }
        int choice = sc.nextInt(); sc.nextLine();
        
        if (choice == 1) {
            updateOwnerStatus();
        } else if (choice == 2) {
            updateVehicleModel();
        } else {
            System.out.println("Invalid update option.");
        }
    }

    private void updateOwnerStatus() {
        String qry = "SELECT O_id, O_name, O_status FROM tbl_owners";
        String[] hdrs = {"ID", "Name", "Status"};
        String[] keys = {"O_id", "O_name", "O_status"};
        config.displayRecords(conf.fetchRecords(qry), hdrs, keys);
        
        System.out.print("Enter Owner ID to update: ");
        if (!sc.hasNextInt()) { System.out.println("Invalid ID."); sc.nextLine(); return; }
        int id = sc.nextInt(); sc.nextLine();
        
        System.out.print("Enter new Status: ");
        String nstatus = sc.nextLine();
        
        String updateQry = "UPDATE tbl_owners SET O_status = ? WHERE O_id = ?";
        conf.updateRecord(updateQry, nstatus, id);
    }

    private void updateVehicleModel() {
        String qry = "SELECT V_id, V_plate, V_model FROM tbl_vehicles";
        String[] hdrs = {"ID", "Plate", "Model"};
        String[] keys = {"V_id", "V_plate", "V_model"};
        config.displayRecords(conf.fetchRecords(qry), hdrs, keys);
        
        System.out.print("Enter Vehicle ID to update: ");
        if (!sc.hasNextInt()) { System.out.println("Invalid ID."); sc.nextLine(); return; }
        int id = sc.nextInt(); sc.nextLine();
        
        System.out.print("Enter new Model: ");
        String nmodel = sc.nextLine();
        
        String updateQry = "UPDATE tbl_vehicles SET V_model = ? WHERE V_id = ?";
        conf.updateRecord(updateQry, nmodel, id);
    }
    
    private void deleteRecordsMenu(){
        System.out.println("\n--- DELETE RECORDS ---");
        System.out.println("1. Owner (Deletes all vehicles)");
        System.out.println("2. Vehicle");
        System.out.print("Enter choice (1 or 2): ");
        
        if (!sc.hasNextInt()) { System.out.println("Invalid choice."); sc.nextLine(); return; }
        int choice = sc.nextInt(); sc.nextLine();
        
        if (choice == 1) {
            deleteOwner();
        } else if (choice == 2) {
            deleteVehicle();
        } else {
            System.out.println("Invalid delete option.");
        }
    }

    private void deleteOwner(){
        System.out.println("\n--- DELETE OWNER ---");
        System.out.print("Enter Owner ID: ");
        
        if (!sc.hasNextInt()) { System.out.println("Invalid ID format. Operation cancelled."); sc.nextLine(); return; }
        int id = sc.nextInt(); sc.nextLine();
        
        String qry = "DELETE FROM tbl_owners WHERE O_id = ?";
        conf.deleteRecord(qry, id);
    }
    
    private void deleteVehicle(){
        System.out.println("\n--- DELETE VEHICLE ---");
        System.out.print("Enter Vehicle ID: ");
        
        if (!sc.hasNextInt()) { System.out.println("Invalid ID format. Operation cancelled."); sc.nextLine(); return; }
        int id = sc.nextInt(); sc.nextLine();
        
        String qry = "DELETE FROM tbl_vehicles WHERE V_id = ?";
        conf.deleteRecord(qry, id);
    }
}