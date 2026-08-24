package com.assigment.Scheduler.dto;

public class MovedInterviewDTO {
    private Long interviewId;
    private String studentName;
    private String companyName;
    private Integer oldDay;
    private Integer oldSlot;
    private String oldRoom;
    private String oldPanel;
    private Integer newDay;
    private Integer newSlot;
    private String newRoom;
    private String newPanel;
    private String action;
    private int cascadeDepth;
    private String reason;

    public MovedInterviewDTO() {}

    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long interviewId) { this.interviewId = interviewId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public Integer getOldDay() { return oldDay; }
    public void setOldDay(Integer oldDay) { this.oldDay = oldDay; }
    public Integer getOldSlot() { return oldSlot; }
    public void setOldSlot(Integer oldSlot) { this.oldSlot = oldSlot; }
    public String getOldRoom() { return oldRoom; }
    public void setOldRoom(String oldRoom) { this.oldRoom = oldRoom; }
    public String getOldPanel() { return oldPanel; }
    public void setOldPanel(String oldPanel) { this.oldPanel = oldPanel; }
    public Integer getNewDay() { return newDay; }
    public void setNewDay(Integer newDay) { this.newDay = newDay; }
    public Integer getNewSlot() { return newSlot; }
    public void setNewSlot(Integer newSlot) { this.newSlot = newSlot; }
    public String getNewRoom() { return newRoom; }
    public void setNewRoom(String newRoom) { this.newRoom = newRoom; }
    public String getNewPanel() { return newPanel; }
    public void setNewPanel(String newPanel) { this.newPanel = newPanel; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public int getCascadeDepth() { return cascadeDepth; }
    public void setCascadeDepth(int cascadeDepth) { this.cascadeDepth = cascadeDepth; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
