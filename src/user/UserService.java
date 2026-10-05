package user;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class UserService {

    public void registerUser(User user) {

        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";

        try {
            Connection connection = DBConnection.getConnection();

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());

            statement.setString(4, "USER");

            statement.executeUpdate();

            System.out.println("User Registered Successfully");

            statement.close();
            statement.close();
        } catch (Exception e) {
            System.out.println("Registration Failed");
            e.printStackTrace();
        }
    }

    public boolean loginUser(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try {
            Connection connection = DBConnection.getConnection();

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setString(1, email);
            statement.setString(2, password);

            var result = statement.executeQuery();

            if (result.next()) {
                System.out.println("Login Successful");
                return true;
            } else {
                System.out.println("Invalid Email or Password");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Login Failed");
            e.printStackTrace();
            return false;
        }
    }
}
