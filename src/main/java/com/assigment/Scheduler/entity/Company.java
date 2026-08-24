package com.assigment.Scheduler.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "company")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyTier tier;
    @Column(name = "cgpa_cutoff", nullable = false)
    private Double cgpaCutoff;
    @Column(name = "arrival_day", nullable = false)
    @com.fasterxml.jackson.annotation.JsonAlias({"day", "arrivalDay"})
    private Integer arrivalDay;
    @Column(name = "max_panels", nullable = false)
    private Integer maxPanels;
    private String arrivalTime = "09:00";
    private String availableUntil = "17:00";
    private Integer interviewDurationMinutes = 45;
    private Integer requiredPanels = 1;
    private Integer interviewersPerPanel = 2;
    private Integer requiredRooms = 1;
    private Integer maxInterviews = 0;
    @Column(length = 500)
    private String eligibleBranches;

    public Company() {
    }

    public Company(String name, CompanyTier tier, Double cgpaCutoff, Integer arrivalDay, Integer maxPanels) {
        this.name = name;
        this.tier = tier;
        this.cgpaCutoff = cgpaCutoff;
        this.arrivalDay = arrivalDay;
        this.maxPanels = maxPanels;
        this.requiredPanels = maxPanels == null ? 1 : maxPanels;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public CompanyTier getTier() {
        return tier;
    }

    public void setTier(CompanyTier v) {
        tier = v;
    }

    public Double getCgpaCutoff() {
        return cgpaCutoff;
    }

    public void setCgpaCutoff(Double v) {
        cgpaCutoff = v;
    }

    public Integer getArrivalDay() {
        return arrivalDay;
    }

    public void setArrivalDay(Integer v) {
        arrivalDay = v;
    }

    public Integer getDay() {
        return arrivalDay;
    }

    public void setDay(Integer v) {
        arrivalDay = v;
    }

    public Integer getMaxPanels() {
        return maxPanels;
    }

    public void setMaxPanels(Integer v) {
        maxPanels = v;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String v) {
        arrivalTime = v;
    }

    public String getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(String v) {
        availableUntil = v;
    }

    public Integer getInterviewDurationMinutes() {
        return interviewDurationMinutes;
    }

    public void setInterviewDurationMinutes(Integer v) {
        interviewDurationMinutes = v;
    }

    public Integer getRequiredPanels() {
        return requiredPanels;
    }

    public void setRequiredPanels(Integer v) {
        requiredPanels = v;
        maxPanels = v;
    }

    public Integer getInterviewersPerPanel() {
        return interviewersPerPanel;
    }

    public void setInterviewersPerPanel(Integer v) {
        interviewersPerPanel = v;
    }

    public Integer getRequiredRooms() {
        return requiredRooms;
    }

    public void setRequiredRooms(Integer v) {
        requiredRooms = v;
    }

    public Integer getMaxInterviews() {
        return maxInterviews;
    }

    public void setMaxInterviews(Integer v) {
        maxInterviews = v;
    }

    public String getEligibleBranches() {
        return eligibleBranches;
    }

    public void setEligibleBranches(String v) {
        eligibleBranches = v;
    }
}
