package com.assigment.Scheduler.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "replan_log")
public class ReplanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "disruption_id", nullable = false)
    private Disruption disruption;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplanAction action;

    @Column(name = "old_timeslot_id")
    private Long oldTimeslotId;

    @Column(name = "old_room_id")
    private Long oldRoomId;

    @Column(name = "old_panel_id")
    private Long oldPanelId;

    @Column(name = "new_timeslot_id")
    private Long newTimeslotId;

    @Column(name = "new_room_id")
    private Long newRoomId;

    @Column(name = "new_panel_id")
    private Long newPanelId;

    @Column(name = "cascade_depth")
    private Integer cascadeDepth = 0;

    @Column(length = 500)
    private String reason;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }

    public ReplanLog() {}

    public ReplanLog(Disruption disruption, Interview interview, ReplanAction action, Integer cascadeDepth, String reason) {
        this.disruption = disruption;
        this.interview = interview;
        this.action = action;
        this.cascadeDepth = cascadeDepth;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Disruption getDisruption() { return disruption; }
    public void setDisruption(Disruption disruption) { this.disruption = disruption; }
    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }
    public ReplanAction getAction() { return action; }
    public void setAction(ReplanAction action) { this.action = action; }
    public Long getOldTimeslotId() { return oldTimeslotId; }
    public void setOldTimeslotId(Long oldTimeslotId) { this.oldTimeslotId = oldTimeslotId; }
    public Long getOldRoomId() { return oldRoomId; }
    public void setOldRoomId(Long oldRoomId) { this.oldRoomId = oldRoomId; }
    public Long getOldPanelId() { return oldPanelId; }
    public void setOldPanelId(Long oldPanelId) { this.oldPanelId = oldPanelId; }
    public Long getNewTimeslotId() { return newTimeslotId; }
    public void setNewTimeslotId(Long newTimeslotId) { this.newTimeslotId = newTimeslotId; }
    public Long getNewRoomId() { return newRoomId; }
    public void setNewRoomId(Long newRoomId) { this.newRoomId = newRoomId; }
    public Long getNewPanelId() { return newPanelId; }
    public void setNewPanelId(Long newPanelId) { this.newPanelId = newPanelId; }
    public Integer getCascadeDepth() { return cascadeDepth; }
    public void setCascadeDepth(Integer cascadeDepth) { this.cascadeDepth = cascadeDepth; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
