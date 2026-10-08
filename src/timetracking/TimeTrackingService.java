package timetracking;

import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TimeTrackingService {

    // 1. ADD TIME ENTRY
    public void addTimeEntry(TimeEntry entry) {

        if (entry.getEndTime().isBefore(entry.getStartTime())) {
            System.out.println("End time cannot be before start time!");
            return;
        }

        long minutes = Duration.between(
                entry.getStartTime(),
                entry.getEndTime()
        ).toMinutes();

        entry.setDurationMinutes((int) minutes);

        String sql = "INSERT INTO time_entries " +
                "(user_id, goal_id, start_time, end_time, duration_minutes) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, entry.getUserId());
            statement.setInt(2, entry.getGoalId());

            statement.setTimestamp(
                    3,
                    Timestamp.valueOf(entry.getStartTime())
            );

            statement.setTimestamp(
                    4,
                    Timestamp.valueOf(entry.getEndTime())
            );

            statement.setInt(
                    5,
                    entry.getDurationMinutes()
            );

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println("Time entry added successfully!");
            }

        } catch (Exception e) {
            System.out.println("Unable to add time entry!");
            e.printStackTrace();
        }
    }


    // 2. GET ALL TIME ENTRIES OF A GOAL
    public List<TimeEntry> getEntriesByGoal(int goalId) {

        List<TimeEntry> entries = new ArrayList<>();

        String sql =
                "SELECT * FROM time_entries WHERE goal_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, goalId);

            ResultSet result = statement.executeQuery();

            while (result.next()) {

                TimeEntry entry = new TimeEntry(
                        result.getInt("entry_id"),
                        result.getInt("user_id"),
                        result.getInt("goal_id"),
                        result.getTimestamp("start_time").toLocalDateTime(),
                        result.getTimestamp("end_time").toLocalDateTime(),
                        result.getInt("duration_minutes")
                );

                entries.add(entry);
            }

        } catch (Exception e) {
            System.out.println("Unable to fetch time entries!");
            e.printStackTrace();
        }

        return entries;
    }


    // 3. GET ALL TIME ENTRIES OF A USER
    public List<TimeEntry> getEntriesByUser(int userId) {

        List<TimeEntry> entries = new ArrayList<>();

        String sql =
                "SELECT * FROM time_entries WHERE user_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);

            ResultSet result = statement.executeQuery();

            while (result.next()) {

                TimeEntry entry = new TimeEntry(
                        result.getInt("entry_id"),
                        result.getInt("user_id"),
                        result.getInt("goal_id"),
                        result.getTimestamp("start_time").toLocalDateTime(),
                        result.getTimestamp("end_time").toLocalDateTime(),
                        result.getInt("duration_minutes")
                );

                entries.add(entry);
            }

        } catch (Exception e) {
            System.out.println("Unable to fetch user time entries!");
            e.printStackTrace();
        }

        return entries;
    }


    // 4. TOTAL TIME SPENT ON ONE GOAL
    public int getTotalMinutesForGoal(int goalId) {

        String sql =
                "SELECT COALESCE(SUM(duration_minutes), 0) AS total " +
                "FROM time_entries WHERE goal_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, goalId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getInt("total");
            }

        } catch (Exception e) {
            System.out.println("Unable to calculate total time!");
            e.printStackTrace();
        }

        return 0;
    }


    // 5. DELETE TIME ENTRY
    public void deleteTimeEntry(int entryId) {

        String sql =
                "DELETE FROM time_entries WHERE entry_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, entryId);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println("Time entry deleted successfully!");
            } else {
                System.out.println("Time entry not found!");
            }

        } catch (Exception e) {
            System.out.println("Unable to delete time entry!");
            e.printStackTrace();
        }
    }
}