package com.assigment.Scheduler.dto;

import java.util.List;

public class ReplanOptionDTO {
    private String optionId;
    private int rank;
    private String strategy;
    private String label;
    private boolean recommended;
    private int totalMoved;
    private int cascadeMoves;
    private int cancelled;
    private double churnRatio;
    private String budgetBand;
    private boolean requiresApproval;
    private List<MovedInterviewDTO> movedInterviews;

    public String getOptionId() {
        return optionId;
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public int getTotalMoved() {
        return totalMoved;
    }

    public void setTotalMoved(int totalMoved) {
        this.totalMoved = totalMoved;
    }

    public int getCascadeMoves() {
        return cascadeMoves;
    }

    public void setCascadeMoves(int cascadeMoves) {
        this.cascadeMoves = cascadeMoves;
    }

    public int getCancelled() {
        return cancelled;
    }

    public void setCancelled(int cancelled) {
        this.cancelled = cancelled;
    }

    public double getChurnRatio() {
        return churnRatio;
    }

    public void setChurnRatio(double churnRatio) {
        this.churnRatio = churnRatio;
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

    public List<MovedInterviewDTO> getMovedInterviews() {
        return movedInterviews;
    }

    public void setMovedInterviews(List<MovedInterviewDTO> movedInterviews) {
        this.movedInterviews = movedInterviews;
    }
}
