package com.assigment.Scheduler.dto;

public class DisruptionResponse {
    private Long disruptionId;
    private String status;
    private int directlyAffectedInterviews;
    private String type;
    private String reasonDescription;

    public DisruptionResponse() {}

    public DisruptionResponse(Long disruptionId, String status, int directlyAffectedInterviews, String type, String reasonDescription) {
        this.disruptionId = disruptionId;
        this.status = status;
        this.directlyAffectedInterviews = directlyAffectedInterviews;
        this.type = type;
        this.reasonDescription = reasonDescription;
    }

    public Long getDisruptionId() { return disruptionId; }
    public void setDisruptionId(Long disruptionId) { this.disruptionId = disruptionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDirectlyAffectedInterviews() { return directlyAffectedInterviews; }
    public void setDirectlyAffectedInterviews(int directlyAffectedInterviews) { this.directlyAffectedInterviews = directlyAffectedInterviews; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
}
