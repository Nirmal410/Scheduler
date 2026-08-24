package com.assigment.Scheduler.dto;

public class SeedRequest {
    private Integer studentCount = 800;
    private Integer companyCount = 35;
    private Integer roomCount = 20;
    private Integer randomSeed = 42;
    private String scenario = "DEFAULT";

    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
    public Integer getCompanyCount() { return companyCount; }
    public void setCompanyCount(Integer companyCount) { this.companyCount = companyCount; }
    public Integer getRoomCount() { return roomCount; }
    public void setRoomCount(Integer roomCount) { this.roomCount = roomCount; }
    public Integer getRandomSeed() { return randomSeed; }
    public void setRandomSeed(Integer randomSeed) { this.randomSeed = randomSeed; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
}
