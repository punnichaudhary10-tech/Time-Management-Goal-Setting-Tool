package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {

        try {

            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Railway MySQL environment variables
            String host = System.getenv("MYSQLHOST");
            String port = System.getenv("MYSQLPORT");
            String database = System.getenv("MYSQLDATABASE");
            String user = System.getenv("MYSQLUSER");
            String password = System.getenv("MYSQLPASSWORD");

            // Local development fallback
            if (host == null || host.isBlank()) {
                host = "localhost";
            }

            if (port == null || port.isBlank()) {
                port = "3306";
            }

            if (database == null || database.isBlank()) {
                database = "time_management_db";
            }

            if (user == null || user.isBlank()) {
                user = "root";
            }

            // Local password fallback
            if (password == null || password.isBlank()) {
                password = System.getenv("DB_PASSWORD");
            }

            if (password == null || password.isBlank()) {

                throw new IllegalStateException(
                        "Database password is not configured. " +
                                "Set DB_PASSWORD for local development.");
            }

            String url = "jdbc:mysql://" +
                    host + ":" +
                    port + "/" +
                    database +
                    "?useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&serverTimezone=UTC";

            Connection connection = DriverManager.getConnection(
                    url,
                    user,
                    password);

            System.out.println(
                    "Database connected successfully!");

            return connection;

        } catch (Exception e) {

            System.out.println(
                    "Database connection failed!");

            e.printStackTrace();

            return null;
        }
    }

    // Used only for testing the database connection
    public static void main(String[] args) {

        Connection connection = getConnection();

        if (connection != null) {

            try {
                connection.close();
                System.out.println(
                        "Database connection closed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}