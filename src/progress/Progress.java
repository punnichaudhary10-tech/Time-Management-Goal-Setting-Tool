package progress;

import java.time.LocalDateTime;

public class Progress {

    private int progressId;
    private int goalId;
    private double progressPercentage;
    private String completionStatus;
    private LocalDateTime updatedAt;

    public Progress(int progressId,
                    int goalId,
                    double progressPercentage,
                    String completionStatus,
                    LocalDateTime updatedAt) {

        this.progressId = progressId;
        this.goalId = goalId;
        this.progressPercentage = progressPercentage;
        this.completionStatus = completionStatus;
        this.updatedAt = updatedAt;
    }

    public int getProgressId() {
        return progressId;
    }

    public int getGoalId() {
        return goalId;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Progress ID: " + progressId +
                "\nGoal ID: " + goalId +
                "\nProgress: " + progressPercentage + "%" +
                "\nStatus: " + completionStatus +
                "\nUpdated At: " + updatedAt;
    }
}