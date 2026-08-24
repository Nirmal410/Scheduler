package com.assigment.Scheduler.dto;

import com.assigment.Scheduler.entity.DisruptionType;
import java.time.LocalDateTime;

public class DisruptionDTO {
    private Long disruptionId;
    private DisruptionType type;
    private Long targetEntityId;
    private Integer day;
    private Integer startSlot;
    private Integer endSlot;
    private String reasonDescription;
    private LocalDateTime createdAt;
    private String status;
    private int directlyAffectedCount;

    public DisruptionDTO() {}

    public Long getDisruptionId() { return disruptionId; }
    public void setDisruptionId(Long disruptionId) { this.disruptionId = disruptionId; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDirectlyAffectedCount() { return directlyAffectedCount; }
    public void setDirectlyAffectedCount(int directlyAffectedCount) { this.directlyAffectedCount = directlyAffectedCount; }
}
