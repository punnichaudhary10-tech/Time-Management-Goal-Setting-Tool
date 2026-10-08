package admin;

import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

public class AdminService {

    // =========================================
    // 1. CREATE GOAL PARAMETER
    // =========================================
    public void createGoalParameter(GoalParameter parameter) {

        String sql =
                "INSERT INTO goal_parameters " +
                "(goal_type, tracking_metric, target_unit, created_by) " +
                "VALUES (?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, parameter.getGoalType());
            statement.setString(2, parameter.getTrackingMetric());
            statement.setString(3, parameter.getTargetUnit());

            if (parameter.getCreatedBy() != null) {
                statement.setInt(4, parameter.getCreatedBy());
            } else {
                statement.setNull(4, java.sql.Types.INTEGER);
            }

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println(
                        "Goal parameter created successfully!"
                );
            }

        } catch (Exception e) {
            System.out.println(
                    "Unable to create goal parameter!"
            );
            e.printStackTrace();
        }
    }


    // =========================================
    // 2. GET ALL GOAL PARAMETERS
    // =========================================
    public List<GoalParameter> getAllGoalParameters() {

        List<GoalParameter> parameters =
                new ArrayList<>();

        String sql =
                "SELECT * FROM goal_parameters";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            while (result.next()) {

                Integer createdBy =
                        result.getObject("created_by") == null
                                ? null
                                : result.getInt("created_by");

                GoalParameter parameter =
                        new GoalParameter(
                                result.getInt("parameter_id"),
                                result.getString("goal_type"),
                                result.getString("tracking_metric"),
                                result.getString("target_unit"),
                                createdBy,
                                result.getTimestamp("created_at")
                                        .toLocalDateTime()
                        );

                parameters.add(parameter);
            }

        } catch (Exception e) {
            System.out.println(
                    "Unable to fetch goal parameters!"
            );
            e.printStackTrace();
        }

        return parameters;
    }


    // =========================================
    // 3. UPDATE GOAL PARAMETER
    // =========================================
    public void updateGoalParameter(
            int parameterId,
            String goalType,
            String trackingMetric,
            String targetUnit
    ) {

        String sql =
                "UPDATE goal_parameters SET " +
                "goal_type = ?, " +
                "tracking_metric = ?, " +
                "target_unit = ? " +
                "WHERE parameter_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, goalType);
            statement.setString(2, trackingMetric);
            statement.setString(3, targetUnit);
            statement.setInt(4, parameterId);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println(
                        "Goal parameter updated successfully!"
                );
            } else {
                System.out.println(
                        "Goal parameter not found!"
                );
            }

        } catch (Exception e) {
            System.out.println(
                    "Unable to update goal parameter!"
            );
            e.printStackTrace();
        }
    }


    // =========================================
    // 4. DELETE GOAL PARAMETER
    // =========================================
    public void deleteGoalParameter(int parameterId) {

        String sql =
                "DELETE FROM goal_parameters " +
                "WHERE parameter_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, parameterId);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println(
                        "Goal parameter deleted successfully!"
                );
            } else {
                System.out.println(
                        "Goal parameter not found!"
                );
            }

        } catch (Exception e) {
            System.out.println(
                    "Unable to delete goal parameter!"
            );
            e.printStackTrace();
        }
    }


    // =========================================
    // 5. VIEW ALL USERS
    // =========================================
    public void viewAllUsers() {

        String sql =
                "SELECT user_id, name, email, role, created_at " +
                "FROM users";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            System.out.println(
                    "\n----- ALL USERS -----"
            );

            while (result.next()) {

                System.out.println(
                        "User ID: "
                                + result.getInt("user_id")
                );

                System.out.println(
                        "Name: "
                                + result.getString("name")
                );

                System.out.println(
                        "Email: "
                                + result.getString("email")
                );

                System.out.println(
                        "Role: "
                                + result.getString("role")
                );

                System.out.println(
                        "Created At: "
                                + result.getTimestamp("created_at")
                );

                System.out.println(
                        "---------------------"
                );
            }

        } catch (Exception e) {
            System.out.println(
                    "Unable to fetch users!"
            );
            e.printStackTrace();
        }
    }


    // =========================================
    // 6. DELETE USER
    // =========================================
    public void deleteUser(int userId) {

        String sql =
                "DELETE FROM users WHERE user_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println(
                        "User deleted successfully!"
                );
            } else {
                System.out.println(
                        "User not found!"
                );
            }

        } catch (Exception e) {
            System.out.println(
                    "Unable to delete user!"
            );
            e.printStackTrace();
        }
    }


    // =========================================
    // 7. ADD USAGE LOG
    // =========================================
    public void addUsageLog(
            int userId,
            String activity
    ) {

        String sql =
                "INSERT INTO usage_logs " +
                "(user_id, activity) " +
                "VALUES (?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);
            statement.setString(2, activity);

            statement.executeUpdate();

            System.out.println(
                    "Activity logged successfully!"
            );

        } catch (Exception e) {
            System.out.println(
                    "Unable to log activity!"
            );
            e.printStackTrace();
        }
    }


    // =========================================
    // 8. VIEW USAGE LOGS
    // =========================================
    public void viewUsageLogs() {

        String sql =
                "SELECT l.log_id, l.user_id, " +
                "u.name, l.activity, l.activity_time " +
                "FROM usage_logs l " +
                "JOIN users u " +
                "ON l.user_id = u.user_id " +
                "ORDER BY l.activity_time DESC";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            System.out.println(
                    "\n----- USAGE LOGS -----"
            );

            while (result.next()) {

                System.out.println(
                        "Log ID: "
                                + result.getInt("log_id")
                );

                System.out.println(
                        "User ID: "
                                + result.getInt("user_id")
                );

                System.out.println(
                        "User: "
                                + result.getString("name")
                );

                System.out.println(
                        "Activity: "
                                + result.getString("activity")
                );

                System.out.println(
                        "Time: "
                                + result.getTimestamp("activity_time")
                );

                System.out.println(
                        "----------------------"
                );
            }

        } catch (Exception e) {
            System.out.println(
                    "Unable to fetch usage logs!"
            );
            e.printStackTrace();
        }
    }
}