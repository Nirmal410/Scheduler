package com.assigment.Scheduler.dto;

import java.util.List;

public class ReplanDiffDTO {
    private Long disruptionId;
    private String snapshotId;
    private int directlyAffectedCount;
    private int repairedCount;
    private int cascadeMovesCount;
    private int cancelledCount;
    private int criticalMovesCount;
    private int totalMovedCount;
    private int totalScheduledCount;
    private double churnRatio;
    private double cascadeRatio;
    private String budgetBand;
    private boolean requiresApproval;
    private boolean hardConstraintsValid;
    private boolean infeasible;
    private String decisionMessage;
    private String recommendedOptionId;
    private List<MovedInterviewDTO> movedInterviews;
    private List<ReplanOptionDTO> options;
    private String status;
    // Same-day repair policy fields
    private int sameDayRepairedCount;
    private int crossDayMovedCount;
    private int crossDayRequiredCount;
    private String sameDayStatus;   // GREEN | AMBER | RED
    private String sameDayMessage;

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

    public int getDirectlyAffectedCount() {
        return directlyAffectedCount;
    }

    public void setDirectlyAffectedCount(int directlyAffectedCount) {
        this.directlyAffectedCount = directlyAffectedCount;
    }

    public int getRepairedCount() {
        return repairedCount;
    }

    public void setRepairedCount(int repairedCount) {
        this.repairedCount = repairedCount;
    }

    public int getCascadeMovesCount() {
        return cascadeMovesCount;
    }

    public void setCascadeMovesCount(int cascadeMovesCount) {
        this.cascadeMovesCount = cascadeMovesCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(int cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public int getCriticalMovesCount() {
        return criticalMovesCount;
    }

    public void setCriticalMovesCount(int criticalMovesCount) {
        this.criticalMovesCount = criticalMovesCount;
    }

    public int getTotalMovedCount() {
        return totalMovedCount;
    }

    public void setTotalMovedCount(int totalMovedCount) {
        this.totalMovedCount = totalMovedCount;
    }

    public int getTotalScheduledCount() {
        return totalScheduledCount;
    }

    public void setTotalScheduledCount(int totalScheduledCount) {
        this.totalScheduledCount = totalScheduledCount;
    }

    public double getChurnRatio() {
        return churnRatio;
    }

    public void setChurnRatio(double churnRatio) {
        this.churnRatio = churnRatio;
    }

    public double getCascadeRatio() {
        return cascadeRatio;
    }

    public void setCascadeRatio(double cascadeRatio) {
        this.cascadeRatio = cascadeRatio;
    }

    public String getBudgetBand() {
        return budgetBand;
    }

    public void setBudgetBand(String budgetBand) {
        this.budgetBand = budgetBand;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public boolean isHardConstraintsValid() {
        return hardConstraintsValid;
    }

    public void setHardConstraintsValid(boolean hardConstraintsValid) {
        this.hardConstraintsValid = hardConstraintsValid;
    }

    public boolean isInfeasible() {
        return infeasible;
    }

    public void setInfeasible(boolean infeasible) {
        this.infeasible = infeasible;
    }

    public String getDecisionMessage() {
        return decisionMessage;
    }

    public void setDecisionMessage(String decisionMessage) {
        this.decisionMessage = decisionMessage;
    }

    public String getRecommendedOptionId() {
        return recommendedOptionId;
    }

    public void setRecommendedOptionId(String recommendedOptionId) {
        this.recommendedOptionId = recommendedOptionId;
    }

    public List<MovedInterviewDTO> getMovedInterviews() {
        return movedInterviews;
    }

    public void setMovedInterviews(List<MovedInterviewDTO> movedInterviews) {
        this.movedInterviews = movedInterviews;
    }

    public List<ReplanOptionDTO> getOptions() {
        return options;
    }

    public void setOptions(List<ReplanOptionDTO> options) {
        this.options = options;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSameDayRepairedCount() { return sameDayRepairedCount; }
    public void setSameDayRepairedCount(int sameDayRepairedCount) { this.sameDayRepairedCount = sameDayRepairedCount; }

    public int getCrossDayMovedCount() { return crossDayMovedCount; }
    public void setCrossDayMovedCount(int crossDayMovedCount) { this.crossDayMovedCount = crossDayMovedCount; }

    public int getCrossDayRequiredCount() { return crossDayRequiredCount; }
    public void setCrossDayRequiredCount(int crossDayRequiredCount) { this.crossDayRequiredCount = crossDayRequiredCount; }

    public String getSameDayStatus() { return sameDayStatus; }
    public void setSameDayStatus(String sameDayStatus) { this.sameDayStatus = sameDayStatus; }

    public String getSameDayMessage() { return sameDayMessage; }
    public void setSameDayMessage(String sameDayMessage) { this.sameDayMessage = sameDayMessage; }
}
