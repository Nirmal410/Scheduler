package com.assigment.Scheduler.dto;

public class ScheduleResultDTO {
    private long totalShortlists;
    private long scheduledCount;
    private long unscheduledCount;
    private double schedulingRatePercent;
    private long executionTimeMs;

    public ScheduleResultDTO() {}

    public ScheduleResultDTO(long totalShortlists, long scheduledCount, long unscheduledCount, long executionTimeMs) {
        this.totalShortlists = totalShortlists;
        this.scheduledCount = scheduledCount;
        this.unscheduledCount = unscheduledCount;
        this.executionTimeMs = executionTimeMs;
        this.schedulingRatePercent = totalShortlists > 0 ? (scheduledCount * 100.0 / totalShortlists) : 0.0;
    }

    public long getTotalShortlists() { return totalShortlists; }
    public void setTotalShortlists(long totalShortlists) { this.totalShortlists = totalShortlists; }
    public long getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(long scheduledCount) { this.scheduledCount = scheduledCount; }
    public long getUnscheduledCount() { return unscheduledCount; }
    public void setUnscheduledCount(long unscheduledCount) { this.unscheduledCount = unscheduledCount; }
    public double getSchedulingRatePercent() { return schedulingRatePercent; }
    public void setSchedulingRatePercent(double schedulingRatePercent) { this.schedulingRatePercent = schedulingRatePercent; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}
