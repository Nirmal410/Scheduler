package com.assigment.Scheduler.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "panel")
public class Panel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "member_count")
    private Integer memberCount = 2;

    @Column(name = "interviewer_names", length = 500)
    private String interviewerNames;

    public Panel() {}

    public Panel(String name, Company company, String interviewerNames) {
        this.name = name;
        this.company = company;
        this.interviewerNames = interviewerNames;
    }

    public Panel(String name, Company company, Integer memberCount, String interviewerNames) {
        this.name = name;
        this.company = company;
        this.memberCount = memberCount;
        this.interviewerNames = interviewerNames;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public String getInterviewerNames() { return interviewerNames; }
    public void setInterviewerNames(String interviewerNames) { this.interviewerNames = interviewerNames; }
}

