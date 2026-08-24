package com.assigment.Scheduler.dto;

public class ReplanRequest {
    private Long disruptionId;
    private String snapshotId;
    private String optionId;
    private Long interviewId;

    public Long getDisruptionId() {
        return disruptionId;
    }

    public void setDisruptionId(Long disruptionId) {
        this.disruptionId = disruptionId;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getOptionId() {
        return optionId;
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }
}
