package com.assigment.Scheduler.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "resource_availability")
public class ResourceAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String resourceType;
    @Column(nullable = false)
    private Long resourceId;
    @Column(nullable = false)
    private Integer day;
    @Column(nullable = false)
    private String startTime;
    @Column(nullable = false)
    private String endTime;
    @Column(nullable = false)
    private Boolean available = true;
    private String reason;

    public ResourceAvailability() {
    }

    public ResourceAvailability(String resourceType, Long resourceId, Integer day, String startTime, String endTime,
            Boolean available, String reason) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String v) {
        resourceType = v;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long v) {
        resourceId = v;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer v) {
        day = v;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String v) {
        startTime = v;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String v) {
        endTime = v;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean v) {
        available = v;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        reason = v;
    }
}
