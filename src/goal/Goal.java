package goal;

import java.time.LocalDate;

public class Goal {

    private int goalId;
    private int userId;
    private String goalName;
    private String description;
    private double target;
    private LocalDate deadline;
    private String status;

    public Goal(int goalId, int userId, String goalName,
                String description, double target,
                LocalDate deadline, String status) {

        this.goalId = goalId;
        this.userId = userId;
        this.goalName = goalName;
        this.description = description;
        this.target = target;
        this.deadline = deadline;
        this.status = status;
    }

    public int getGoalId() {
        return goalId;
    }

    public int getUserId() {
        return userId;
    }

    public String getGoalName() {
        return goalName;
    }

    public String getDescription() {
        return description;
    }

    public double getTarget() {
        return target;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Goal ID: " + goalId +
                "\nUser ID: " + userId +
                "\nGoal Name: " + goalName +
                "\nDescription: " + description +
                "\nTarget: " + target +
                "\nDeadline: " + deadline +
                "\nStatus: " + status;
    }
}
