package scheduler.model;
import scheduler.db.ConnectionManager;
import scheduler.util.*;
import java.sql.*;
import java.util.Arrays;
import java.util.Queue;

public class Reservations {
    public String appointmentID;
    public String patient;
    public String caregiver;
    public String vaccine;
    public String time;

    public Reservations(String appointmentID, String vaccine) {
        this.appointmentID = appointmentID;
        this.time = null;
        this.patient = null;
        this.caregiver = null;
        this.vaccine = vaccine;
    }

    public Reservations(String appointmentID, String time, String patient,
                        String caregiver, String vaccine) {
        this.appointmentID = appointmentID;
        this.time = time;
        this.patient = patient;
        this.caregiver = caregiver;
        this.vaccine = vaccine;
    }

    public void MakeReservation() throws SQLException{
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addReservations = "INSERT INTO Reservations VALUES (?, ?, ?, ?, ?)";
        try{
            PreparedStatement statement = con.prepareStatement(addReservations);
            statement.setString(1, this.appointmentID);
            statement.setString(2,this.patient);
            statement.setString(3, caregiver);
            statement.setString(4, vaccine);
            statement.setString(5, time);
            statement.executeUpdate();
        } catch(SQLException e) {
            System.out.println("Please try again!");
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
}
