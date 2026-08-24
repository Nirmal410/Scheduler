package com.assigment.Scheduler;

import com.assigment.Scheduler.entity.Student;
import com.assigment.Scheduler.repository.StudentRepository;
import com.assigment.Scheduler.service.StudentImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StudentImportServiceTest {

    @Autowired
    private StudentImportService importService;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void importsAndUpdatesStudentWithCsvId() {
        Student firstImport = student(101L, "Rahul Sharma", 9.2);
        importService.saveStudentSafely(firstImport);

        Student update = student(101L, "Rahul Updated", 9.5);
        importService.saveStudentSafely(update);

        assertThat(studentRepository.count()).isEqualTo(1);
        Student saved = studentRepository.findById(101L).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Rahul Updated");
        assertThat(saved.getCgpa()).isEqualTo(9.5);
    }

    private Student student(Long id, String name, double cgpa) {
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setBranch("COMPUTER SCIENCE & ENGINEERING");
        student.setCgpa(cgpa);
        student.setWithdrawn(false);
        return student;
    }
}