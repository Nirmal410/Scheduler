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
    private final PanelRepository panels;

    public ShortlistController(ShortlistRepository s, CompanyRepository c, StudentRepository st, PanelRepository p) {
        shortlists = s;
        companies = c;
        students = st;
        panels = p;
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
                    Student s = findStudent(p[1].trim());
                    int rank = Integer.parseInt(p[2].trim().replaceAll("[^0-9]", ""));
                    String reason = null;
                    if (c == null)
                        reason = "Company '" + p[0] + "' not found in database";
                    else if (s == null)
                        reason = "Student ID/Name '" + p[1] + "' not found in database";
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
        if (ref == null || ref.isBlank()) return null;
        String cleanRef = ref.trim();

        // 1. Try lookup by numeric ID
        try {
            Optional<Company> byId = companies.findById(Long.valueOf(cleanRef));
            if (byId.isPresent())
                return byId.get();
        } catch (Exception ignored) {
        }

        List<Company> all = companies.findAll();

        // 2. Exact match case-insensitive
        Optional<Company> exact = all.stream()
                .filter(c -> c.getName() != null && c.getName().trim().equalsIgnoreCase(cleanRef))
                .findFirst();
        if (exact.isPresent()) return exact.get();

        // 3. Normalized match (stripping common terms like IDC, Inc, Corp, India, etc.)
        String normRef = normalizeCompanyName(cleanRef);
        if (!normRef.isEmpty()) {
            Optional<Company> normMatch = all.stream()
                    .filter(c -> c.getName() != null && normalizeCompanyName(c.getName()).equalsIgnoreCase(normRef))
                    .findFirst();
            if (normMatch.isPresent()) return normMatch.get();

            // 4. Substring / Containment match
            Optional<Company> containsMatch = all.stream()
                    .filter(c -> {
                        if (c.getName() == null) return false;
                        String cn = c.getName().toLowerCase();
                        String r = cleanRef.toLowerCase();
                        return cn.contains(r) || r.contains(cn) || cn.startsWith(r) || r.startsWith(cn);
                    })
                    .findFirst();
            if (containsMatch.isPresent()) return containsMatch.get();
        }

        // 5. Auto-create company if missing
        try {
            Company newCompany = new Company();
            newCompany.setName(cleanRef);
            newCompany.setTier(CompanyTier.CORE);
            newCompany.setArrivalDay(1);
            newCompany.setCgpaCutoff(6.0);
            newCompany.setMaxPanels(1);
            newCompany.setRequiredPanels(1);
            newCompany.setInterviewDurationMinutes(45);
            Company saved = companies.save(newCompany);

            Panel p = new Panel("Panel 1 (" + saved.getName() + ")", saved, "Interviewer 1, Interviewer 2");
            p.setMemberCount(2);
            panels.save(p);

            return saved;
        } catch (Exception e) {
            return null;
        }
    }

    private Student findStudent(String ref) {
        if (ref == null || ref.isBlank()) return null;
        String cleanRef = ref.trim();
        String numericId = cleanRef.replaceAll("[^0-9]", "");
        if (!numericId.isEmpty()) {
            try {
                Optional<Student> sOpt = students.findById(Long.valueOf(numericId));
                if (sOpt.isPresent()) return sOpt.get();
            } catch (Exception ignored) {}
        }
        return students.findAll().stream()
                .filter(st -> st.getName() != null && st.getName().trim().equalsIgnoreCase(cleanRef))
                .findFirst().orElse(null);
    }

    private String normalizeCompanyName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("(?i)\\b(inc|corp|corporation|ltd|limited|pvt|private|india|idc|development|center|tech|technologies|gds|global|services|solutions)\\b", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
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
