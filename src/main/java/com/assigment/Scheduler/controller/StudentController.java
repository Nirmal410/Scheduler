package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.entity.Student;
import com.assigment.Scheduler.repository.StudentRepository;
import com.assigment.Scheduler.service.StudentImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentRepository repository;
    private final StudentImportService importService;

    public StudentController(StudentRepository repository, StudentImportService importService) {
        this.repository = repository;
        this.importService = importService;
    }

    @GetMapping
    public List<Student> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createOrUpdate(@RequestBody Student s) {
        if (s.getWithdrawn() == null) {
            s.setWithdrawn(false);
        }
        if (s.getId() != null) {
            Optional<Student> existingOpt = repository.findById(s.getId());
            if (existingOpt.isPresent()) {
                Student existing = existingOpt.get();
                if (s.getName() != null) existing.setName(s.getName());
                if (s.getBranch() != null) existing.setBranch(s.getBranch());
                if (s.getCgpa() != null) existing.setCgpa(s.getCgpa());
                if (s.getWithdrawn() != null) existing.setWithdrawn(s.getWithdrawn());
                return ResponseEntity.ok(repository.save(existing));
            }
        }
        Student newStudent = new Student();
        newStudent.setName(s.getName());
        newStudent.setBranch(s.getBranch());
        newStudent.setCgpa(s.getCgpa());
        newStudent.setWithdrawn(s.getWithdrawn());
        return ResponseEntity.ok(repository.save(newStudent));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<?> importCsv(@RequestPart("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> errors = new ArrayList<>();
        List<Student> toSave = new ArrayList<>();
        int row = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                row++;
                String cleanedLine = line.replace("\uFEFF", "").replace("\r", "").trim();
                if (cleanedLine.isEmpty()) continue;

                String[] p = cleanedLine.split(",", -1);
                for (int i = 0; i < p.length; i++) {
                    p[i] = p[i].trim().replaceAll("^\"|\"$", "");
                }

                // Skip header row — first column is not numeric
                if (firstLine) {
                    firstLine = false;
                    if (!p[0].matches("\\d+")) continue;
                }

                if (p.length < 4) {
                    errors.add(error(row, "Expected 4 columns: studentId,name,branch,cgpa (got " + p.length + ")"));
                    continue;
                }

                try {
                    String rawId = p[0].replaceAll("[^0-9]", "");
                    if (rawId.isEmpty()) {
                        errors.add(error(row, "Student ID is empty or non-numeric (value: '" + p[0] + "')"));
                        continue;
                    }
                    Long id = Long.valueOf(rawId);
                    String name = p[1].trim();
                    String branch = p[2].trim().toUpperCase();
                    String rawCgpa = p[3].replace(',', '.').replaceAll("[^0-9.]", "");
                    if (rawCgpa.isEmpty()) {
                        errors.add(error(row, "CGPA is empty or invalid (value: '" + p[3] + "')"));
                        continue;
                    }
                    double cgpa = Double.parseDouble(rawCgpa);

                    Student s = new Student();
                    s.setId(id);
                    s.setName(name);
                    s.setBranch(branch);
                    s.setCgpa(cgpa);
                    s.setWithdrawn(false);
                    toSave.add(s);

                } catch (Exception e) {
                    errors.add(error(row, "Parse error: " + e.getMessage()));
                }
            }
        }

        int valid = 0;
        for (Student s : toSave) {
            try {
                importService.saveStudentSafely(s);
                valid++;
            } catch (Exception e) {
                errors.add(error(-1, "DB error for student ID " + s.getId() + ": " + e.getMessage()));
            }
        }

        return ResponseEntity.ok(Map.of("valid", valid, "invalid", errors.size(), "errors", errors));
    }

    private Map<String, Object> error(int row, String reason) {
        return Map.of("row", row, "reason", reason);
    }
}
