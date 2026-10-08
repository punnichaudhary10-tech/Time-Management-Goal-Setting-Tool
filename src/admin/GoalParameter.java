package admin;

import java.time.LocalDateTime;

public class GoalParameter {

    private int parameterId;
    private String goalType;
    private String trackingMetric;
    private String targetUnit;
    private Integer createdBy;
    private LocalDateTime createdAt;

    public GoalParameter(int parameterId,
                         String goalType,
                         String trackingMetric,
                         String targetUnit,
                         Integer createdBy,
                         LocalDateTime createdAt) {

        this.parameterId = parameterId;
        this.goalType = goalType;
        this.trackingMetric = trackingMetric;
        this.targetUnit = targetUnit;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getParameterId() {
        return parameterId;
    }

    public String getGoalType() {
        return goalType;
    }

    public String getTrackingMetric() {
        return trackingMetric;
    }

    public String getTargetUnit() {
        return targetUnit;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public void setTrackingMetric(String trackingMetric) {
        this.trackingMetric = trackingMetric;
    }

    public void setTargetUnit(String targetUnit) {
        this.targetUnit = targetUnit;
    }

    @Override
    public String toString() {
        return "Parameter ID: " + parameterId +
                "\nGoal Type: " + goalType +
                "\nTracking Metric: " + trackingMetric +
                "\nTarget Unit: " + targetUnit +
                "\nCreated By: " + createdBy +
                "\nCreated At: " + createdAt;
    }
}