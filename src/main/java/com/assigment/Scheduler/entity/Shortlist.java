package com.assigment.Scheduler.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shortlist")
public class Shortlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "priority_rank", nullable = false)
    private Integer priorityRank;

    public Shortlist() {}

    public Shortlist(Company company, Student student, Integer priorityRank) {
        this.company = company;
        this.student = student;
        this.priorityRank = priorityRank;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Integer getPriorityRank() { return priorityRank; }
    public void setPriorityRank(Integer priorityRank) { this.priorityRank = priorityRank; }
}
