package user;

import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserService {

    public boolean registerUser(User user) {

        String sql = "INSERT INTO users (name, email, password, role) " +
                "VALUES (?, ?, ?, ?)";

        Connection connection = null;

        try {

            connection = DBConnection.getConnection();

            if (connection == null) {
                System.out.println("Database connection failed.");
                return false;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        user.getUsername());

                statement.setString(
                        2,
                        user.getEmail());

                statement.setString(
                        3,
                        user.getPassword());

                statement.setString(
                        4,
                        "USER");

                statement.executeUpdate();

                System.out.println(
                        "User Registered Successfully");

                return true;
            }

        } catch (Exception e) {

            System.out.println(
                    "Registration Failed");

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

    public boolean loginUser(
            String email,
            String password) {

        String sql = "SELECT user_id, name, email, role " +
                "FROM users " +
                "WHERE email = ? AND password = ?";

        Connection connection = null;

        try {

            connection = DBConnection.getConnection();

            if (connection == null) {
                System.out.println(
                        "Database connection failed.");
                return false;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        email);

                statement.setString(
                        2,
                        password);

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

            System.out.println(
                    "Login Failed");

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

    public int getUserIdByEmail(String email) {

        String sql = "SELECT user_id " +
                "FROM users " +
                "WHERE email = ? " +
                "LIMIT 1";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return -1;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        email.trim());

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()) {

                        return result.getInt(
                                "user_id");
                    }
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "User ID lookup failed: " +
                            e.getMessage());
        }

        return -1;
    }

    public boolean emailExists(String email) {

        String sql = "SELECT 1 " +
                "FROM users " +
                "WHERE email = ? " +
                "LIMIT 1";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return false;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        email.trim());

                try (ResultSet result = statement.executeQuery()) {

                    return result.next();
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "Email check failed: " +
                            e.getMessage());

            return false;
        }
    }

    /*
     * =========================================
     * GET COMPLETE USER PROFILE
     * =========================================
     */

    public UserProfile getUserProfileByEmail(
            String email) {

        String sql = "SELECT user_id, name, email, role, created_at " +
                "FROM users " +
                "WHERE email = ? " +
                "LIMIT 1";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return null;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        email.trim());

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()) {

                        return new UserProfile(

                                result.getInt(
                                        "user_id"),

                                result.getString(
                                        "name"),

                                result.getString(
                                        "email"),

                                result.getString(
                                        "role"),

                                result.getTimestamp(
                                        "created_at").toString());
                    }
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "User profile lookup failed: " +
                            e.getMessage());
        }

        return null;
    }
}