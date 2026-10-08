package timetracking;

import java.time.LocalDateTime;

public class TimeEntry {

    private int entryId;
    private int userId;
    private int goalId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationMinutes;

    public TimeEntry(int entryId, int userId, int goalId,
                     LocalDateTime startTime,
                     LocalDateTime endTime,
                     int durationMinutes) {

        this.entryId = entryId;
        this.userId = userId;
        this.goalId = goalId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
    }

    public int getEntryId() {
        return entryId;
    }

    public int getUserId() {
        return userId;
    }

    public int getGoalId() {
        return goalId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    @Override
    public String toString() {
        return "Entry ID: " + entryId +
                "\nUser ID: " + userId +
                "\nGoal ID: " + goalId +
                "\nStart Time: " + startTime +
                "\nEnd Time: " + endTime +
                "\nDuration: " + durationMinutes + " minutes";
    }
}