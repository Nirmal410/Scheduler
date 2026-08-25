package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.entity.*;
import com.assigment.Scheduler.repository.*;
import com.assigment.Scheduler.service.StudentImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private final InterviewRepository interviewRepository;
    private final ShortlistRepository shortlistRepository;
    private final DisruptionRepository disruptionRepository;
    private final ReplanLogRepository replanLogRepository;

    public StudentController(
            StudentRepository repository,
            StudentImportService importService,
            InterviewRepository interviewRepository,
            ShortlistRepository shortlistRepository,
            DisruptionRepository disruptionRepository,
            ReplanLogRepository replanLogRepository) {
        this.repository = repository;
        this.importService = importService;
        this.interviewRepository = interviewRepository;
        this.shortlistRepository = shortlistRepository;
        this.disruptionRepository = disruptionRepository;
        this.replanLogRepository = replanLogRepository;
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
        Student saved = importService.saveStudentSafely(s);
        return ResponseEntity.ok(saved);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // 1. Delete associated interviews and their replan logs
        List<Interview> ivs = interviewRepository.findByStudentId(id);
        if (!ivs.isEmpty()) {
            List<ReplanLog> replanLogs = replanLogRepository.findByInterviewIn(ivs);
            if (!replanLogs.isEmpty()) {
                replanLogRepository.deleteAll(replanLogs);
            }
            interviewRepository.deleteAll(ivs);
        }

        // 2. Delete associated shortlists
        List<Shortlist> sls = shortlistRepository.findByStudentId(id);
        if (!sls.isEmpty()) {
            shortlistRepository.deleteAll(sls);
        }

        // 3. Delete associated disruptions and their replan logs
        try {
            List<Disruption> disruptions = disruptionRepository.findAll().stream()
                    .filter(d -> id.equals(d.getTargetEntityId()) && d.getType() == DisruptionType.STUDENT_WITHDRAW)
                    .toList();
            for (Disruption d : disruptions) {
                List<ReplanLog> disruptionLogs = replanLogRepository.findByDisruptionId(d.getId());
                if (!disruptionLogs.isEmpty()) {
                    replanLogRepository.deleteAll(disruptionLogs);
                }
                disruptionRepository.delete(d);
            }
        } catch (Exception ignored) {}

        // 4. Delete student
        repository.deleteById(id);
        return ResponseEntity.ok().build();
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
