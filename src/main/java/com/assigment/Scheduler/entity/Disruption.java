package com.assigment.Scheduler.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "disruption")
public class Disruption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisruptionType type;

    @Column(name = "target_entity_id", nullable = false)
    private Long targetEntityId;

    @Column(name = "day_num", nullable = false)
    private Integer day;

    @Column(name = "start_slot", nullable = false)
    private Integer startSlot;

    @Column(name = "end_slot", nullable = false)
    private Integer endSlot;

    @Column(name = "reason_description", length = 500)
    private String reasonDescription;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "varchar(20) default 'LOGGED'")
    private String status = "LOGGED";

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Disruption() {}

    public Disruption(DisruptionType type, Long targetEntityId, Integer day, Integer startSlot, Integer endSlot, String reasonDescription) {
        this.type = type;
        this.targetEntityId = targetEntityId;
        this.day = day;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
        this.reasonDescription = reasonDescription;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
}
