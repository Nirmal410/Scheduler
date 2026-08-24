package com.assigment.Scheduler.dto;

public class MetricsDTO {
    private long totalStudents;
    private long totalCompanies;
    private long totalRooms;
    private long totalTimeSlots;
    private long totalShortlists;
    private long interviewsScheduled;
    private long interviewsUnscheduled;
    private long interviewsMoved;
    private double schedulingRatePercent;
    private double overallRoomUtilizationPercent;
    private double averageStudentWaitMinutes;
    private long maxStudentWaitMinutes;
    private long studentConflictCount;
    private long totalDisruptionsProcessed;
    private double averageReplanChurnRatio;

    public MetricsDTO() {}

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }
    public long getTotalCompanies() { return totalCompanies; }
    public void setTotalCompanies(long totalCompanies) { this.totalCompanies = totalCompanies; }
    public long getTotalRooms() { return totalRooms; }
    public void setTotalRooms(long totalRooms) { this.totalRooms = totalRooms; }
    public long getTotalTimeSlots() { return totalTimeSlots; }
    public void setTotalTimeSlots(long totalTimeSlots) { this.totalTimeSlots = totalTimeSlots; }
    public long getTotalShortlists() { return totalShortlists; }
    public void setTotalShortlists(long totalShortlists) { this.totalShortlists = totalShortlists; }
    public long getInterviewsScheduled() { return interviewsScheduled; }
    public void setInterviewsScheduled(long interviewsScheduled) { this.interviewsScheduled = interviewsScheduled; }
    public long getInterviewsUnscheduled() { return interviewsUnscheduled; }
    public void setInterviewsUnscheduled(long interviewsUnscheduled) { this.interviewsUnscheduled = interviewsUnscheduled; }
    public long getInterviewsMoved() { return interviewsMoved; }
    public void setInterviewsMoved(long interviewsMoved) { this.interviewsMoved = interviewsMoved; }
    public double getSchedulingRatePercent() { return schedulingRatePercent; }
    public void setSchedulingRatePercent(double schedulingRatePercent) { this.schedulingRatePercent = schedulingRatePercent; }
    public double getOverallRoomUtilizationPercent() { return overallRoomUtilizationPercent; }
    public void setOverallRoomUtilizationPercent(double overallRoomUtilizationPercent) { this.overallRoomUtilizationPercent = overallRoomUtilizationPercent; }
    public double getAverageStudentWaitMinutes() { return averageStudentWaitMinutes; }
    public void setAverageStudentWaitMinutes(double averageStudentWaitMinutes) { this.averageStudentWaitMinutes = averageStudentWaitMinutes; }
    public long getMaxStudentWaitMinutes() { return maxStudentWaitMinutes; }
    public void setMaxStudentWaitMinutes(long maxStudentWaitMinutes) { this.maxStudentWaitMinutes = maxStudentWaitMinutes; }
    public long getStudentConflictCount() { return studentConflictCount; }
    public void setStudentConflictCount(long studentConflictCount) { this.studentConflictCount = studentConflictCount; }
    public long getTotalDisruptionsProcessed() { return totalDisruptionsProcessed; }
    public void setTotalDisruptionsProcessed(long totalDisruptionsProcessed) { this.totalDisruptionsProcessed = totalDisruptionsProcessed; }
    public double getAverageReplanChurnRatio() { return averageReplanChurnRatio; }
    public void setAverageReplanChurnRatio(double averageReplanChurnRatio) { this.averageReplanChurnRatio = averageReplanChurnRatio; }
}
