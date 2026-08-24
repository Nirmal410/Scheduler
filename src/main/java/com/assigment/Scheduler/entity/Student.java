package com.assigment.Scheduler.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private Double cgpa;
    @Column(nullable = false)
    private String branch;
    @Column(unique = true)
    private String email;
    @Column(nullable = false)
    private Boolean withdrawn = false;

    public Student() {
    }

    public Student(String name, Double cgpa, String branch, String email) {
        this.name = name;
        this.cgpa = cgpa;
        this.branch = branch;
        this.email = email;
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

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double v) {
        cgpa = v;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String v) {
        branch = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        email = v;
    }

    public Boolean getWithdrawn() {
        return withdrawn;
    }

    public void setWithdrawn(Boolean v) {
        withdrawn = v;
    }
}
