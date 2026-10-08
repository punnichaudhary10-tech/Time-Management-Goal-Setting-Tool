package goal;

import database.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

public class GoalService {

    // 1. CREATE GOAL
    public void createGoal(Goal goal) {

        String sql = "INSERT INTO goals " +
                "(user_id, goal_name, description, target, deadline, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, goal.getUserId());
            statement.setString(2, goal.getGoalName());
            statement.setString(3, goal.getDescription());
            statement.setDouble(4, goal.getTarget());
            statement.setDate(5, Date.valueOf(goal.getDeadline()));
            statement.setString(6, goal.getStatus());

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println("Goal created successfully!");
            }

        } catch (Exception e) {
            System.out.println("Goal creation failed!");
            e.printStackTrace();
        }
    }


    // 2. GET GOAL BY ID
    public Goal getGoalById(int goalId) {

        String sql = "SELECT * FROM goals WHERE goal_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, goalId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {

                return new Goal(
                        result.getInt("goal_id"),
                        result.getInt("user_id"),
                        result.getString("goal_name"),
                        result.getString("description"),
                        result.getDouble("target"),
                        result.getDate("deadline").toLocalDate(),
                        result.getString("status")
                );
            }

        } catch (Exception e) {
            System.out.println("Unable to fetch goal!");
            e.printStackTrace();
        }

        return null;
    }


    // 3. GET ALL GOALS OF ONE USER
    public List<Goal> getGoalsByUser(int userId) {

        List<Goal> goals = new ArrayList<>();

        String sql = "SELECT * FROM goals WHERE user_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);

            ResultSet result = statement.executeQuery();

            while (result.next()) {

                Goal goal = new Goal(
                        result.getInt("goal_id"),
                        result.getInt("user_id"),
                        result.getString("goal_name"),
                        result.getString("description"),
                        result.getDouble("target"),
                        result.getDate("deadline").toLocalDate(),
                        result.getString("status")
                );

                goals.add(goal);
            }

        } catch (Exception e) {
            System.out.println("Unable to fetch user goals!");
            e.printStackTrace();
        }

        return goals;
    }


    // 4. UPDATE GOAL
    public void updateGoal(Goal goal) {

        String sql = "UPDATE goals SET " +
                "goal_name = ?, " +
                "description = ?, " +
                "target = ?, " +
                "deadline = ?, " +
                "status = ? " +
                "WHERE goal_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, goal.getGoalName());
            statement.setString(2, goal.getDescription());
            statement.setDouble(3, goal.getTarget());
            statement.setDate(4, Date.valueOf(goal.getDeadline()));
            statement.setString(5, goal.getStatus());
            statement.setInt(6, goal.getGoalId());

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println("Goal updated successfully!");
            } else {
                System.out.println("Goal not found!");
            }

        } catch (Exception e) {
            System.out.println("Goal update failed!");
            e.printStackTrace();
        }
    }


    // 5. DELETE GOAL
    public void deleteGoal(int goalId) {

        String sql = "DELETE FROM goals WHERE goal_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, goalId);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println("Goal deleted successfully!");
            } else {
                System.out.println("Goal not found!");
            }

        } catch (Exception e) {
            System.out.println("Goal deletion failed!");
            e.printStackTrace();
        }
    }
}