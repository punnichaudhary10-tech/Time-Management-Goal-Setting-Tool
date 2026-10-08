package user;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserService {

    // =====================================================
    // REGISTER USER
    // =====================================================

    public boolean registerUser(User user) {

        String sql = "INSERT INTO users (name, email, password, role) " +
                "VALUES (?, ?, ?, ?)";

        Connection connection = null;

        try {

            connection = DBConnection.getConnection();

            // Check connection BEFORE using it
            if (connection == null) {
                System.out.println("Database connection failed.");
                return false;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, user.getUsername());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getPassword());
                statement.setString(4, "USER");

                statement.executeUpdate();

                System.out.println(
                        "User Registered Successfully");

                return true;
            }

        } catch (Exception e) {

            System.out.println("Registration Failed");
            e.printStackTrace();

            return false;

        } finally {

            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // =====================================================
    // LOGIN USER
    // =====================================================

    public boolean loginUser(
            String email,
            String password) {

        String sql = "SELECT user_id, name, email, role " +
                "FROM users " +
                "WHERE email = ? AND password = ?";

        Connection connection = null;

        try {

            connection = DBConnection.getConnection();

            // Check connection BEFORE using it
            if (connection == null) {
                System.out.println("Database connection failed.");
                return false;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, email);
                statement.setString(2, password);

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()) {

                        System.out.println(
                                "Login Successful");

                        System.out.println(
                                "User ID: " +
                                        result.getInt("user_id"));

                        System.out.println(
                                "Name: " +
                                        result.getString("name"));

                        System.out.println(
                                "Role: " +
                                        result.getString("role"));

                        return true;

                    } else {

                        System.out.println(
                                "Invalid Email or Password");

                        return false;
                    }
                }
            }

        } catch (Exception e) {

            System.out.println("Login Failed");
            e.printStackTrace();

            return false;

        } finally {

            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}