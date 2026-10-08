package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/time_management_db";
    private static final String USER = "time_app";
    private static final String PASSWORD = "gdgy276243pcv.ed,rh3794";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
            return connection;
        }

        catch (Exception e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        getConnection();
    }
}
