package com.assigment.Scheduler.dto;

public class SeedResponse {
    private String status;
    private int companiesCreated;
    private int studentsCreated;
    private int roomsCreated;
    private int panelsCreated;
    private int timeSlotsCreated;
    private int shortlistsCreated;
    private long executionTimeMs;
    private String message;

    public SeedResponse() {}

    public SeedResponse(String status, int companiesCreated, int studentsCreated, int roomsCreated,
                        int panelsCreated, int timeSlotsCreated, int shortlistsCreated, long executionTimeMs) {
        this.status = status;
        this.companiesCreated = companiesCreated;
        this.studentsCreated = studentsCreated;
        this.roomsCreated = roomsCreated;
        this.panelsCreated = panelsCreated;
        this.timeSlotsCreated = timeSlotsCreated;
        this.shortlistsCreated = shortlistsCreated;
        this.executionTimeMs = executionTimeMs;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getCompaniesCreated() { return companiesCreated; }
    public void setCompaniesCreated(int companiesCreated) { this.companiesCreated = companiesCreated; }
    public int getStudentsCreated() { return studentsCreated; }
    public void setStudentsCreated(int studentsCreated) { this.studentsCreated = studentsCreated; }
    public int getRoomsCreated() { return roomsCreated; }
    public void setRoomsCreated(int roomsCreated) { this.roomsCreated = roomsCreated; }
    public int getPanelsCreated() { return panelsCreated; }
    public void setPanelsCreated(int panelsCreated) { this.panelsCreated = panelsCreated; }
    public int getTimeSlotsCreated() { return timeSlotsCreated; }
    public void setTimeSlotsCreated(int timeSlotsCreated) { this.timeSlotsCreated = timeSlotsCreated; }
    public int getShortlistsCreated() { return shortlistsCreated; }
    public void setShortlistsCreated(int shortlistsCreated) { this.shortlistsCreated = shortlistsCreated; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
