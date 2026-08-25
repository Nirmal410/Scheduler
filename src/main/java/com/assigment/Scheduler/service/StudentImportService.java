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
    public Student saveStudentSafely(Student student) {
        if (student.getWithdrawn() == null) {
            student.setWithdrawn(false);
        }
        if (student.getId() != null) {
            Student existing = repository.findById(student.getId()).orElse(null);
            if (existing != null) {
                if (student.getName() != null) existing.setName(student.getName());
                if (student.getBranch() != null) existing.setBranch(student.getBranch());
                if (student.getCgpa() != null) existing.setCgpa(student.getCgpa());
                if (student.getEmail() != null) existing.setEmail(student.getEmail());
                if (student.getWithdrawn() != null) existing.setWithdrawn(student.getWithdrawn());
                Student saved = repository.save(existing);
                return saved;
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
                entityManager.flush();
                entityManager.clear();
                return repository.findById(student.getId()).orElse(student);
            }
        }
        return repository.save(student);
    }
}