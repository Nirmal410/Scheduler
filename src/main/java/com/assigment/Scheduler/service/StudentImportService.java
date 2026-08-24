package com.assigment.Scheduler.service;

import com.assigment.Scheduler.entity.Student;
import com.assigment.Scheduler.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentImportService {
    private final StudentRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public StudentImportService(StudentRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveStudentSafely(Student student) {
        Student existing = repository.findById(student.getId()).orElse(null);
        if (existing != null) {
            existing.setName(student.getName());
            existing.setBranch(student.getBranch());
            existing.setCgpa(student.getCgpa());
            existing.setWithdrawn(student.getWithdrawn());
        } else {
            entityManager.createNativeQuery("""
                    INSERT INTO student (id, name, branch, cgpa, email, withdrawn)
                    VALUES (:id, :name, :branch, :cgpa, :email, :withdrawn)
                    """)
                    .setParameter("id", student.getId())
                    .setParameter("name", student.getName())
                    .setParameter("branch", student.getBranch())
                    .setParameter("cgpa", student.getCgpa())
                    .setParameter("email", student.getEmail())
                    .setParameter("withdrawn", student.getWithdrawn())
                    .executeUpdate();
        }
        entityManager.flush();
        entityManager.clear();
    }
}