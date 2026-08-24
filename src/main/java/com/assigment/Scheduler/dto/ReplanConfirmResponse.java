package com.assigment.Scheduler.dto;

import java.time.LocalDateTime;

public class ReplanConfirmResponse {
    private Long disruptionId;
    private String status;
    private LocalDateTime timestamp;
    private int interviewsMoved;
    private int interviewsCancelled;
    private int interviewsNewlyScheduled;

    public ReplanConfirmResponse() {}

    public Long getDisruptionId() { return disruptionId; }
    public void setDisruptionId(Long disruptionId) { this.disruptionId = disruptionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public int getInterviewsMoved() { return interviewsMoved; }
    public void setInterviewsMoved(int interviewsMoved) { this.interviewsMoved = interviewsMoved; }
    public int getInterviewsCancelled() { return interviewsCancelled; }
    public void setInterviewsCancelled(int interviewsCancelled) { this.interviewsCancelled = interviewsCancelled; }
    public int getInterviewsNewlyScheduled() { return interviewsNewlyScheduled; }
    public void setInterviewsNewlyScheduled(int interviewsNewlyScheduled) { this.interviewsNewlyScheduled = interviewsNewlyScheduled; }
}
