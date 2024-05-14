package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Random;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    //part 1
    private static void createPatient(String[] tokens) {
        if(tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if(!checkStrongPassword(tokens[2])) {
            System.out.println("Please use a strong password!");
            return;
        }
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        //create patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch(SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if(!checkStrongPassword(tokens[2])) {
            System.out.println("Please use a strong password!");
            return;
        }
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean checkStrongPassword(String password) {
        boolean lengthStrain = false;
        boolean lower = false;
        boolean upper = false;
        boolean num = false;
        boolean letter = false;
        boolean specialChar = false;

        lengthStrain = (password.length() >= 8);
        for (int i = 0; i < password.length(); i++) {
            char temp = password.charAt(i);
            if(Character.isDigit(temp)) {
                num = true;
            } else if(Character.isLetter(temp)) {
                letter = true;
                if(Character.isLowerCase(temp)) {
                    lower = true;
                } else {
                    upper = true;
                }
            } else if (temp == '?' || temp == '@' || temp == '#' || temp == '!') {
                specialChar = true;
            } else {
                return false;
            }
        }

        return letter && num && lengthStrain && lower && upper && specialChar;
    }

    //part 1
    private static boolean usernameExistsPatient(String username) {
        //check if username exists for patient
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try{
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch(SQLException e){
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    //part 1
    private static void loginPatient(String[] tokens) {
        if(currentPatient != null || currentCaregiver != null){
            System.out.println("User already logged in.");
            return;
        }

        if(tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Patient patient = null;
        try{
            patient = new Patient.PatientGetter(username, password).get();
        } catch(SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }

        if(patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens){
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if(tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        //check if date is valid
        String date = tokens[1];
        String[] temp = date.split("-");
        if(!(temp.length == 3 &&
                (temp[0].length() == 4 && temp[1].length() == 2 && temp[2].length() == 2))){
            System.out.println("Please try again with valid date!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectAvailable = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
        String selectDoses = "SELECT * FROM Vaccines WHERE Doses > 0";
        try{
            PreparedStatement statement1 = con.prepareStatement(selectAvailable);
            PreparedStatement statement2 = con.prepareStatement(selectDoses);
            statement1.setString(1, date);
            ResultSet rs1 = statement1.executeQuery();
            ResultSet rs2 = statement2.executeQuery();
            System.out.println("Available caregivers on selected date:");
            System.out.print("| ");
            while(rs1.next()){
                System.out.print(rs1.getString(1) + " | ");
            }
            System.out.println();

            System.out.println("Available vaccines:");
            System.out.print("| ");
            while(rs2.next()){
                System.out.print(rs2.getString(1)  + " " + rs2.getInt(2) + " | ");
            }
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

    }

    private static void reserve(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        } else if (currentCaregiver != null && currentPatient == null) {
            System.out.println("Please login as a patient!");
        }
        if(tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        int reserveID = generateReserveID();
        if(reserveID <= 0){
            System.out.println("Please try again!");
        } else {
            updateReservation(tokens[1], tokens[2], reserveID);
        }
    }

    private static int generateReserveID(){
        Random rand = new Random();
        String result;

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        boolean flag = false;
        try{
            result = "";
            int intResult = -1;
            while(!flag){
                for(int i = 0; i < 7; i++) {
                    result += rand.nextInt(10);
                }
                intResult = Integer.parseInt(result);
                String checkUnique = "SELECT * FROM Reservations WHERE ID = ?";
                PreparedStatement statement = con.prepareStatement(checkUnique);
                statement.setInt(1, intResult);
                ResultSet rs = statement.executeQuery();
                flag = !rs.isBeforeFirst();
            }
            return intResult;
        } catch(SQLException e) {
            System.out.println("Please try again!");
        } finally {
            cm.closeConnection();
        }
        return -1;
    }

    private static void updateReservation(String date, String vaccine, int reserveID) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String updateStatement = "INSERT INTO Reservations VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(updateStatement);
            statement.setInt(1, reserveID);
            String caregiver = checkAvailability(date, vaccine);
            if(caregiver == null) {
                return;
            }
            if(!updateValues(date, caregiver, vaccine)) {
                System.out.println("Please try again!");
                return;
            }
            statement.setString(2, currentPatient.getUsername());
            statement.setString(3, caregiver);
            statement.setString(4, vaccine);
            statement.setString(5, date);
            if(statement.executeUpdate() != 1) {
                System.out.println("Please try again!");
                return;
            }
            System.out.println("Appointment ID: " + reserveID +", Caregiver username: " + caregiver);
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static String checkAvailability(String date, String vaccine) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectCaregiver = "SELECT TOP 1 Username FROM Availabilities WHERE Time = ?";
        String checkVaccine = "SELECT * FROM Vaccines WHERE Name = ? AND Doses >= 1";
        try {
            PreparedStatement stVaccine = con.prepareStatement(checkVaccine);
            stVaccine.setString(1, vaccine);
            if(!stVaccine.executeQuery().isBeforeFirst()) {
                System.out.println("Not enough available doses!");
                return null;
            }
            PreparedStatement stCaregiver = con.prepareStatement(selectCaregiver);
            stCaregiver.setString(1, date);
            ResultSet rs = stCaregiver.executeQuery();
            if (!rs.isBeforeFirst()) {
                System.out.println("No Caregiver is available!");
                return null;
            } else {
                rs.next();
                return rs.getString(1);
            }
        } catch(SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return null;
    }

    private static boolean updateValues(String date, String caregiver, String vaccine) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String Available = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
        String Vaccine = "UPDATE Vaccines SET Doses = doses - 1 WHERE Name = ?";
        try{
            PreparedStatement stAvailable = con.prepareStatement(Available);
            stAvailable.setString(1, date);
            stAvailable.setString(2, caregiver);
            boolean flag1 = (stAvailable.executeUpdate() == 1);
            PreparedStatement stVaccine = con.prepareStatement(Vaccine);
            stVaccine.setString(1, vaccine);
            boolean flag2 = (stVaccine.executeUpdate() == 1);
            return flag1 && flag2;
        } catch(SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return false;
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {

    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if(tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        String current;
        int reservationID;
        String patient;
        String caregiver;
        String vaccine;
        String date;
        String select = "SELECT * FROM Reservations WHERE ";

        if(currentPatient != null) {
            select = select + "Patient = ? ";
            current = currentPatient.getUsername();
        } else {
            select = select + "Caregiver = ? ";
            current = currentCaregiver.getUsername();
        }
        select += " ORDER BY ID ASC";
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            PreparedStatement statement = con.prepareStatement(select);
            statement.setString(1, current);
            ResultSet rs = statement.executeQuery();
            System.out.println("Current reservations:");
            while(rs.next()){
                reservationID = rs.getInt(1);
                patient = rs.getString(2);
                caregiver = rs.getString(3);
                vaccine = rs.getString(4);
                date = rs.getString(5);
                System.out.print(reservationID + " " + vaccine + " " + date + " ");
                if(currentPatient != null) {
                    System.out.println(caregiver);
                } else {
                    System.out.println(patient);
                }
            }
        } catch(SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        if(tokens.length != 1){
            System.out.println("Please try again!");
            return;
        }
        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        } else if(currentPatient != null || currentCaregiver != null){
            currentPatient = null;
            currentCaregiver = null;
            System.out.println("Successfully logged out!");
            return;
        }
        System.out.println("Please try again!");
        return;
    }
}
