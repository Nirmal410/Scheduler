package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.entity.*;
import com.assigment.Scheduler.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/shortlists")
public class ShortlistController {
    private final ShortlistRepository shortlists;
    private final CompanyRepository companies;
    private final StudentRepository students;

    public ShortlistController(ShortlistRepository s, CompanyRepository c, StudentRepository st) {
        shortlists = s;
        companies = c;
        students = st;
    }

    @GetMapping
    public List<Shortlist> list() {
        return shortlists.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createShortlist(@RequestBody Map<String, Object> body) {
        Object compIdObj = body.get("companyId");
        Object studIdObj = body.get("studentId");
        Object rankObj = body.get("priorityRank");

        if (compIdObj == null || studIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Company and Student are required"));
        }

        Long compId = Long.valueOf(compIdObj.toString());
        Long studId = Long.valueOf(studIdObj.toString());
        int rank = rankObj != null ? Integer.parseInt(rankObj.toString()) : 1;

        Optional<Company> cOpt = companies.findById(compId);
        Optional<Student> sOpt = students.findById(studId);

        if (cOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Company not found"));
        if (sOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Student not found"));

        Company c = cOpt.get();
        Student s = sOpt.get();

        if (Boolean.TRUE.equals(s.getWithdrawn())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Student is withdrawn"));
        }

        Shortlist sl = new Shortlist(c, s, rank);
        Shortlist saved = shortlists.save(sl);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShortlist(@PathVariable Long id) {
        if (shortlists.existsById(id)) {
            shortlists.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<?> importCsv(@RequestPart("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> errors = new ArrayList<>();
        int valid = 0, row = 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                row++;
                String cleanedLine = line.replace("\uFEFF", "").trim();
                if (cleanedLine.isEmpty()) {
                    continue;
                }
                String[] p = cleanedLine.split(",", -1);
                for (int i = 0; i < p.length; i++) {
                    p[i] = p[i].trim().replaceAll("^\"|\"$", "");
                }

                if (firstLine) {
                    firstLine = false;
                    String firstCol = p[0].toLowerCase();
                    if (firstCol.contains("company") || firstCol.contains("id") || firstCol.contains("name")) {
                        continue; // Skip header row
                    }
                }

                if (p.length < 3) {
                    errors.add(err(row, "Expected companyId/companyName,studentId,priorityRank"));
                    continue;
                }
                try {
                    Company c = findCompany(p[0].trim());
                    String studentIdClean = p[1].trim().replaceAll("[^0-9]", "");
                    Student s = studentIdClean.isEmpty() ? null : students.findById(Long.valueOf(studentIdClean)).orElse(null);
                    int rank = Integer.parseInt(p[2].trim().replaceAll("[^0-9]", ""));
                    String reason = null;
                    if (c == null)
                        reason = "Company '" + p[0] + "' not found in database";
                    else if (s == null)
                        reason = "Student ID '" + p[1] + "' not found in database";
                    else if (Boolean.TRUE.equals(s.getWithdrawn()))
                        reason = "Student " + s.getName() + " is withdrawn";
                    else if (s.getCgpa() < c.getCgpaCutoff())
                        reason = "Student CGPA (" + s.getCgpa() + ") below cutoff (" + c.getCgpaCutoff() + ") for " + c.getName();
                    else if (!branchAllowed(c.getEligibleBranches(), s.getBranch()))
                        reason = "Student branch (" + s.getBranch() + ") not eligible for " + c.getName();
                    else if (shortlists.findByCompanyId(c.getId()).stream()
                            .anyMatch(x -> x.getStudent().getId().equals(s.getId())))
                        reason = "Duplicate shortlist for student " + s.getName() + " and " + c.getName();
                    if (reason != null) {
                        errors.add(err(row, reason));
                        continue;
                    }
                    shortlists.save(new Shortlist(c, s, rank));
                    valid++;
                } catch (Exception e) {
                    errors.add(err(row, "Invalid company, student, or priority value (Row: " + line + ")"));
                }
            }
        }
        return ResponseEntity.ok(Map.of("valid", valid, "invalid", errors.size(), "errors", errors));
    }

    private Company findCompany(String ref) {
        try {
            Optional<Company> byId = companies.findById(Long.valueOf(ref));
            if (byId.isPresent())
                return byId.get();
        } catch (Exception ignored) {
        }
        return companies.findAll().stream().filter(c -> c.getName().equalsIgnoreCase(ref)).findFirst().orElse(null);
    }

    private boolean branchAllowed(String configured, String branch) {
        if (configured == null || configured.isBlank())
            return true;
        return Arrays.stream(configured.split(",")).map(String::trim).map(String::toUpperCase)
                .anyMatch(x -> x.equals(branch.trim().toUpperCase()));
    }

    private Map<String, Object> err(int row, String reason) {
        return Map.of("row", row, "reason", reason);
    }
}
