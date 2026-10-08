package progress;

import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProgressService {

    // UPDATE OR CREATE PROGRESS
    public void updateProgress(int goalId, double percentage) {

        // Progress must be between 0 and 100
        if (percentage < 0 || percentage > 100) {
            System.out.println("Progress must be between 0 and 100!");
            return;
        }

        String status;

        if (percentage == 0) {
            status = "NOT_STARTED";
        } else if (percentage < 100) {
            status = "IN_PROGRESS";
        } else {
            status = "COMPLETED";
        }

        Connection connection = DBConnection.getConnection();

        if (connection == null) {
            System.out.println("Database connection failed!");
            return;
        }

        try {

            connection.setAutoCommit(false);

            // Check whether goal exists
            String goalCheckSql =
                    "SELECT goal_id FROM goals WHERE goal_id = ?";

            PreparedStatement goalCheck =
                    connection.prepareStatement(goalCheckSql);

            goalCheck.setInt(1, goalId);

            ResultSet goalResult = goalCheck.executeQuery();

            if (!goalResult.next()) {
                System.out.println("Goal not found!");
                connection.rollback();
                return;
            }

            // Check whether progress already exists
            String checkSql =
                    "SELECT progress_id FROM progress WHERE goal_id = ? LIMIT 1";

            PreparedStatement checkStatement =
                    connection.prepareStatement(checkSql);

            checkStatement.setInt(1, goalId);

            ResultSet result = checkStatement.executeQuery();

            if (result.next()) {

                // Existing progress → UPDATE
                String updateSql =
                        "UPDATE progress " +
                        "SET progress_percentage = ?, completion_status = ? " +
                        "WHERE goal_id = ?";

                PreparedStatement updateStatement =
                        connection.prepareStatement(updateSql);

                updateStatement.setDouble(1, percentage);
                updateStatement.setString(2, status);
                updateStatement.setInt(3, goalId);

                updateStatement.executeUpdate();

                updateStatement.close();

            } else {

                // No progress yet → INSERT
                String insertSql =
                        "INSERT INTO progress " +
                        "(goal_id, progress_percentage, completion_status) " +
                        "VALUES (?, ?, ?)";

                PreparedStatement insertStatement =
                        connection.prepareStatement(insertSql);

                insertStatement.setInt(1, goalId);
                insertStatement.setDouble(2, percentage);
                insertStatement.setString(3, status);

                insertStatement.executeUpdate();

                insertStatement.close();
            }

            // Keep goals.status synchronized
            String goalUpdateSql =
                    "UPDATE goals SET status = ? WHERE goal_id = ?";

            PreparedStatement goalUpdateStatement =
                    connection.prepareStatement(goalUpdateSql);

            goalUpdateStatement.setString(1, status);
            goalUpdateStatement.setInt(2, goalId);

            goalUpdateStatement.executeUpdate();

            connection.commit();

            System.out.println("Progress updated successfully!");
            System.out.println("Goal Status: " + status);

            goalUpdateStatement.close();
            result.close();
            checkStatement.close();
            goalResult.close();
            goalCheck.close();

        } catch (Exception e) {

            try {
                connection.rollback();
            } catch (Exception ignored) {
            }

            System.out.println("Progress update failed!");
            e.printStackTrace();

        } finally {

            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (Exception ignored) {
            }
        }
    }


    // GET PROGRESS OF A GOAL
    public Progress getProgressByGoal(int goalId) {

        String sql =
                "SELECT * FROM progress " +
                "WHERE goal_id = ? " +
                "ORDER BY updated_at DESC LIMIT 1";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, goalId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {

                return new Progress(
                        result.getInt("progress_id"),
                        result.getInt("goal_id"),
                        result.getDouble("progress_percentage"),
                        result.getString("completion_status"),
                        result.getTimestamp("updated_at").toLocalDateTime()
                );
            }

        } catch (Exception e) {
            System.out.println("Unable to fetch progress!");
            e.printStackTrace();
        }

        return null;
    }
}