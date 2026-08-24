package com.assigment.Scheduler.dto;

import com.assigment.Scheduler.entity.DisruptionType;

public class DisruptionRequest {
    private DisruptionType type;
    private Long targetEntityId;
    private Integer day;
    private Integer startSlot;
    private Integer endSlot;
    private String reasonDescription;

    public DisruptionType getType() { return type; }
    public void setType(DisruptionType type) { this.type = type; }
    public Long getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(Long targetEntityId) { this.targetEntityId = targetEntityId; }
    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }
    public Integer getStartSlot() { return startSlot; }
    public void setStartSlot(Integer startSlot) { this.startSlot = startSlot; }
    public Integer getEndSlot() { return endSlot; }
    public void setEndSlot(Integer endSlot) { this.endSlot = endSlot; }
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
}
